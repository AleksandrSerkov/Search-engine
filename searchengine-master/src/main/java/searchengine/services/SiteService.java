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
