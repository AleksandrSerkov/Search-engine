package searchengine.controllers;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import searchengine.config.SitesList;
import searchengine.dto.statistics.StatisticsResponse;

import java.util.ArrayList;
import java.util.Date;

import searchengine.entity.Site;
import searchengine.repository.SiteRepository;

import searchengine.model.Status;
import searchengine.services.IndexingService;
import searchengine.services.SiteService;
import searchengine.services.StatisticsService;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private boolean isIndexingInProgress = false;
    private final SiteRepository siteRepository;
    private final SiteService siteService;

    // Логгер (пример использования SLF4J)
    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);

    public ApiController(StatisticsService statisticsService, IndexingService indexingService, SiteRepository siteRepository, SiteService siteService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.siteRepository = siteRepository;
        this.siteService = siteService;
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

    private void startIndexingService() {
        isIndexingInProgress = true;

        new Thread(() -> {
            try {
                List<Site> modelSites = new ArrayList<>();
                List<Site> sites = siteService.getAllSites();

                for (Site site : sites) {
                    Site modelSite = new Site();
                    modelSite.setStatus(Status.INDEXING);
                    modelSite.setStatusTime(new Date());
                    modelSite.setUrl(site.getUrl());

                    modelSites.add(modelSite);

                    try {
                        // Сохранение модифицированного сайта в базу данных
                        siteRepository.save(modelSite);
                    } catch (Exception e) {
                        logger.error("Ошибка при сохранении модифицированного сайта в базу данных", e);
                    }
                }

                for (Site modelSite : modelSites) {
                    indexingService.processSitePages(modelSite, modelSite.getUrl());
                }
            } catch (Exception e) {
                logger.error("Ошибка при индексации", e);
            } finally {
                isIndexingInProgress = false;
            }
        }).start();
    }



    private void stopIndexingService() {
        isIndexingInProgress = false;
    }
}
