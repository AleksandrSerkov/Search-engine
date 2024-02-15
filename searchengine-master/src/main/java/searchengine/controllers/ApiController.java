package searchengine.controllers;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.StatisticsResponse;
import java.util.Date;
import searchengine.model.SiteRepository;

import searchengine.model.Status;
import searchengine.services.IndexingService;
import searchengine.services.SiteService;
import searchengine.services.StatisticsService;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SitesList sitesList;
    private boolean isIndexingInProgress = false;
    private final SiteRepository siteRepository;
    public ApiController(StatisticsService statisticsService, IndexingService indexingService, SitesList sitesList, SiteRepository siteRepository) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.sitesList = sitesList;
        this.siteRepository = siteRepository;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<?> startIndexing() {
        if (isIndexingInProgress) {
            return ResponseEntity.badRequest().body("Индексация уже запущена");
        }

        startIndexingService();

        return ResponseEntity.ok("Индексация успешно запущена");
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

        // Запуск сервиса индексации сайтов в отдельном потоке
        new Thread(() -> {
            try {
                List<Site> sites = sitesList.getSites();

                for (Site site : sites) {
                    // Создание нового объекта Site для передачи данных в базу данных
                    searchengine.model.Site currentSite = new searchengine.model.Site();
                    // Заполнение полей объекта currentSite значениями из объекта site
                    currentSite.setStatus(Status.INDEXING);
                    currentSite.setStatusTime(new Date());
                    currentSite.setUrl(site.getUrl());
                    currentSite.setName(site.getName());

                    // Сохранить объект currentSite в базе данных (с использованием JPA или другой ORM)
                    siteRepository.save(currentSite);
                    // Инициирование процесса индексации
                    indexingService.processSitePages(currentSite, site.getUrl());
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
