package searchengine.services;
import java.util.Date;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import searchengine.entity.Index;
import searchengine.entity.Lemma;
import searchengine.entity.Page;
import searchengine.entity.Site;
import searchengine.model.Status;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
@Service
public class DatabaseService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final TransactionTemplate transactionTemplate;

    @Autowired
    public DatabaseService(SiteRepository siteRepository, 
                           PageRepository pageRepository,
                           LemmaRepository lemmaRepository, 
                           IndexRepository indexRepository,
                           PlatformTransactionManager transactionManager) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public void saveOrUpdateSite(Site site) {
        transactionTemplate.execute(status -> {
            Optional<Site> existingSite = siteRepository.findByUrl(site.getUrl());
            if (existingSite.isEmpty()) {
                site.setStatus(Status.INDEXING);
                site.setStatusTime(new Date());
                site.setLastError("");
                siteRepository.save(site);
            } else {
                Site existing = existingSite.get();
                existing.setStatus(Status.INDEXING);
                existing.setStatusTime(new Date());
                existing.setLastError("");
                siteRepository.save(existing);
            }
            return null;
        });
    }

    public void savePage(Page page) {
        transactionTemplate.execute(status -> {
            pageRepository.save(page);
            return null;
        });
    }

    public void saveLemmaAndIndex(Lemma lemma, Index index) {
        transactionTemplate.execute(status -> {
            lemmaRepository.save(lemma);
            indexRepository.save(index);
            return null;
        });
    }

    public void updateSiteStatus(Site site, Status status) {
        transactionTemplate.execute(statusTransaction -> {
            site.setStatus(status);
            site.setStatusTime(new Date());
            siteRepository.save(site);
            return null;
        });
    }
}
