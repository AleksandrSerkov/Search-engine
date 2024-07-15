package searchengine.services;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import searchengine.entity.Site;
import searchengine.repository.SiteRepository;
@Service

public class SiteService {

    private final SiteRepository siteRepository;

    @Autowired
    public SiteService(SiteRepository siteRepository) {
        this.siteRepository = siteRepository;
    }

    // Сохранить объект currentSite в базе данных
    public void saveSite(Site currentSite) {
        siteRepository.save(currentSite);
    }

    // Получить все сайты из базы данных
    public List<Site> getAllSites() {
        return siteRepository.findAll();
    }
}