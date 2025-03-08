
package searchengine.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
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
                         SiteService siteService, SiteRepository siteRepository, 
                         SitesList sitesList, LemmaService lemmaService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.siteService = siteService;
        this.siteRepository = siteRepository;
        this.sitesList = sitesList;
        this.lemmaService = lemmaService;
    }
    
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam String query,
            @RequestParam(required = false) String site,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int limit) {

        logger.info("Поисковый запрос: '{}', Сайт: '{}', Offset: {}, Limit: {}", query, site, offset, limit);

        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("result", false, "error", "Поисковый запрос не может быть пустым"));
        }

        if (site != null && !siteService.isSiteIndexed(site)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("result", false, "error", "Сайт не проиндексирован"));
        }

        try {
            List<SearchResult> searchResults = indexingService.search(query, site, offset, limit);
            int totalResults = indexingService.countSearchResults(query, site);

            logger.info("Найдено результатов: {}", totalResults);

            return ResponseEntity.ok(Map.of(
                    "result", true,
                    "count", totalResults,
                    "data", searchResults
            ));
        } catch (Exception e) {
            logger.error("Ошибка при выполнении поиска: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("result", false, "error", "Ошибка выполнения поиска"));
        }
    }

    /**
     * Разбивает поисковый запрос на отдельные леммы (слова),
     * приводя их к нижнему регистру и исключая короткие слова.
     *
     * @param query поисковый запрос
     * @return список лемм
     */
    private List<String> extractLemmas(String query) {
        return Arrays.stream(query.split("\\s+"))
                .map(String::toLowerCase)
                .distinct()
                .filter(word -> word.length() > 2)
                .collect(Collectors.toList());
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

    @PostMapping("/indexPage")
    public ResponseEntity<Map<String, Object>> indexPage(@RequestParam String url) {
        if (url == null || url.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("result", false, "error", "URL не может быть пустым"));
        }
        
        try {
            Site site = new Site();
            site.setUrl(url);
            site.setName("New Site");
            site.setStatus(Status.INDEXING);
            siteService.saveSite(site);
            indexingService.processSitePages(site, url);
            return ResponseEntity.ok(Map.of("result", true, "message", "Индексация страницы запущена"));
        } catch (Exception e) {
            logger.error("Ошибка при индексации страницы", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("result", false, "error", "Ошибка индексации страницы"));
        }
    }

    @GetMapping("/startIndexing")
public synchronized ResponseEntity<Map<String, Object>> startIndexing() {
    if (isIndexingInProgress) {
        return ResponseEntity.badRequest()
                .body(Map.of("result", false, "error", "Индексация уже выполняется"));
    }

    isIndexingInProgress = true;
    CompletableFuture.runAsync(() -> {
        try {
            List<Site> sites = siteService.getAllSites();
            indexingService.startIndexing(sites);
            logger.info("Индексация завершена");
        } catch (Exception e) {
            logger.error("Ошибка индексации", e);
        } finally {
            isIndexingInProgress = false;
        }
    });
    
    return ResponseEntity.ok(Map.of("result", true, "message", "Индексация запущена"));
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
