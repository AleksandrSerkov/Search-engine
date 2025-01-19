package searchengine.services;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import searchengine.config.SitesList;
import searchengine.entity.Index;
import searchengine.entity.Lemma;
import searchengine.entity.Page;
import searchengine.entity.Site;
import searchengine.model.SearchResult;
import searchengine.model.Status;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

@Service
public class IndexingService {

    private static final Logger logger = LoggerFactory.getLogger(IndexingService.class);

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    @Autowired
    private SitesList sitesList;

    @Autowired
    private PageService pageService;

    @Autowired
    private SiteService siteService;

    @Autowired
    public IndexingService(SiteRepository siteRepository, PageRepository pageRepository,
                           LemmaRepository lemmaRepository, IndexRepository indexRepository) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
    }

    private boolean isIndexingInProgress = false;

    @Async
    public CompletableFuture<Void> startIndexing(List<Site> sites) {
        logger.info("Starting indexing process...");
        if (sites == null || sites.isEmpty()) {
            logger.warn("No sites found in configuration.");
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try {
                sites.stream()
                    .map(this::createNewSiteEntry)
                    .filter(Objects::nonNull)
                    .forEach(modifiedSite -> {
                        try {
                            String url = modifiedSite.getUrl();
                            processSitePages(modifiedSite, url);
                            pageService.indexPage(url);
                            updateSiteStatus(modifiedSite, Status.INDEXED);
                        } catch (IOException e) {
                            logger.error("Error indexing page: {}", modifiedSite.getUrl(), e);
                        }
                    });
            } catch (Exception e) {
                logger.error("Error during indexing process", e);
            } finally {
                logger.info("Indexing process completed.");
            }
        });
    }

    public List<SearchResult> search(String query, String site, int offset, int limit) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Поисковый запрос не может быть пустым");
        }

        List<String> lemmas = extractLemmas(query);
        if (lemmas.isEmpty()) {
            return Collections.emptyList();
        }

        Optional<Site> siteEntity = Optional.empty();
        if (site != null && !site.isEmpty()) {
            siteEntity = siteRepository.findByUrl(site);
            if (siteEntity.isEmpty()) {
                throw new IllegalArgumentException("Сайт с URL " + site + " не найден");
            }
        }

        List<Page> pages = siteEntity.isPresent()
                ? pageRepository.findPagesByLemmasAndSite(lemmas, siteEntity.get().getId())
                : pageRepository.findPagesByLemmas(lemmas);

        List<SearchResult> results = calculateRelevance(pages, lemmas);
        return results.stream()
                .sorted(Comparator.comparingDouble(SearchResult::getRelevance).reversed())
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());
    }

    public int countSearchResults(String query, String site) {
        if (query == null || query.trim().isEmpty()) {
            return 0;
        }
    
        List<String> lemmas = extractLemmas(query);
        if (lemmas.isEmpty()) {
            return 0;
        }
    
        Optional<Site> siteEntity = Optional.empty();
        if (site != null && !site.isEmpty()) {
            siteEntity = siteRepository.findByUrl(site);
            if (siteEntity.isEmpty()) {
                return 0;
            }
        }
    
        // Теперь, если сайт найден, используем метод countPagesByLemmasAndSite,
        // если не найден — countPagesByLemmas
        return siteEntity.isPresent()
                ? pageRepository.findPagesByLemmasAndSite(lemmas, siteEntity.get().getId()).size()
                : pageRepository.findPagesByLemmas(lemmas).size();
    }

    private List<String> extractLemmas(String query) {
        return Arrays.stream(query.split("\\s+"))
                .map(String::toLowerCase)
                .distinct()
                .filter(lemma -> lemma.length() > 2)
                .collect(Collectors.toList());
    }
    private List<SearchResult> calculateRelevance(List<Page> pages, List<String> lemmas) {
        // Вычисляем максимальную релевантность
        double maxRelevance = pages.stream()
                                   .mapToDouble(page -> calculatePageRelevance(page, lemmas))
                                   .max()
                                   .orElse(0); // если список страниц пустой, то maxRelevance будет 0
       
        Map<Page, Double> relevanceMap = new HashMap<>();
    
        // Заполняем карту с релевантностью
        for (Page page : pages) {
            double relevance = calculatePageRelevance(page, lemmas);
            relevanceMap.put(page, relevance);
        }
    
        // Преобразуем карты в результаты
        return relevanceMap.entrySet().stream()
                .map(entry -> {
                    Page page = entry.getKey();
                    double normalizedRelevance = entry.getValue() / maxRelevance;
                    return new SearchResult(
                            page.getSite().getUrl(),
                            page.getSite().getName(),
                            page.getPath(),
                            page.getTitle(),
                            generateSnippet(page.getContent(), lemmas),
                            normalizedRelevance
                    );
                })
                .collect(Collectors.toList());
    }
    
    private double calculatePageRelevance(Page page, List<String> lemmas) {
        return lemmas.stream()
                .mapToDouble(lemma -> {
                    try {
                        return pageRepository.findLemmaRank(page.getId(), lemma);
                    } catch (Exception e) {
                        return 0.0; // Возвращаем 0.0 для отсутствующих данных
                    }
                })
                .sum();
    }
    
    

    private String generateSnippet(String content, List<String> lemmas) {
        String snippet = content;
        for (String lemma : lemmas) {
            snippet = snippet.replaceAll("(?i)" + lemma, "<b>" + lemma + "</b>");
        }
        return snippet.length() > 200 ? snippet.substring(0, 200) + "..." : snippet;
    }

    private Site createNewSiteEntry(Site siteConfig) {
        if (siteConfig == null) {
            return null;
        }

        Site site = new Site();
        site.setStatus(Status.INDEXING);
        site.setUrl(siteConfig.getUrl());
        site.setName(siteConfig.getName());
        site.setStatusTime(new Timestamp(System.currentTimeMillis()));

        Site savedSite = siteRepository.save(site);
        logger.info("Сайт сохранен: {}", savedSite);
        return savedSite;
    }

    private void updateSiteStatus(Site site, Status status) {
        site.setStatus(status);
        site.setStatusTime(new Timestamp(System.currentTimeMillis()));
        siteRepository.save(site);
    }

    private void updateSiteLastError(Site site, String error) {
        site.setLastError(error);
        siteRepository.save(site);
    }

    public String cleanHtmlTags(String htmlContent) {
        Document doc = Jsoup.parse(htmlContent);
        return doc.text();
    }
    // Метод для расчета ранга леммы на странице
private float calculateRank(String content, String lemma) {
    // Логика для вычисления ранга, например, основанная на частоте встречаемости леммы на странице
    int occurrences = countOccurrences(content, lemma);
    return (float) occurrences;  // Пример: просто возвращаем количество вхождений леммы
}// Метод для подсчета количества вхождений леммы в контент
private int countOccurrences(String content, String lemma) {
    int count = 0;
    int index = 0;
    while ((index = content.indexOf(lemma, index)) != -1) {
        count++;
        index += lemma.length();  // Пропускаем уже найденную лемму
    }
    return count;
}public void processSitePages(Site site, String url) throws IOException {
    // Проверяем входные параметры
    if (site == null || url == null || url.isEmpty()) {
        logger.error("Ошибка: site или URL не могут быть null или пустыми. site: {}, url: {}", site, url);
        throw new IllegalArgumentException("Site или URL не могут быть null или пустыми.");
    }

    try {
        // Загружаем страницу с помощью JSoup с тайм-аутом и проверкой статуса
        Connection connection = Jsoup.connect(url).timeout(5000); // Устанавливаем тайм-аут
        Document document = connection.get();
        
        int statusCode = document.connection().response().statusCode();
        if (statusCode != 200) {
            logger.warn("Получен нестандартный HTTP-статус для URL {}: {}", url, statusCode);
            return; // Возвращаемся, если статус не 200
        }

        // Извлекаем основную информацию о странице
        String title = document.title();
        String content = document.body().text(); // Текстовое содержимое страницы

        // Проверка на пустое содержимое
        if (content.isEmpty()) {
            logger.warn("Страница по URL {} не содержит текста.", url);
            return;
        }

        // Создаем и сохраняем объект Page
        Page page = new Page();
        page.setSite(site);
        page.setPath(url); // URL используется как путь страницы
        page.setCode(statusCode); // HTTP-код ответа
        page.setContent(content); // Сохраняем текстовое содержимое
        Page savedPage = pageRepository.save(page);

        // Обрабатываем леммы на странице
        List<String> lemmas = extractLemmas(content); // Метод для извлечения лемм
        for (String lemma : lemmas) {
            processLemmaAndIndex(savedPage, lemma, content);
        }

    }catch (Exception e) {
        // Логируем любую другую ошибку
        logger.error("Неизвестная ошибка при обработке URL: {}. Сообщение об ошибке: {}", url, e.getMessage(), e);
        throw new RuntimeException("Неизвестная ошибка при обработке URL: " + url, e);
    }
        // Логируем ошибку с деталями
        
}


@Transactional
private void processLemmaAndIndex(Page savedPage, String lemma, String content) {
    // Проверяем, существует ли лемма в базе
    Lemma lemmaEntity = lemmaRepository.findByLemmaText(lemma);
    if (lemmaEntity == null) {
        // Если лемма не найдена, создаём и сохраняем новую
        lemmaEntity = new Lemma();
        lemmaEntity.setLemmaText(lemma);
        lemmaEntity = lemmaRepository.save(lemmaEntity); // Сохраняем и сразу получаем сохранённый объект
    }

    // Преобразуем pageId в String
    String pageIdString = String.valueOf(savedPage.getId());

    // Проверяем, существует ли уже индекс с таким pageId и lemma
    Index existingIndex = indexRepository.findByPageIdAndLemma(pageIdString, lemma);
    
    // Если индекс не найден, создаем новый
    if (existingIndex == null) {
        // Создаем индекс для страницы и леммы
        Index index = new Index();
        index.setPageId(savedPage.getId()); // Используем ID страницы
        index.setLemma(lemmaEntity.getLemmaText()); // Используем текст леммы
        index.setRank(calculateRank(content, lemma)); // Метод для расчета ранга
        indexRepository.save(index);
    } else {
        // Если индекс найден, обновляем его (если нужно)
        existingIndex.setRank(calculateRank(content, lemma));
        indexRepository.save(existingIndex);
    }
}




/**
 * Обрабатывает список URL для указанного сайта.
 *
 * @param site объект сайта
 * @param urls список URL
 */
public void processSitePages(Site site, List<String> urls) {
    if (site == null || urls == null || urls.isEmpty()) {
        throw new IllegalArgumentException("Site или список URL не могут быть null или пустыми.");
    }

    for (String url : urls) {
        try {
            processSitePages(site, url);
        } catch (IOException e) {
            // Логируем ошибку, чтобы не прерывать обработку остальных URL
            System.err.println("Ошибка обработки URL: " + url + " - " + e.getMessage());
        }
    }
}

}