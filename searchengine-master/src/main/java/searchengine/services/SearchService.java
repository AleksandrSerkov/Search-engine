package searchengine.services;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import searchengine.entity.Page;
import searchengine.entity.Site;
import searchengine.model.SearchResult;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

@Service
public class SearchService {

    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;

    public SearchService(LemmaRepository lemmaRepository, PageRepository pageRepository, SiteRepository siteRepository) {
        this.lemmaRepository = lemmaRepository;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
    }

    public List<SearchResult> search(String query, String site, int offset, int limit) {
        // Разбиение запроса на леммы
        List<String> lemmas = extractLemmas(query);

        if (lemmas.isEmpty()) {
            return Collections.emptyList();
        }

        // Фильтрация страниц по леммам
        List<Page> pages = filterPagesByLemmas(lemmas, site);

        // Рассчитываем релевантность страниц
        List<SearchResult> results = calculateRelevance(pages, lemmas);

        // Сортировка по релевантности и постраничный вывод
        return results.stream()
                .sorted(Comparator.comparingDouble(SearchResult::getRelevance).reversed())
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());
    }

    public int countSearchResults(String query, String site) {
        List<String> lemmas = extractLemmas(query);
        if (lemmas.isEmpty()) {
            return 0;
        }

        List<Page> pages = filterPagesByLemmas(lemmas, site);
        return pages.size();
    }

    private List<String> extractLemmas(String query) {
        // Реализуйте разбиение строки на леммы и исключение ненужных слов
        // Пример: вызов метода лемматизации
        return List.of(query.split("\\s+")).stream()
                .map(this::lemmatizeWord)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private String lemmatizeWord(String word) {
        // Реализуйте лемматизацию слова
        return word.toLowerCase(); // Пример: приведение к нижнему регистру
    }

    private List<Page> filterPagesByLemmas(List<String> lemmas, String site) {
        // Фильтрация страниц по леммам и сайту
        if (site != null && !site.isEmpty()) {
            Optional<Site> siteEntity = siteRepository.findByUrl(site);
            if (siteEntity.isEmpty()) {
                return Collections.emptyList();
            }
            // Передаем siteId вместо объекта Site
            return pageRepository.findPagesByLemmasAndSite(lemmas, siteEntity.get().getId());
        } else {
            return pageRepository.findPagesByLemmas(lemmas);
        }
    }
    

    private List<SearchResult> calculateRelevance(List<Page> pages, List<String> lemmas) {
        // Рассчитываем абсолютную релевантность страниц
        Map<Page, Double> relevanceMap = new HashMap<>();
    
        for (Page page : pages) {
            double relevance = calculatePageRelevance(page, lemmas);
            relevanceMap.put(page, relevance);
        }
    
        // Вычисляем максимальную релевантность
        double maxRelevance = relevanceMap.values().stream()
                .max(Double::compare)
                .orElse(1.0); // Защита от деления на 0
    
        // Преобразование в список SearchResult с нормализацией релевантности
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
        // Рассчитываем абсолютную релевантность страницы
        return lemmas.stream()
                .mapToDouble(lemma -> pageRepository.findLemmaRank(page.getId(), lemma))
                .sum();
    }
    
    

    private String generateSnippet(String content, List<String> lemmas) {
        // Создаем сниппет с выделением ключевых слов
        String snippet = content;
        for (String lemma : lemmas) {
            snippet = snippet.replaceAll("(?i)" + lemma, "<b>" + lemma + "</b>");
        }
        return snippet.length() > 200 ? snippet.substring(0, 200) + "..." : snippet;
    }
}

