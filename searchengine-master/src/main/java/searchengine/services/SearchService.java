package searchengine.services;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import searchengine.dto.statistics.SearchResultDTO;
import searchengine.entity.Page;
import searchengine.entity.Site;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

@Service
public class SearchService {

    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);

    private static final int DEFAULT_MIN_WORD_LENGTH = 3; // Возможность настройки минимальной длины слова

    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;

    public SearchService(LemmaRepository lemmaRepository, PageRepository pageRepository, SiteRepository siteRepository) {
        this.lemmaRepository = lemmaRepository;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
    }

    public List<SearchResultDTO> search(String query, String site, int offset, int limit) {
        if (query == null || query.trim().isEmpty()) {
            String message = "Поисковый запрос не может быть пустым.";
            logger.warn(message);
            throw new IllegalArgumentException(message);
        }

        List<String> lemmas = extractLemmas(query, DEFAULT_MIN_WORD_LENGTH);
        if (lemmas.isEmpty()) {
            String message = "Не найдено лемм для запроса: '" + query + "'";
            logger.info(message);
            throw new IllegalArgumentException(message);
        }

        if (site != null && !site.isEmpty()) {
            Optional<Site> siteEntity = siteRepository.findByUrl(site);
            if (siteEntity.isEmpty()) {
                String message = "Сайт с URL '" + site + "' не проиндексирован.";
                logger.info(message);
                throw new IllegalArgumentException(message);
            }
        }

        List<Page> pages = filterPagesByLemmas(lemmas, site);
        List<SearchResultDTO> results = calculateRelevance(pages, lemmas);

        if (results.isEmpty()) {
            String message = "По вашему запросу ничего не найдено.";
            logger.info(message);
            return Collections.emptyList(); // Возвращаем пустой список, а не исключение
        }

        return results.stream()
                .sorted(Comparator.comparingDouble(SearchResultDTO::getRelevance).reversed())
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Метод для извлечения лемм из запроса с учетом минимальной длины слова.
     * @param query          Исходный поисковый запрос.
     * @param minWordLength  Минимальная длина слова для учета.
     * @return Список уникальных лемм.
     */
    private List<String> extractLemmas(String query, int minWordLength) {
        return Arrays.stream(query.toLowerCase().split("[,\\s]+"))
                .map(this::lemmatizeWord)
                .filter(word -> word.length() >= minWordLength) // Гибкая настройка длины
                .distinct()
                .collect(Collectors.toList());
    }
    private String lemmatizeWord(String word) {
        return word.replaceAll("\\d", "").replaceAll("\\W", "");
    }

    private List<Page> filterPagesByLemmas(List<String> lemmas, String site) {
        List<Page> pages;
        if (site != null && !site.isEmpty()) {
            Optional<Site> siteEntity = siteRepository.findByUrl(site);
            if (siteEntity.isEmpty()) {
                logger.info("Сайт '{}' не найден в базе.", site);
                return Collections.emptyList();
            }
            pages = pageRepository.findPagesByLemmasAndSite(lemmas, siteEntity.get().getId());
        } else {
            pages = pageRepository.findPagesByLemmas(lemmas);
        }

        // Фильтруем страницы: оставляем только те, где присутствуют ВСЕ леммы
        return pages.stream()
                .filter(page -> lemmas.stream().allMatch(lemma -> page.getContent().toLowerCase().contains(lemma)))
                .collect(Collectors.toList());
    }

    private List<SearchResultDTO> calculateRelevance(List<Page> pages, List<String> lemmas) {
        Map<Page, Double> relevanceMap = new HashMap<>();

        for (Page p : pages) {
            double relevance = calculatePageRelevance(p, lemmas);
            relevanceMap.put(p, relevance);
        }

        double maxRelevance = relevanceMap.values().stream().max(Double::compare).orElse(1.0);

        return relevanceMap.entrySet().stream()
                .map(entry -> {
                    Page p = entry.getKey();
                    String siteUrl = p.getSite().getUrl();
                    String pagePath = p.getPath();
                    String fullUrl = pagePath.startsWith("http") ? pagePath 
                            : siteUrl + (pagePath.startsWith("/") ? "" : "/") + pagePath;
                    String baseUrl = extractBaseUrl(fullUrl);
                    String relativeUri = extractRelativeUrl(fullUrl);
                    String fileName = extractFileName(fullUrl, siteUrl);

                    logger.info("Создан результат: fileName={}, Title={}", fileName, p.getTitle());

                    return new SearchResultDTO(
                            baseUrl,
                            relativeUri,
                            p.getTitle(),
                            generateSnippet(p.getContent(), lemmas),
                            entry.getValue() / maxRelevance,
                            p.getSite().getName(),
                            fileName
                    );
                })
                .collect(Collectors.toList());
    }

    private String extractBaseUrl(String fullUrl) {
        try {
            java.net.URL url = new java.net.URL(fullUrl);
            return url.getProtocol() + "://" + url.getHost();
        } catch (Exception e) {
            return fullUrl;
        }
    }

    private String extractRelativeUrl(String fullUrl) {
        try {
            java.net.URL url = new java.net.URL(fullUrl);
            String path = url.getPath();
            return (path != null) ? path : "";
        } catch (Exception e) {
            return "";
        }
    }

    private String extractFileName(String fullUrl, String siteUrl) {
        if (fullUrl == null || fullUrl.isEmpty()) return "";
        int lastSlash = fullUrl.lastIndexOf('/');
        String extracted = (lastSlash != -1 && lastSlash < fullUrl.length() - 1)
                ? fullUrl.substring(lastSlash + 1)
                : "";
        return extracted.isEmpty() ? siteUrl.replaceFirst("^https?://", "") : extracted;
    }

    private double calculatePageRelevance(Page page, List<String> lemmas) {
        String content = page.getContent().toLowerCase();
        return lemmas.stream()
                .mapToDouble(lemma -> countOccurrences(content, lemma))
                .sum();
    }

    private int countOccurrences(String content, String lemma) {
        int count = 0;
        int index = 0;
        while ((index = content.indexOf(lemma, index)) != -1) {
            count++;
            index += lemma.length();
        }
        return count;
    }

    private String generateSnippet(String content, List<String> lemmas) {
        for (String lemma : lemmas) {
            content = content.replaceAll("(?i)" + lemma, "<b>" + lemma + "</b>");
        }
        return content.length() > 200 ? content.substring(0, 200) + "..." : content;
    }
}

