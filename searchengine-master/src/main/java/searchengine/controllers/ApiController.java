package searchengine.controllers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import searchengine.config.SitesList;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.entity.Site;
import searchengine.model.Status;
import searchengine.services.IndexingService;
import searchengine.services.SiteService;
import searchengine.services.StatisticsService;
import searchengine.repository.SiteRepository;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SiteService siteService;
    private final SiteRepository siteRepository;
    private boolean isIndexingInProgress = false;
private final SitesList sitesList;
    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);

    public ApiController(StatisticsService statisticsService, IndexingService indexingService, SiteService siteService, SiteRepository siteRepository,SitesList sitesList ) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.siteService = siteService;
        this.siteRepository = siteRepository;
        this.sitesList = sitesList;
    }

    @PostMapping("/indexPage")
    public ResponseEntity<?> indexPage(@RequestParam String url) {
        if (!isValidUrl(url)) {
            return ResponseEntity.badRequest().body(Map.of("result", false, "error", "Неверный адрес страницы"));
        }

        // Логика добавления или обновления страницы в индексе

        return ResponseEntity.ok(Map.of("result", true));
    }

    private boolean isValidUrl(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<?> startIndexing() {
        synchronized (this) {
            if (isIndexingInProgress) {
                return ResponseEntity.badRequest().body("Индексация уже запущена");
            }

            startIndexingService();
            return ResponseEntity.ok("Индексация запущена");
        }
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<?> stopIndexing() {
        if (!isIndexingInProgress) {
            return ResponseEntity.badRequest().body("Индексация не запущена");
        }

        stopIndexingService();

        return ResponseEntity.ok().body("Индексация успешно остановлена");
    }

    @Async
    @Transactional
    public void startIndexingService() {
        logger.info("Starting indexing process...");

        try {
            // Получаем список сайтов из sitesList и преобразуем его в нужный тип
            List<searchengine.entity.Site> sites = sitesList.getSites().stream()
                    .map(site -> {
                        searchengine.entity.Site convertedSite = new searchengine.entity.Site();
                        convertedSite.setUrl(site.getUrl());
                        convertedSite.setName(site.getName());
                        return convertedSite;
                    })
                    .collect(Collectors.toList());

            if (sites.isEmpty()) {
                logger.warn("No sites found in configuration. Loading sites from database...");
                sites = siteService.getAllSites(); // Загрузка сайтов из базы данных
            }

            List<searchengine.entity.Site> modifiedSites = new ArrayList<>();

            for (searchengine.entity.Site site : sites) {
                searchengine.entity.Site modifiedSite = modifyAndSaveSite(site);
                modifiedSites.add(modifiedSite);
            }

            for (searchengine.entity.Site modifiedSite : modifiedSites) {
                indexingService.processSitePages(modifiedSite, modifiedSite.getUrl());
            }

        } catch (Exception e) {
            logger.error("Error during indexing process", e);
        } finally {
            logger.info("Indexing process completed.");
        }
    }

    @Transactional
    private Site modifyAndSaveSite(Site site) {
        Site modifiedSite = new Site();
        modifiedSite.setStatus(Status.INDEXING);
        modifiedSite.setStatusTime(new Date());
        modifiedSite.setUrl(site.getUrl());
        modifiedSite.setName(site.getName()); // Пример модификации

        siteService.saveSite(modifiedSite); // Сохраняем модифицированный сайт в базу данных

        return modifiedSite;
    }


    private void stopIndexingService() {
        isIndexingInProgress = false;
    }
}
