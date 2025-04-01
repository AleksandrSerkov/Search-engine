package searchengine.controllers;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import searchengine.config.SitesList;
import searchengine.dto.statistics.SearchResultDTO;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.entity.Site;
import searchengine.repository.SiteRepository;
import searchengine.services.IndexingService;
import searchengine.services.LemmaService;
import searchengine.services.SearchService;
import searchengine.services.SiteService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SiteService siteService;
    private final SiteRepository siteRepository;
    private final SearchService searchService;
    private final LemmaService lemmaService;
    private final SitesList sitesList;
    private volatile boolean isIndexingInProgress = false;
    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);

    public ApiController(StatisticsService statisticsService, IndexingService indexingService,
                         SiteService siteService, SiteRepository siteRepository,
                         SitesList sitesList, LemmaService lemmaService, SearchService searchService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.siteService = siteService;
        this.siteRepository = siteRepository;
        this.sitesList = sitesList;
        this.lemmaService = lemmaService;
        this.searchService = searchService;
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam String query,
            @RequestParam(required = false) String site,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int limit) {
    
        logger.info("Поисковый запрос: '{}', Сайт: '{}', Offset: {}, Limit: {}", query, site, offset, limit);
        
        if (query == null || query.trim().isEmpty()) {
            logger.warn("Поисковый запрос пустой.");
            return ResponseEntity.badRequest()
                    .body(Map.of("result", false, "error", "Поисковый запрос не может быть пустым"));
        }
    
        String siteParam = (site != null && !site.trim().isEmpty()) ? site : "";
    
        // Разбиваем запрос на леммы
        List<String> lemmas = Arrays.stream(query.split("[,\\s]+"))
                .map(String::toLowerCase)
                .distinct()
                .filter(word -> word.length() > 2)
                .collect(Collectors.toList());
        logger.info("Извлеченные леммы: {}", lemmas);
    
        String joinedQuery = String.join(" ", lemmas);
        logger.info("Объединенный запрос: '{}'", joinedQuery);
    
        try {
            // Вызываем метод search и получаем List<SearchResultDTO>
            List<SearchResultDTO> searchResults = searchService.search(joinedQuery, siteParam, offset, limit);
    
            logger.info("Найдено результатов: {}", searchResults.size());
            return ResponseEntity.ok(Map.of(
                    "result", true,
                    "count", searchResults.size(),
                    "data", searchResults
            ));
        } catch (Exception e) {
            logger.error("Ошибка при выполнении поиска: {}", e.getMessage(), e);
            // Возвращаем подробное сообщение об ошибке клиенту
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("result", false, "error", "Ошибка выполнения поиска: " + e.getMessage()));
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @PostMapping("/indexPage")
    public ResponseEntity<Map<String, Object>> indexPage(@RequestParam String url) {
        logger.info("Попытка индексации страницы: {}", url);
        if (url == null || url.trim().isEmpty()) {
            logger.warn("URL для индексирования пустой.");
            return ResponseEntity.badRequest()
                    .body(Map.of("result", false, "error", "URL не может быть пустым"));
        }
        try {
            // Находим сайт по URL (предполагается, что URL сайта совпадает с URL, переданным для индексирования)
            Site site;
            try {
                site = siteService.findSiteByUrl(url);
                logger.info("Найден сайт для индексирования страницы: {} (ID: {})", site.getUrl(), site.getId());
            } catch (IllegalArgumentException ex) {
                logger.error("Сайт не найден для URL: {}", url, ex);
                return ResponseEntity.badRequest()
                        .body(Map.of("result", false, "error", "Сайт не найден: " + ex.getMessage()));
            }
            // Используем тот же URL для индексирования страницы (в дальнейшем можно реализовать извлечение URL страницы)
            String pageUrl = url;
            indexingService.indexPage(pageUrl, site);
            return ResponseEntity.ok(Map.of("result", true, "message", "Индексация страницы запущена"));
        } catch (Exception e) {
            logger.error("Ошибка при индексации страницы: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("result", false, "error", "Ошибка индексации страницы: " + e.getMessage()));
        }
    }
    
    @Async("taskExecutor")
    @GetMapping("/startIndexing")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> startIndexing() {
        if (isIndexingInProgress) {
            logger.warn("Индексация уже выполняется, новый запуск не допускается");
            return CompletableFuture.completedFuture(ResponseEntity.badRequest()
                    .body(Map.of("result", false, "error", "Индексация уже выполняется")));
        }
    
        logger.info("Запуск индексации через API");
        isIndexingInProgress = true;
    
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Установка таймаута
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        indexingService.startIndexing();
                    } catch (Exception e) {
                        logger.error("Ошибка при индексации: {}", e.getMessage(), e);
                        throw new RuntimeException(e);
                    }
                });
                
                // Таймаут в 2 минуты
                future.get(120, TimeUnit.SECONDS);  // Если задача не завершится за 120 секунд, произойдет TimeoutException
    
                return ResponseEntity.ok(Map.of("result", true, "message", "Индексация запущена"));
            } catch (TimeoutException e) {
                logger.error("Индексация не завершена за 2 минуты: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("result", false, "error", "Таймаут выполнения индексации: " + e.getMessage()));
            } catch (Exception e) {
                logger.error("Ошибка при запуске индексации: {}", e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("result", false, "error", "Ошибка запуска индексации: " + e.getMessage()));
            } finally {
                isIndexingInProgress = false;
            }
        });
    }
   
    @RequestMapping(value = "/stopIndexing", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Map<String, Object>> stopIndexing() {
        stopIndexingService();
        logger.info("Индексация остановлена");
        return ResponseEntity.ok(Map.of("result", true, "message", "Индексация остановлена"));
    }

    private void stopIndexingService() {
        // TODO: Реализовать корректную остановку индексации (прерывание асинхронных задач)
        isIndexingInProgress = false;
        logger.warn("Остановка индексации не реализована полностью!");
    }
}

