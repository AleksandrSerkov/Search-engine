package searchengine.controllers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private boolean isIndexingInProgress = false;
private final SitesList sitesList;
private final Site site;
    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);

    public ApiController(StatisticsService statisticsService, IndexingService indexingService, SiteService siteService, SiteRepository siteRepository,SitesList sitesList,Site site ) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.siteService = siteService;
        this.siteRepository = siteRepository;
        this.sitesList = sitesList;
        this.site = site;
    }

    @PostMapping("/indexPage")
public ResponseEntity<?> indexPage(@RequestParam String url) {
    if (!isValidUrl(url)) {
        return ResponseEntity.badRequest().body(Map.of("result", false, "error", "Неверный адрес страницы"));
    }

    // Логика добавления или обновления страницы в индексе
    try {
        searchengine.entity.Site site = new searchengine.entity.Site();
        site.setUrl(url);
        site.setName("New Site");

        // Сохраняем сайт в базу данных
        siteService.saveSite(site);

        // Запускаем процесс индексации для новой страницы
        indexingService.processSitePages(site, url);

    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("result", false, "error", "Ошибка при индексации страницы"));
    }

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
        // Получаем список сайтов из базы данных
        List<searchengine.entity.Site> sitesInDatabase = siteService.getAllSites();

        if (sitesInDatabase == null || sitesInDatabase.isEmpty()) {
            logger.warn("No sites found in the database. Loading sites from the configuration...");

            if (sitesList != null) {
                List<searchengine.entity.Site> sitesFromConfig = sitesList.getSites().stream()
                    .filter(Objects::nonNull)
                    .map(siteConfig -> {
                        searchengine.entity.Site site = new searchengine.entity.Site();
                        site.setUrl(siteConfig.getUrl());
                        site.setName(siteConfig.getName());
                        site.setStatus(Status.INDEXED);
                        return site;
                    })
                    .collect(Collectors.toList());

                // Сохраняем новые сайты в базу данных
                sitesFromConfig.forEach(site -> {
                    if (site != null) {
                        siteService.saveSite(site);
                        sitesInDatabase.add(site);
                    }
                });
            }
        }

        // Процесс модификации и индексации сайтов
        List<searchengine.entity.Site> modifiedSites = modifyAndSaveSites(sitesInDatabase);

        modifiedSites.forEach(modifiedSite -> {
            modifiedSite.setStatus(Status.INDEXING);
            siteService.saveSite(modifiedSite);
            indexingService.processSitePages(modifiedSite, modifiedSite.getUrl());
        });

    } catch (Exception e) {
        logger.error("Error during the indexing process", e);
    } finally {
        logger.info("Indexing process completed.");
    }
}
    


    








@Transactional
private List<Site> modifyAndSaveSites(List<Site> sites) {
    List<Site> modifiedSites = new ArrayList<>();
    
    for (Site site : sites) {
        Site modifiedSite = new Site();
        modifiedSite.setStatus(Status.INDEXING);
        modifiedSite.setStatusTime(new Date());
        modifiedSite.setUrl(site.getUrl());
        modifiedSite.setName(site.getName()); // Пример модификации
        
        modifiedSites.add(modifiedSite);
    }
    
    // Сохраняем модифицированные сайты по одному
    for (Site modifiedSite : modifiedSites) {
        siteService.saveSite(modifiedSite);
    }
    
    return modifiedSites;
}



    private void stopIndexingService() {
        isIndexingInProgress = false;
    }
}
