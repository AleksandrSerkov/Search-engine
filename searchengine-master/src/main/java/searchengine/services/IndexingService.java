package searchengine.services;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import searchengine.model.*;

import java.io.IOException;
import java.sql.Timestamp;

import java.util.*;

import searchengine.model.SiteRepository;
import searchengine.model.PageRepository;
import searchengine.model.Page;

@Service
public class IndexingService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    public IndexingService(SiteRepository siteRepository, PageRepository pageRepository, LemmaRepository lemmaRepository, IndexRepository indexRepository) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;

    }// Метод для индексирования страницы





    // Метод для очистки HTML-тегов
    public String cleanHtmlTags(String htmlContent) {
        Document doc = Jsoup.parse(htmlContent);
        String text = doc.text();
        return text;
    }

    // Этот метод начинает процесс индексации указанного сайта.
   // Алгоритм включает в себя поиск и сохранение информации с сайта в базу данных.
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


        //сохранения страницы в базу данных
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
                uniqueLinks.add(linkUrl); // Добавить ссылку в uniqueLinks
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

        return linkUrl.contains("example.com");
    }

    private Page createNewPageEntry(Site site, String url) {
        Page page = new Page();
        page.setSite(site);
        page.setPath(url);
        page.setCode(200);
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
