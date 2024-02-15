package searchengine.services;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import searchengine.model.*;

import java.io.IOException;
import java.sql.Timestamp;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class IndexingService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;


    public IndexingService(SiteRepository siteRepository, PageRepository pageRepository) {
        this.siteRepository = siteRepository;

        this.pageRepository = pageRepository;
    }

    public void startIndexing(List<Site> siteConfigs) {
        for (Site siteConfig : siteConfigs) {
            try {
                Site site = createNewSiteEntry(siteConfig);
                processSitePages(site, siteConfig.getUrl());
                updateSiteStatus(site, Status.INDEXED);
            } catch (Exception e) {
                // Обработка ошибки и обновление статуса на FAILED
                updateSiteStatus(siteConfig, Status.FAILED);
                updateSiteLastError(siteConfig, e.getMessage());
            }
        }
    }

    private Site createNewSiteEntry(Site siteConfig) {
        Site site = new Site();
        site.setStatus(Status.INDEXING);
        site.setUrl(siteConfig.getUrl());
        site.setName(siteConfig.getName());
        site.setStatusTime(new Timestamp(System.currentTimeMillis()));
        return siteRepository.save(site);
    }

    //Обработка страницы
    public void processSitePages(Site site, String url) throws IOException, InterruptedException {
        // Обработка страницы
        // TODO: Добавьте код для обработки страницы и сохранения в базу данных

        //Пример кода сохранения страницы в базу данных
        Page page = new Page();
        page.setSite(site);
        page.setPath("/PlayBack.Ru");
        page.setCode(200);
        page.setContent("<!DOCTYPE html><html><head><title>Инт...</html>");
        // Проверка перед сохранением в базу данных

        pageRepository.save(page);

        Set<String> uniqueLinks = new HashSet<>();
        String userAgent = "HeliontSearchBot";
        String referrer = "http://www.google.com";
        Document doc = Jsoup.connect(url)
                .userAgent(userAgent)
                .referrer(referrer)
                .get();

        Elements links = doc.select("a[href]");

        for (Element link : links) {
            String linkUrl = link.absUrl("href");
            if (isValidLink(linkUrl)) {
                try {
                    Page newPage = createNewPageEntry(site, linkUrl);
                    processPageContent(newPage, linkUrl);
                } catch (Exception e) {
                    // Обработка ошибки и пропуск страницы
                }

                // задержка перед следующим запросом
                Thread.sleep((long) (500 + Math.random() * 4500));
            }
        }

        for (String linkUrl : uniqueLinks) {
            try {
                Page newPage = createNewPageEntry(site, linkUrl);
                processPageContent(newPage, linkUrl);
                uniqueLinks.add(linkUrl);
            } catch (Exception e) {
                // Обработка ошибки и пропуск страницы
            }

            // задержка перед следующим запросом
            Thread.sleep((long) (500 + Math.random() * 4500));
        }
    }


    private boolean isValidLink(String linkUrl) {
        // Дополнительная проверка ссылки, если необходимо
        return true;
    }

    private Page createNewPageEntry(Site site, String url) {
        Page page = new Page();
        page.setSite(site);
        page.setPath(url);
        page.setCode(200); // Здесь можно обработать код ответа сервера, если это необходимо
        return pageRepository.save(page);
    }

    private void processPageContent(Page page, String url) throws IOException {
        // Здесь можно обработать содержимое страницы, если это необходимо
        String content = ""; // Пример пустого содержимого
        page.setContent(content);
        pageRepository.save(page);
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
}
