
package searchengine.controllers;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import searchengine.config.SitesList;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.entity.Lemma;
import searchengine.entity.Site;
import searchengine.model.SearchResult;
import searchengine.model.Status;
import searchengine.repository.SiteRepository;
import searchengine.services.IndexingService;
import searchengine.services.LemmaService;
import searchengine.services.SiteService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SiteService siteService;
    private final SiteRepository siteRepository;
    private final SitesList sitesList;
    private boolean isIndexingInProgress = false;
    private final LemmaService lemmaService;
    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);

    public ApiController(StatisticsService statisticsService, IndexingService indexingService, 
                         SiteService siteService, SiteRepository siteRepository, SitesList sitesList,LemmaService lemmaService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.siteService = siteService;
        this.siteRepository = siteRepository;
        this.sitesList = sitesList;
        this.lemmaService=lemmaService;
    }
    @GetMapping(value = "/search", produces = "application/json")
public ResponseEntity<Map<String, Object>> search(
        @RequestParam String query,
        @RequestParam(required = false) String site,
        @RequestParam(defaultValue = "0") int offset,
        @RequestParam(defaultValue = "20") int limit) {
    try {
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("result", false, "error", "Задан пустой поисковый запрос"));
        }

        // Проверка индексации
        boolean isIndexed = siteService.isSiteIndexed(site);
        if (!isIndexed) {
            return ResponseEntity.badRequest()
                    .body(Map.of("result", false, "error", "Сайт не проиндексирован"));
        }

        // Получаем результаты поиска
        List<SearchResult> searchResults = indexingService.search(query, site, offset, limit);
        int totalResults = indexingService.countSearchResults(query, site);

        // Формируем ответ
        Map<String, Object> response = Map.of(
                "result", true,
                "count", totalResults,
                "data", searchResults
        );
        return ResponseEntity.ok(response);

    } catch (Exception e) {
        logger.error("Error during search", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("result", false, "error", "Ошибка выполнения поиска"));
    }
}

    @PostMapping(value = "/saveLemma", produces = "application/json")
    public ResponseEntity<Map<String, Object>> saveLemma(@RequestParam String lemmaText, @RequestParam int siteId) {
        try {
            Lemma savedLemma = lemmaService.saveLemma(lemmaText, siteId);
            logger.info("Lemma saved successfully: {}", savedLemma);
            return ResponseEntity.ok(Map.of("result", true, "lemma", savedLemma));
        } catch (IllegalArgumentException e) {
            logger.error("Error saving lemma: ", e);
            return ResponseEntity.badRequest().body(Map.of("result", false, "error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error while saving lemma: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("result", false, "error", "Unexpected error occurred"));
        }
    }

    @GetMapping(value = "/statistics", produces = "application/json")
    public ResponseEntity<StatisticsResponse> statistics() {
        try {
            StatisticsResponse response = statisticsService.getStatistics();
            logger.info("Sending statistics: {}", response);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping(value = "/indexPage", produces = "application/json")
    public ResponseEntity<Map<String, Object>> indexPage(@RequestParam String url) {
        if (!isValidUrl(url)) {
            logger.warn("Invalid URL: {}", url);
            return ResponseEntity.badRequest().body(Map.of("result", false, "error", "Invalid URL"));
        }

        try {
            Site site = new Site();
            site.setUrl(url);
            site.setName("New Site");
            site.setStatus(Status.INDEXING);  // Присваиваем статус INDEXING
            siteService.saveSite(site);
            indexingService.processSitePages(site, url);
        } catch (Exception e) {
            logger.error("Error indexing page", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("result", false, "error", "Error while indexing the page"));
        }

        return ResponseEntity.ok(Map.of("result", true));
    }

    @GetMapping(value = "/startIndexing", produces = "application/json")
    public synchronized ResponseEntity<Map<String, Object>> startIndexing() {
        if (isIndexingInProgress) {
            return ResponseEntity.badRequest()
                    .body(Map.of("result", false, "error", "Indexing is already in progress"));
        }

        isIndexingInProgress = true;
        startIndexingService().thenAccept(result -> logger.info("Indexing completed"));
        return ResponseEntity.ok(Map.of("result", true, "message", "Indexing started"));
    }

    @Async
@Transactional
public CompletableFuture<ResponseEntity<Map<String, Object>>> startIndexingService() {
    logger.info("Starting indexing process...");
    try {
        List<Site> sitesInDatabase = siteService.getAllSites();
        if (sitesInDatabase == null || sitesInDatabase.isEmpty()) {
            logger.debug("No sites found in the database. Loading sites from the configuration...");
            if (sitesList == null || sitesList.getSites() == null) {
                throw new IllegalArgumentException("Configuration error: sitesList is null.");
            }

            List<Site> sitesFromConfig = sitesList.getSites().stream()
                    .filter(Objects::nonNull)
                    .map(siteConfig -> {
                        Site site = new Site();
                        site.setUrl(siteConfig.getUrl());
                        site.setName(siteConfig.getName());
                        site.setStatus(Status.INDEXED); // Присваиваем статус INDEXED
                        return site;
                    })
                    .collect(Collectors.toList());

            if (sitesFromConfig.isEmpty()) {
                logger.warn("No sites found in the configuration.");
                return CompletableFuture.completedFuture(ResponseEntity.badRequest()
                        .body(Map.of("result", false, "error", "No sites found in the configuration.")));
            }

            sitesFromConfig.forEach(siteService::saveSite);
            sitesInDatabase.addAll(sitesFromConfig);
        }

        List<Site> modifiedSites = modifyAndSaveSites(sitesInDatabase);
        if (!modifiedSites.isEmpty()) {
            modifiedSites.forEach(modifiedSite -> {
                try {
                    modifiedSite.setStatus(Status.INDEXING);  // Присваиваем статус INDEXING перед обработкой
                    siteService.saveSite(modifiedSite);
                    indexingService.processSitePages(modifiedSite, modifiedSite.getUrl());
                } catch (IOException e) {
                    logger.error("Error processing site pages for site: " + modifiedSite.getUrl(), e);
                    modifiedSite.setStatus(Status.FAILED); // Присваиваем статус FAILED при ошибке
                    siteService.saveSite(modifiedSite);
                }
            });
        } else {
            logger.info("No sites to process after modification.");
            return CompletableFuture.completedFuture(ResponseEntity.ok(Map.of("result", true, "message", "No sites to process.")));
        }
    } catch (IllegalArgumentException e) {
        logger.error("Invalid input: ", e);
        return CompletableFuture.completedFuture(ResponseEntity.badRequest()
                .body(Map.of("result", false, "error", e.getMessage())));
    } catch (Exception e) {
        logger.error("Error during indexing process", e);
        return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("result", false, "error", "Error during indexing process")));
    } finally {
        isIndexingInProgress = false;
        logger.info("Indexing process completed.");
    }

    return CompletableFuture.completedFuture(ResponseEntity.ok(Map.of("result", true, "message", "Indexing process completed.")));
}

    @Transactional
    private List<Site> modifyAndSaveSites(List<Site> sites) {
        List<Site> modifiedSites = new ArrayList<>();
        for (Site site : sites) {
            Site modifiedSite = new Site();
            modifiedSite.setStatus(Status.INDEXING);  // Устанавливаем статус INDEXING для всех
            modifiedSite.setStatusTime(new Date());
            modifiedSite.setUrl(site.getUrl());
            modifiedSite.setName(site.getName());
            modifiedSites.add(modifiedSite);
        }
        modifiedSites.forEach(siteService::saveSite);
        return modifiedSites;
    }

    private boolean isValidUrl(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }

    private void stopIndexingService() {
        isIndexingInProgress = false;
    }

}
