package searchengine.services;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import searchengine.entity.Page;
import searchengine.repository.PageRepository;

@Service
public class PageDataService {

    private static final Logger logger = LoggerFactory.getLogger(PageDataService.class);

    @Autowired
    private PageRepository pageRepository;
    
    public Page savePage(Page page) {
        logger.info("Сохранение страницы с URL: {}", page.getPath());
        try {
            Page savedPage = pageRepository.saveAndFlush(page);
            logger.info("Страница сохранена с ID: {}", savedPage.getId());
            return savedPage;
        } catch (Exception e) {
            logger.error("Ошибка сохранения страницы {}: {}", page.getPath(), e.getMessage());
            throw e;
        }
    }
    
    public Optional<Page> findPageByPathAndSiteId(String path, Integer siteId) {
        logger.info("Поиск страницы с путем: {} и Site ID: {}", path, siteId);
        Optional<Page> page = pageRepository.findByPathAndSiteId(path, siteId);
        if (page.isPresent()) {
            logger.info("Найдена страница с ID: {}", page.get().getId());
        } else {
            logger.warn("Страница не найдена для пути: {} и Site ID: {}", path, siteId);
        }
        return page;
    }
}
