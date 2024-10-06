
package searchengine.controllers;
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
import searchengine.entity.Site;
import searchengine.model.Status;
import searchengine.repository.SiteRepository;
import searchengine.services.IndexingService;
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

    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);

    public ApiController(StatisticsService statisticsService, IndexingService indexingService, 
                         SiteService siteService, SiteRepository siteRepository, SitesList sitesList) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.siteService = siteService;
        this.siteRepository = siteRepository;
        this.sitesList = sitesList;
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

    @PostMapping(value = "/statistics", produces = "application/json")
    public ResponseEntity<StatisticsResponse> statisticsPost() {
        try {
            StatisticsResponse response = statisticsService.getStatistics();
            logger.info("Sending statistics via POST: {}", response);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching statistics via POST", e);
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

    @GetMapping(value = "/stopIndexing", produces = "application/json")
    public synchronized ResponseEntity<Map<String, Object>> stopIndexing() {
        if (!isIndexingInProgress) {
            return ResponseEntity.badRequest()
                    .body(Map.of("result", false, "error", "Indexing is not in progress"));
        }

        stopIndexingService();
        return ResponseEntity.ok(Map.of("result", true, "message", "Indexing stopped"));
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
                            site.setStatus(Status.INDEXED);
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
                    modifiedSite.setStatus(Status.INDEXING);
                    siteService.saveSite(modifiedSite);
                    indexingService.processSitePages(modifiedSite, modifiedSite.getUrl());
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
            modifiedSite.setStatus(Status.INDEXING);
            modifiedSite.setStatusTime(new Date());
            modifiedSite.setUrl(site.getUrl());
            modifiedSite.setName(site.getName());
            modifiedSites.add(modifiedSite);
        }
        modifiedSites.forEach(siteService::saveSite);
        return modifiedSites;
    }

    private void stopIndexingService() {
        isIndexingInProgress = false;
    }

    private boolean isValidUrl(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }
}
