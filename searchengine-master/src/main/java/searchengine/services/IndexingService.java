package searchengine.services;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.entity.Index;
import searchengine.entity.Lemma;
import searchengine.entity.Site;
import searchengine.model.*;

import java.io.IOException;
import java.sql.Timestamp;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.SiteRepository;
import searchengine.repository.PageRepository;
import searchengine.entity.Page;

@Service
public class IndexingService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    @Autowired
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
    public void startIndexing(List<Site> siteConfigs) {
        PageService pageService = new PageService(pageRepository, lemmaRepository, indexRepository, siteRepository);

        for (Site siteConfig : siteConfigs) {
            Site site = null;

            try {
                site = createNewSiteEntry(siteConfig);

                if(site != null) {
                    String url = site.getUrl();
                    processSitePages(site, url);

                    pageService.indexPage(url); // передаем URL страницы, а не объект Site
                    updateSiteStatus(site, Status.INDEXED);
                } else {
                    throw new Exception("Failed to create Site object");
                }

            } catch (Exception e) {
                // Логируем ошибку
                System.out.println("Error occurred: " + e.getMessage());
                if(site != null) {
                    updateSiteStatus(site, Status.FAILED);
                    updateSiteLastError(site, e.getMessage());
                } else {
                    System.out.println("Site object is null, cannot update status or last error message");
                }
            }
        }
    }












    private Site createNewSiteEntry(Site siteConfig) {
        if (siteConfig == null) {
            return null; // Лучше обрабатывать ситуацию, когда siteConfig равен null
        }

        Site site = new Site();
        site.setStatus(Status.INDEXING);
        site.setUrl(siteConfig.getUrl());
        site.setName(siteConfig.getName());
        site.setStatusTime(new Timestamp(System.currentTimeMillis()));

        // Логгирование перед вызовом save
        System.out.println("Попытка сохранить сайт: " + site);

        Site savedSite = siteRepository.save(site);

        // Логгирование после вызова save
        System.out.println("Сайт сохранен: " + savedSite);

        return savedSite;
    }


    public Page createNewPageEntry(Site site, String pageURL) {
        // Создаем новую запись о странице
        Page page = new Page();
        page.setSite(site);
        page.setPath(pageURL);

        // Сохраняем запись о странице в базу данных
        return pageRepository.save(page);
    }
    public void processSitePages(Site site, String url) throws IOException {
        Document doc = Jsoup.connect(url).get();
        String content = doc.outerHtml();

        if (content != null && !content.isEmpty()) {
            Page page = new Page();
            page.setSite(site);
            page.setPath(url);
            page.setCode(200);
            page.setContent(content);
            pageRepository.save(page);

            Set<String> uniqueLinks = new HashSet<>();
            Elements links = doc.select("a[href]");
            ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);

            for (Element link : links) {
                String linkUrl = link.absUrl("href");

                if (isValidLink(linkUrl) && !uniqueLinks.contains(linkUrl)) {
                    uniqueLinks.add(linkUrl);
                    executor.schedule(() -> {
                        try {
                            Page newPage = createNewPageEntry(site, linkUrl);
                            processPageContent(newPage, linkUrl);
                        } catch (Exception e) {
                            // Обработка ошибок
                        }
                    }, 500 + (long) (Math.random() * 4500), TimeUnit.MILLISECONDS);
                }
            }

            // Ждем завершения всех запланированных задач
            executor.shutdown();
            try {
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            updateSiteLastError(site, "Failed to retrieve HTML content for URL: " + url);
        }
    }







    public void processPageContent(Page page, String url) {
        try {
            // Получаем страницу с сайта
            Document doc = Jsoup.connect(url).get();

            // Получаем текст страницы без тегов
            String text = cleanHtmlTags(doc.html());

            // Лемматизация текста страницы
            String[] tokens = text.split("\s+"); // Разделение текста на токены
            List<String> lemmas = new ArrayList<>();

            // Пример простой лемматизации: приведение всех слов к нижнему регистру
            for (String token : tokens) {
                lemmas.add(token.toLowerCase());
            }

            // Сохраняем леммы в базу данных
            for (String lemmaText : lemmas) {
                Lemma lemma = new Lemma();
                lemma.setLemmaText(lemmaText);
                lemma = lemmaRepository.save(lemma);

                // Создаем индекс и заполняем информацией о лемме
                Index index = new Index();
                index.setPageId(page.getId());
                index.setLemmaId(lemma.getId());
                index.setLemma(lemmaText);
                index.setRank(1.0f); // Пример значения для rank
                indexRepository.save(index);
            }

        } catch (IOException e) {
            System.out.println("Error while processing page content: " + e.getMessage());
        }
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
    private boolean isValidLink(String url) {
        // Проверка наличия протокола http(s)
        if(!url.startsWith("http://") && !url.startsWith("https://")) {
            return false;
        }

        // Проверка формата домена
        String domain = url.split("/")[2];
        if(!domain.endsWith(".com") && !domain.endsWith(".net") && !domain.endsWith(".org") && !domain.endsWith(".ru")) {
            return false;
        }
// Дополнительная проверка на наличие конечного слеша
        if (url.endsWith("/")) {
            return false; // В конце URL не должно быть слеша
        }

        return true; // Если все проверки пройдены успешно
    }

}
