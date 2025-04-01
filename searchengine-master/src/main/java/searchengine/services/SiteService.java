package searchengine.services;


import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import searchengine.config.SitesList;
import searchengine.entity.Site;
import searchengine.model.Status;
import searchengine.repository.SiteRepository;


@Service
public class SiteService {
    private static final Logger logger = LoggerFactory.getLogger(SiteService.class);
    private final SiteRepository siteRepository;
    private final SitesList siteConfig;
    private final EntityManager entityManager; // Inject EntityManager

    @Autowired
    public SiteService(SiteRepository siteRepository, SitesList siteConfig, EntityManager entityManager) {
        this.siteRepository = siteRepository;
        this.siteConfig = siteConfig;
        this.entityManager = entityManager;
    }

    /**
     * Преобразование конфигурационного сайта в сущность Site.
     */
    public Site convertToEntitySite(searchengine.config.Site configSite) {
        logger.info("Преобразование configSite: URL='{}', Name='{}'", configSite.getUrl(), configSite.getName());
        return new Site(
                configSite.getUrl(),
                configSite.getName(),
                Status.INDEXING,
                new Date(),
                ""  // lastError инициализируем пустой строкой
        );
    }

    /**
     * Получение списка сайтов из YAML-конфигурации.
     */
    public List<Site> getAllConfigSitesAsEntities() {
        logger.info("Получение сайтов из конфигурации...");
        List<searchengine.config.Site> configSites = siteConfig.getSites();
        if (configSites == null || configSites.isEmpty()) {
            logger.warn("Список сайтов в конфигурации пуст.");
            return List.of();
        }
        List<Site> sites = configSites.stream()
                .map(this::convertToEntitySite)
                .collect(Collectors.toList());
        logger.info("Преобразовано сайтов из YAML: {}", sites.size());
        return sites;
    }

    /**
     * Проверка, проиндексирован ли сайт.
     */
    public boolean isSiteIndexed(String siteUrl) {
        boolean indexed = (siteUrl == null || siteUrl.isEmpty())
                ? siteRepository.existsByStatus(Status.INDEXED)
                : siteRepository.existsByUrlAndStatus(siteUrl, Status.INDEXED);
        logger.info("Проверка индексации [{}]: {}", siteUrl, indexed);
        return indexed;
    }

    /**
     * Поиск сайта по URL.
     */
    public Site findSiteByUrl(String siteUrl) {
        logger.info("Поиск сайта по URL: {}", siteUrl);
        return siteRepository.findByUrl(siteUrl)
                .orElseThrow(() -> {
                    String errorMessage = "Сайт с URL " + siteUrl + " не найден";
                    logger.error(errorMessage);
                    return new IllegalArgumentException(errorMessage);
                });
    }

    /**
     * Удаление сайта.
     */
    public void deleteSite(Site site) {
        logger.info("Удаление сайта: {}", site.getUrl());
        siteRepository.delete(site);
        logger.info("Сайт удален: {}", site.getUrl());
    }

    /**
     * Очистка всех сайтов из БД.
     */
    public void clearAllSites() {
        logger.info("Очистка всех сайтов из БД...");
        siteRepository.deleteAll();
        logger.info("Все сайты удалены.");
    }

    /**
     * Получение всех сайтов из БД.
     */
    public List<Site> getAllSites() {
        logger.info("Получение всех сайтов из БД...");
        List<Site> sites = siteRepository.findAll();
        logger.info("Найдено {} сайтов в БД.", sites.size());
        return sites;
    }

    /**
     * Сохранение или обновление сайта в отдельной транзакции.
     * Используется для операций, выполняемых после инициализации.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 30)
    public Site saveOrUpdateSite(Site site) {
        Optional<Site> existingSiteOpt = siteRepository.findByUrl(site.getUrl());
    
        if (existingSiteOpt.isPresent()) {
            Site existingSite = existingSiteOpt.get();
            
            // Если сайт уже долго в статусе INDEXING, сбрасываем в FAILED
            if (existingSite.getStatus() == Status.INDEXING) {
                long elapsedMinutes = ChronoUnit.MINUTES.between(existingSite.getStatusTime().toInstant(), Instant.now());
                if (elapsedMinutes > 5) { // Если более 5 минут
                    existingSite.setStatus(Status.FAILED);
                    existingSite.setLastError("Timeout error: indexing took too long.");
                }
            }
            
            existingSite.setName(site.getName());
            existingSite.setStatus(Status.INDEXING);
            existingSite.setStatusTime(new Date());
            existingSite.setLastError("");
            return siteRepository.saveAndFlush(existingSite);
        } else {
            Site newSite = new Site(site.getUrl(), site.getName(), Status.INDEXING, new Date(), "");
            return siteRepository.saveAndFlush(newSite);
        }
    }
    
    /**
     * Обновление статуса сайта.
     */
    @Transactional
    public void updateSiteStatus(Site site, Status status) {
        logger.info("Обновление статуса сайта {} на {}", site.getUrl(), status);
        site.setStatus(status);
        siteRepository.save(site);
        logger.info("Статус сайта {} обновлен на {}", site.getUrl(), status);
    }

    /**
     * Обновление поля lastError для сайта.
     */
    @Transactional
    public void updateSiteLastError(Site site, String errorMessage) {
        logger.info("Обновление lastError для сайта {}: {}", site.getUrl(), errorMessage);
        site.setLastError(errorMessage);
        siteRepository.save(site);
        logger.error("Ошибка сайта {} -> {}", site.getUrl(), errorMessage);
    }
}

