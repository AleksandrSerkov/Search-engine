package searchengine.services;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import searchengine.config.SitesList;
import searchengine.entity.Index;
import searchengine.entity.Lemma;
import searchengine.entity.Page;
import searchengine.entity.Site;
import searchengine.model.Status;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.utils.XmlMapperFactory;
@Service
public class IndexingService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
  
    @Autowired
    private  SitesList sitesList;
    @Autowired
    private  PageService pageService;
    @Autowired
    private  SiteService siteService;

    private List<Site> sites;
    private boolean isIndexingInProgress = false;
    @Autowired
    public IndexingService(SiteRepository siteRepository, PageRepository pageRepository, LemmaRepository lemmaRepository, IndexRepository indexRepository) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        
    }
    private static final Logger logger = LoggerFactory.getLogger(IndexingService.class);


    @Async
    public void startIndexing(List<Site> sites) {
        logger.info("Starting indexing process...");

        try {
            this.sites = sites;

            if (sites.isEmpty()) {
                logger.warn("No sites found in configuration.");
                return;
            }

            // Используем стрим для обработки каждого сайта
            sites.stream()
                    .map(this::createNewSiteEntry)
                    .filter(modifiedSite -> modifiedSite != null)
                    .forEach(modifiedSite -> {
                        String url = modifiedSite.getUrl();
                        processSitePages(modifiedSite, url);
                        try {
                            pageService.indexPage(url);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        updateSiteStatus(modifiedSite, Status.INDEXED);
                    });

        } catch (Exception e) {
            logger.error("Error during indexing process", e);
        } finally {
            logger.info("Indexing process completed.");
        }
    }



    // Метод для индексирования страницы
    public String cleanHtmlTags(String htmlContent) {
        Document doc = Jsoup.parse(htmlContent);
        String text = doc.text();
        return text;
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

    public void processAllSitesPages() {
        List<Site> sites = siteRepository.findAll();
    
        for (Site site : sites) {
            String url = site.getUrl(); // Получаем URL страницы напрямую
    
            processSitePages(site, url); // Передаем объект site и URL страницы
        }
    }
    
    
    
    public void processSitePages(Site site, String url) {
    XmlMapper xmlMapper = XmlMapperFactory.createXmlMapper(); // Создаем XmlMapper через фабрику

    try {
        // Логируем начало обработки страницы
        System.out.println("Starting to process URL: " + url);

        // Устанавливаем таймаут для подключения
        Document doc = Jsoup.connect(url).timeout(10000).get();
        String content = doc.text(); // Convert HTML content to plain text

        // Конвертация контента в XML
        String xmlContent = toXml(content, xmlMapper);

        // Ограничение на размер контента
        int maxContentLength = 16 * 1024 * 1024; // 16 MB в байтах

        // Если контент превышает максимальный размер, обрезаем его
        byte[] xmlContentBytes = xmlContent.getBytes(StandardCharsets.UTF_8);
        if (xmlContentBytes.length > maxContentLength) {
            xmlContent = new String(Arrays.copyOf(xmlContentBytes, maxContentLength), StandardCharsets.UTF_8);
        }

        // Проверка на null и пустоту содержимого
        if (xmlContent == null || xmlContent.isEmpty()) {
            System.out.println("Failed to retrieve HTML content for URL: " + url);
            updateSiteLastError(site, "Failed to retrieve HTML content for URL: " + url);
        } else {
            // Создаем объект страницы для сохранения в базу данных
            Page page = new Page();
            page.setSite(site);
            page.setPath(url);
            page.setCode(200);
            page.setContent(xmlContent);

            // Сохраняем страницу в базу данных
            pageRepository.save(page);

            // Обработка ссылок на другие страницы
            processLinks(site, doc);
        }

        // Логируем успешное завершение обработки страницы
        System.out.println("Successfully processed URL: " + url);
    } catch (IOException e) {
        updateSiteLastError(site, "Error processing URL: " + url + ". Error: " + e.getMessage());
        e.printStackTrace();
    } catch (DataIntegrityViolationException e) {
        updateSiteLastError(site, "Data integrity violation: " + e.getMessage());
        e.printStackTrace();
    } catch (Exception e) {
        updateSiteLastError(site, "Unexpected error: " + e.getMessage());
        e.printStackTrace();
    }
}
    
    private String toXml(String content, XmlMapper xmlMapper) throws IOException {
        StringWriter writer = new StringWriter();
        xmlMapper.writeValue(writer, content);
        return writer.toString();
    }




    private void processLinks(Site site, Document doc) {
        Elements links = doc.select("a[href]");
        Set<String> uniqueLinks = new HashSet<>();

        for (Element link : links) {
            String linkUrl = link.absUrl("href");

            if (isValidLink(linkUrl) && !uniqueLinks.contains(linkUrl)) {
                uniqueLinks.add(linkUrl);

                try {
                    // Создаем новую запись о странице и обрабатываем ее контент
                    Page newPage = createNewPageEntry(site, linkUrl);
                    processPageContent(newPage, linkUrl);
                } catch (Exception e) {
                    // Логируем ошибку обработки страницы
                    System.err.println("Error processing page: " + linkUrl + ". Error: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
    public void processPageContent(Page page, String url) {
        try {
            // Получаем страницу с сайта
            Document doc = Jsoup.connect(url).get();

            // Получаем текст страницы без тегов
            String text = cleanHtmlTags(doc.html());

            // Лемматизация текста страницы
            String[] tokens = text.split("\\s+"); // Разделение текста на токены
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
            // Логируем ошибку обработки содержимого страницы
            System.err.println("Error while processing page content: " + e.getMessage());
            e.printStackTrace();
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
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return false;
        }

        // Проверка формата домена
        String domain = url.split("/")[2];
        if (!domain.endsWith(".com") && !domain.endsWith(".net") && !domain.endsWith(".org") && !domain.endsWith(".ru")) {
            return false;
        }

        // Дополнительная проверка на наличие конечного слеша
        if (url.endsWith("/")) {
            return false; // В конце URL не должно быть слеша
        }

        return true; // Если все проверки пройдены успешно
    }

}
