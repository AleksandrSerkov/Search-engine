package searchengine.services;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.springframework.stereotype.Service;

import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.repository.SiteRepository;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    private final Random random = new Random();
    private final SitesList sites;
    private final SiteRepository siteRepository;

    public StatisticsServiceImpl(SitesList sites, SiteRepository siteRepository) {
        this.sites = sites;
        this.siteRepository = siteRepository;
    }

    @Override
    public StatisticsResponse getStatistics() {
        String[] statuses = { "INDEXED", "FAILED", "INDEXING" };
        String[] errors = {
                "Ошибка индексации: главная страница сайта не доступна",
                "Ошибка индексации: сайт не доступен",
                ""
        };

        TotalStatistics total = new TotalStatistics();
        List<Site> configSites = sites.getSites();
        System.out.println("Sites list size: " + configSites.size()); // Логирование размера списка

        // Проверка на пустой список и вывод значения в консоль
        boolean isSiteListEmpty = configSites.isEmpty();
        System.out.println("Is sites list empty? " + isSiteListEmpty);

        if (isSiteListEmpty) {
            System.out.println("Sites list is empty.");
            total.setSites(0);
            total.setPages(0);
            total.setLemmas(0);
            total.setIndexing(false);

            StatisticsResponse response = new StatisticsResponse();
            StatisticsData data = new StatisticsData();
            data.setTotal(total);
            data.setDetailed(Collections.emptyList()); // Пустой список детализированной статистики
            response.setStatistics(data);
            response.setResult(true);

            return response;
        }

        System.out.println("Processing sites list...");

        // Если список не пустой, выполняем основную логику
        total.setSites(configSites.size());
        total.setIndexing(true);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        for (int i = 0; i < configSites.size(); i++) {
            Site site = configSites.get(i);
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
            int pages = random.nextInt(1_000);
            int lemmas = pages * random.nextInt(1_000);
            item.setPages(pages);
            item.setLemmas(lemmas);
            item.setStatus(statuses[i % 3]);
            item.setError(errors[i % 3]);
            item.setStatusTime(System.currentTimeMillis() - (random.nextInt(10_000)));
            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
            detailed.add(item);
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
