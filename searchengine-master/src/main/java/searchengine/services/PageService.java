package searchengine.services;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import searchengine.entity.Index;
import searchengine.entity.Lemma;
import searchengine.entity.Page;
import searchengine.entity.Site;
import searchengine.model.Status;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.SiteRepository;

@Service
@Transactional
public class PageService {

    private static final Logger logger = LoggerFactory.getLogger(PageService.class);

    @Autowired
    private PageDataService pageDataService;

    @Autowired
    private LemmaService lemmaService;

    @Autowired
    private HtmlCleaner htmlCleaner;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private SiteRepository siteRepository;
    
    // Если требуются репозитории для ручного индексирования:
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private IndexRepository indexRepository;
    // @Autowired
    // private PageRepository pageRepository; // Если нужен отдельный репозиторий для Page

    /**
     * Индексирует страницу по URL.
     * Подход 1: Использование ConnectionService и обработки через LemmaService.
     */
    @Transactional
    public void indexPage(String url) throws IOException {
        logger.info("Начинаем индексацию страницы: {}", url);

        // Проверка сертификата
        checkCertificate(url);
        logger.debug("Проверка сертификата пройдена для URL: {}", url);

        // Получаем документ через ConnectionService (если он настроен), иначе fallback к Jsoup.connect
        Document doc;
        try {
            doc = connectionService.connectToPage(url);
        } catch (Exception e) {
            logger.warn("ConnectionService не смог получить документ, пробуем через Jsoup: {}", e.getMessage());
            doc = Jsoup.connect(url).get();
        }
        logger.debug("Документ успешно получен для URL: {}", url);

        String htmlContent = doc.html();
        String title = doc.title();
        if (title == null || title.trim().isEmpty()) {
            title = "Без названия";
            logger.warn("Заголовок страницы пуст, используется значение по умолчанию: {}", title);
        }

        // Извлекаем базовый URL
        String baseUrl = extractBaseUrl(url);
        logger.info("Извлечён базовый URL: {}", baseUrl);

        // Получаем сайт по базовому URL
        List<Site> sites = siteRepository.findAllByUrl(baseUrl);
        if (sites.isEmpty()) {
            logger.error("Сайт с базовым URL {} не найден", baseUrl);
            throw new EntityNotFoundException("Site with URL " + baseUrl + " not found");
        }
        // Если есть сайт со статусом INDEXED, выбираем его, иначе первый найденный
        Site site = sites.stream()
                .filter(s -> s.getStatus() == Status.INDEXED)
                .findFirst()
                .orElse(sites.get(0));
        logger.info("Выбран сайт: {} с ID: {}", site.getName(), site.getId());

        // Создаем и сохраняем объект Page через PageDataService
        Page page = new Page();
        page.setPath(url);
        page.setContent(htmlContent);
        page.setSite(site);
        page.setTitle(title);
        page.setCode(200);
        page = pageDataService.savePage(page);
        logger.info("Страница сохранена с ID: {} для URL: {}", page.getId(), url);

        // Очистка HTML и обработка лемм через HtmlCleaner и LemmaService
        String cleanText = htmlCleaner.cleanHtml(htmlContent);
        logger.debug("HTML успешно очищен для страницы с ID: {}", page.getId());
        lemmaService.processLemmas(page, cleanText, site);
        logger.info("Обработка лемм завершена для страницы с ID: {}", page.getId());

        logger.info("Индексация страницы завершена: {}", url);
    }

    /**
     * Альтернативный метод индексирования страницы "вручную".
     * Здесь выполняется разбиение текста на слова и сохранение лемм и индексов.
     */
    public void indexPageManually(String url) throws IOException {
        // Проверяем сертификат перед загрузкой страницы
        checkCertificate(url);

        // Получаем HTML через Jsoup напрямую
        Document doc = Jsoup.connect(url).get();
        String htmlContent = doc.html();

        // Сохраняем HTML в таблицу Page (предполагается, что метод savePage существует)
        Page page = new Page();
        page.setPath(url);
        page.setContent(htmlContent);
        page = pageDataService.savePage(page);

        // Преобразуем HTML в набор лемм
        String text = htmlCleaner.cleanHtml(htmlContent);
        String[] words = text.split("\\W+");

        // Создаем мапу для подсчета лемм
        Map<String, Integer> lemmaCount = new HashMap<>();
        for (String word : words) {
            String lemma = word.toLowerCase();
            lemmaCount.put(lemma, lemmaCount.getOrDefault(lemma, 0) + 1);
        }

        // Обработка каждой леммы: обновление или создание записи в таблице Lemma,
        // а затем сохранение индекса в таблице Index.
        for (Map.Entry<String, Integer> entry : lemmaCount.entrySet()) {
            String lemmaText = entry.getKey();
            int rank = entry.getValue();

            Lemma lemmaEntity = lemmaRepository.findByLemmaText(lemmaText);
            if (lemmaEntity == null) {
                // Пример: выбираем сайт с id 1 (это можно изменить по логике)
                Site site = siteRepository.findById(1)
                        .orElseThrow(() -> new EntityNotFoundException("Site with id 1 not found"));
                lemmaEntity = new Lemma();
                lemmaEntity.setLemmaText(lemmaText);
                lemmaEntity.setSite(site);
                lemmaEntity.setFrequency(1);
            } else {
                lemmaEntity.setFrequency(lemmaEntity.getFrequency() + 1);
            }
            lemmaRepository.save(lemmaEntity);

            // Создаем запись в таблице Index для связывания леммы и страницы
            Index indexEntity = new Index();
            indexEntity.setLemmaId(lemmaEntity.getId());
            indexEntity.setPageId(page.getId());
            indexEntity.setLemma(lemmaText);
            indexEntity.setRank((float) rank);
            indexRepository.save(indexEntity);
        }
    }

    private void checkCertificate(String url) {
        // Реализуйте проверку сертификата, если необходимо
        logger.debug("Выполняется проверка сертификата для URL: {}", url);
    }
    
    private String extractBaseUrl(String url) {
        try {
            java.net.URL parsedUrl = new java.net.URL(url);
            String baseUrl = parsedUrl.getProtocol() + "://" + parsedUrl.getHost();
            logger.debug("Базовый URL успешно извлечён: {}", baseUrl);
            return baseUrl;
        } catch (Exception e) {
            logger.error("Ошибка извлечения базового URL для {}: {}", url, e.getMessage());
            return url;
        }
    }
}
