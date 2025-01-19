package searchengine.services;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import searchengine.entity.Site;
import searchengine.repository.SiteRepository;

@Service
public class SiteService {

    private final SiteRepository siteRepository;
    private final searchengine.config.SitesList siteConfig;

    @Autowired
    public SiteService(SiteRepository siteRepository, searchengine.config.SitesList siteConfig) {
        this.siteRepository = siteRepository;
        this.siteConfig = siteConfig;
    }
    // Проверить, проиндексирован ли сайт
public boolean isSiteIndexed(String siteUrl) {
    if (siteUrl == null || siteUrl.isEmpty()) {
        return siteRepository.existsByStatus(searchengine.model.Status.INDEXED);
    }
    return siteRepository.existsByUrlAndStatus(siteUrl, searchengine.model.Status.INDEXED);
}

// Найти сайт по URL
public Site findSiteByUrl(String siteUrl) {
    return siteRepository.findByUrl(siteUrl)
            .orElseThrow(() -> new IllegalArgumentException("Сайт с URL " + siteUrl + " не найден"));
}

// Удалить сайт из базы данных
public void deleteSite(Site site) {
    siteRepository.delete(site);
}

// Очистить все сайты из базы данных
public void clearAllSites() {
    siteRepository.deleteAll();
}


    // Сохранить объект currentSite в базе данных
    public void saveSite(Site currentSite) {
        siteRepository.save(currentSite);
    }

    // Получить все сайты из базы данных
    public List<Site> getAllSites() {
        return siteRepository.findAll();
    }

    // Загрузить сайты из YAML и сохранить в базе данных
    public void loadSitesFromYaml(List<Site> sitesFromYaml) {
        for (Site site : sitesFromYaml) {
            saveSite(site);
        }
        
    }
}
