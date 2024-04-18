package searchengine.services;
import jakarta.persistence.EntityNotFoundException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PageService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    @Autowired
    public PageService(PageRepository pageRepository, LemmaRepository lemmaRepository, IndexRepository indexRepository, SiteRepository siteRepository) {
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.siteRepository = siteRepository;
    }

    private void checkCertificate(String url) {
        try {
            Document doc = Jsoup.connect(url).get();
            // Дополнительные действия, если не нужно обрабатывать содержимое страницы
        } catch (Exception e) {
            e.printStackTrace(); // Обработка ошибки сертификата
        }
    }

    // Сохранить объект page в базе данных
    public void savePage(Page page) {
        pageRepository.save(page);
    }

    // Получить все пейджи из базы данных
    public List<Page> getAllPages() {
        return pageRepository.findAll();
    }

    // Индексировать страницу по URL
    public void indexPage(String url) throws IOException {
        IndexingService indexingService = new IndexingService(siteRepository, pageRepository, lemmaRepository, indexRepository);

        // Проверяем сертификат перед загрузкой страницы
        checkCertificate(url);

        // Получение HTML-кода веб-страницы
        Document doc = Jsoup.connect(url).get();
        String htmlContent = doc.html();

        // Сохраняем HTML-код в таблицу page
        Page page = new Page();
        page.setPath(url);
        page.setContent(htmlContent);
        pageRepository.save(page);

        // Преобразуем HTML-код в набор лемм
        String text = indexingService.cleanHtmlTags(htmlContent);
        String[] words = text.split("\\W+");

        // Создаем связки леммы и страницы в таблице index
        Map<String, Integer> lemmaCount = new HashMap<>();
        for (String word : words) {
            String lemma = word.toLowerCase();
            lemmaCount.put(lemma, lemmaCount.getOrDefault(lemma, 0) + 1);
        }

        for (Map.Entry<String, Integer> entry : lemmaCount.entrySet()) {
            String lemmaText = entry.getKey();
            int rank = entry.getValue();

            Lemma lemmaEntity = lemmaRepository.findByLemmaText(lemmaText);
            if (lemmaEntity == null) {
                Site site = siteRepository.findById(1L).orElseThrow(EntityNotFoundException::new);
                lemmaEntity = new Lemma();
                lemmaEntity.setLemmaText(lemmaText);
                lemmaEntity.setSite(site);
                lemmaEntity.setFrequency(1); // Устанавливаем начальное значение частоты
            } else {
                lemmaEntity.setFrequency(lemmaEntity.getFrequency() + 1); // Увеличиваем частоту леммы
            }
            lemmaRepository.save(lemmaEntity);

            // Создаем связки леммы и страницы в таблице index
            Index indexEntity = new Index();
            indexEntity.setLemmaId(lemmaEntity.getId());
            indexEntity.setPageId(page.getId());
            indexEntity.setLemma(lemmaText);
            indexEntity.setRank((float) rank);
            indexRepository.save(indexEntity);
        }
    }
}