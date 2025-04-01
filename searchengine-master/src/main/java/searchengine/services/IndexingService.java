package searchengine.services;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import searchengine.entity.Site;
import searchengine.model.Status;

@Service
public class IndexingService {

    private static final Logger logger = LoggerFactory.getLogger(IndexingService.class);

    @Autowired
    private SiteService siteService;

    @Autowired
    private PageService pageService;

    /**
     * Запускает процесс индексации для всех сайтов, полученных из базы.
     * При возникновении ошибок выбрасываются исключения с подробной информацией.
     */
    @Async("taskExecutor")
    @Transactional
    public CompletableFuture<Void> startIndexing() {
        logger.info("Запуск процесса индексации...");

        return CompletableFuture.runAsync(() -> {
            try {
                List<Site> sites = siteService.getAllSites();
                if (sites == null || sites.isEmpty()) {
                    String errMsg = "В базе данных не найдено сайтов.";
                    logger.error(errMsg);
                    throw new RuntimeException(errMsg);
                }
                logger.info("Обнаружено {} сайтов в базе данных.", sites.size());

                // Обрабатываем каждый сайт
                for (Site site : sites) {
                    try {
                        logger.info("Начинаем обработку сайта: {} (ID: {})", site.getUrl(), site.getId());
                        processSite(site); // Логика индексации для сайта
                    } catch (Exception e) {
                        logger.error("Ошибка при обработке сайта {}: {}", site.getUrl(), e.toString(), e);
                        siteService.updateSiteStatus(site, Status.FAILED);
                        siteService.updateSiteLastError(site, "Ошибка обработки: " + e.getMessage());
                        siteService.saveOrUpdateSite(site);
                        // Передаем подробное сообщение об ошибке пользователю
                        throw new RuntimeException("Ошибка при обработке сайта " + site.getUrl() + ": " + e.getMessage(), e);
                    }
                }
                logger.info("Процесс индексации завершен.");
            } catch (Exception e) {
                logger.error("Ошибка при индексации: {}", e.getMessage(), e);
                throw new RuntimeException("Ошибка при индексации: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Индексирует конкретный сайт: обновляет статус, обрабатывает страницы.
     * При ошибках выбрасывается исключение с подробным сообщением.
     */
    @Transactional
    public void processSite(Site site) throws IOException {
        logger.info("Начата индексация сайта: {} (ID: {})", site.getUrl(), site.getId());

        int attempts = 0;
        boolean success = false;
        while (attempts < 3 && !success) {
            try {
                attempts++;

                // Устанавливаем статус INDEXING и сохраняем
                siteService.updateSiteStatus(site, Status.INDEXING);
                siteService.saveOrUpdateSite(site);

                // Пример: список URL страниц для индексирования (используем URL сайта)
                List<String> pageUrls = List.of(site.getUrl());
                for (String pageUrl : pageUrls) {
                    logger.info("Индексируем страницу: {} для сайта: {}", pageUrl, site.getUrl());
                    indexPage(pageUrl, site);
                }

                // После успешного индексирования обновляем статус на INDEXED
                siteService.updateSiteStatus(site, Status.INDEXED);
                siteService.saveOrUpdateSite(site);
                logger.info("Сайт успешно проиндексирован: {}", site.getUrl());

                success = true;  // Завершаем цикл при успешном выполнении

            } catch (PessimisticLockingFailureException e) {
                if (attempts >= 3) {
                    logger.error("Ошибка при обработке сайта {}: {}", site.getUrl(), e.toString(), e);
                    siteService.updateSiteStatus(site, Status.FAILED);
                    siteService.updateSiteLastError(site, "Ошибка обработки: " + e.getMessage());
                    siteService.saveOrUpdateSite(site);
                    throw new IOException("Ошибка при обработке сайта " + site.getUrl() + ": " + e.getMessage(), e);
                }
                logger.warn("Проблемы с блокировкой при обработке сайта {}. Попытка {}/3.", site.getUrl(), attempts);
                try {
                    TimeUnit.SECONDS.sleep(2);  // Задержка перед повторной попыткой
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Индексация прервана.", ex);
                }
            } catch (Exception e) {
                logger.error("Ошибка при обработке сайта {}: {}", site.getUrl(), e.toString(), e);
                siteService.updateSiteStatus(site, Status.FAILED);
                siteService.updateSiteLastError(site, "Ошибка обработки: " + e.getMessage());
                siteService.saveOrUpdateSite(site);
                throw new IOException("Ошибка при обработке сайта " + site.getUrl() + ": " + e.getMessage(), e);
            }
        }
    }

    /**
     * Индексирует конкретную страницу для указанного сайта.
     * При ошибках выбрасывается исключение с подробным сообщением.
     */
    @Transactional
    public void indexPage(String url, Site site) throws IOException {
        logger.info("Начата индексизация страницы: {} для сайта: {}", url, site.getUrl());
        try {
            // Устанавливаем статус INDEXING перед индексированием страницы
            siteService.updateSiteStatus(site, Status.INDEXING);
            siteService.saveOrUpdateSite(site);

            // Вызываем метод, который индексирует страницу
            pageService.indexPage(url);

            // По завершении обновляем статус на INDEXED
            siteService.updateSiteStatus(site, Status.INDEXED);
            siteService.saveOrUpdateSite(site);
            logger.info("Страница успешно проиндексирована: {} для сайта: {}", url, site.getUrl());
        } catch (SocketTimeoutException e) {
            logger.error("Таймаут при подключении к странице {}: {}", url, e.toString(), e);
            siteService.updateSiteStatus(site, Status.FAILED);
            siteService.updateSiteLastError(site, "Таймаут подключения: " + e.getMessage());
            siteService.saveOrUpdateSite(site);
            throw new IOException("Таймаут подключения к странице " + url + ": " + e.getMessage(), e);
        } catch (IOException e) {
            logger.error("Ошибка ввода-вывода при индексировании страницы {}: {}", url, e.toString(), e);
            siteService.updateSiteStatus(site, Status.FAILED);
            siteService.updateSiteLastError(site, e.getMessage());
            siteService.saveOrUpdateSite(site);
            throw new IOException("Ошибка ввода-вывода при индексировании страницы " + url + ": " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Неожиданная ошибка при индексировании страницы {}: {}", url, e.toString(), e);
            siteService.updateSiteStatus(site, Status.FAILED);
            siteService.updateSiteLastError(site, e.getMessage());
            siteService.saveOrUpdateSite(site);
            throw new IOException("Ошибка при обработке страницы " + url + ": " + e.getMessage(), e);
        }
    }
}

