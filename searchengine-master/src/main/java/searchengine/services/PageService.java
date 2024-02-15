package searchengine.services;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.Page;
import searchengine.model.PageRepository;

import java.util.List;

@Service
public class PageService {

    private final PageRepository pageRepository;

    @Autowired
    public PageService(PageRepository pageRepository) {
        this.pageRepository = pageRepository;
    }

    // Сохранить объект page в базе данных
    public void savePage(Page page) {
        pageRepository.save(page);
    }

    // Получить все пейджи из базы данных
    public List<Page> getAllPages() {
        return pageRepository.findAll();
    }
}
