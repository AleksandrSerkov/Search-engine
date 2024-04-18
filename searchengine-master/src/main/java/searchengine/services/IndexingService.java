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
    public void startIndexing(List<Site> siteConfigs) {
        // Создание экземпляра PageService с передачей репозиториев в конструктор
        PageService pageService = new PageService(pageRepository, lemmaRepository, indexRepository, siteRepository);

        for (Site siteConfig : siteConfigs) {
            try {
                Site site = createNewSiteEntry(siteConfig);
                processSitePages(site, siteConfig.getUrl());

                // Вызов метода indexPage у PageService
                pageService.indexPage(siteConfig.getUrl());

                updateSiteStatus(site, Status.INDEXED);
            } catch (Exception e) {
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
    public Page createNewPageEntry(Site site, String pageURL) {
        // Создаем новую запись о странице
        Page page = new Page();
        page.setSite(site);
        page.setPath(pageURL);

        // Сохраняем запись о странице в базу данных
        return pageRepository.save(page);
    }


    public void processSitePages(Site site, String url) throws IOException, InterruptedException {

        // Получаем страницу с сайта
        Document doc = Jsoup.connect(url)
                .get();

        // Получаем HTML-код страницы
        String content = doc.outerHtml();

        // Сохраняем HTML-код страницы в базу данных
        Page page = new Page();
        page.setSite(site);
        page.setPath(url); // Устанавливаем путь страницы
        page.setCode(200); // Устанавливаем код ответа
        page.setContent(content); // Устанавливаем содержимое страницы
        pageRepository.save(page); // Сохраняем страницу

        Set<String> uniqueLinks = new HashSet<>();
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

                // Задержка перед следующим запросом
                Thread.sleep((long) (500 + Math.random() * 4500));
                uniqueLinks.add(linkUrl); // Добавить ссылку в uniqueLinks
            }
        }
    }
    public void processPageContent(Page page, String url) {
        try {
            // Получаем страницу с сайта
            Document doc = Jsoup.connect(url).get();

            // Получаем текст страницы без тегов
            String text = cleanHtmlTags(doc.html());

            // Лемматизация текста страницы (код для лемматизации)

            // Сохраняем леммы в базу данных
            Lemma lemma = new Lemma();
            lemma.setLemmaText("lemma_text");
            lemma = lemmaRepository.save(lemma);

            // Создаем индекс и заполняем информацией о лемме
            Index index = new Index();
            index.setPageId(page.getId());
            index.setLemmaId(lemma.getId());
            index.setLemma("lemma_text");
            index.setRank(1.0f); // Пример значения для rank
            indexRepository.save(index);

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
