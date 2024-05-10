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

@RestController
@RequestMapping("/api")
@Controller
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SitesList sitesList;
    private boolean isIndexingInProgress = false;
    private final SiteRepository siteRepository;
    private final SiteService siteService;
    public ApiController(StatisticsService statisticsService,  IndexingService indexingService, SitesList sitesList, SiteRepository siteRepository,SiteService siteService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.sitesList = sitesList;
        this.siteRepository = siteRepository;
this.siteService =siteService;
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

            isIndexingInProgress = true;

            try {
                indexingService.startIndexing(siteRepository.findAll());
                return ResponseEntity.ok("Индексация запущена");
            } catch (Exception e) {
                isIndexingInProgress = false; // Сброс флага в случае ошибки
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Произошла ошибка при запуске индексации");
            }
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
                }

                for (Site modelSite : modelSites) {
                    siteRepository.save(modelSite);
                    indexingService.processSitePages(modelSite, modelSite.getUrl());
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                isIndexingInProgress = false;
            }
        }).start();
    }








    private void stopIndexingService() {
        isIndexingInProgress = false;
    }
}
