package searchengine.services;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import searchengine.entity.Lemma;
import searchengine.entity.Site;
import searchengine.repository.LemmaRepository;
import searchengine.repository.SiteRepository;
@Service
public class LemmaService {

    private final LemmaRepository lemmaRepository;
    private final SiteRepository siteRepository;

    @Autowired
    public LemmaService(LemmaRepository lemmaRepository, SiteRepository siteRepository) {
        this.lemmaRepository = lemmaRepository;
        this.siteRepository = siteRepository;
    }

    // Метод для сохранения леммы с проверкой наличия сайта
    public Lemma saveLemma(String lemmaText, int siteId) {
        // Проверка: существует ли сайт с данным ID
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new IllegalArgumentException("Site with ID " + siteId + " does not exist"));

        // Создание и сохранение леммы
        Lemma lemma = new Lemma(site, lemmaText);
        return lemmaRepository.save(lemma);
    }
}
