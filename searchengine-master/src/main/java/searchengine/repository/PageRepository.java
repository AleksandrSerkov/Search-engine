package searchengine.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import searchengine.dto.statistics.SearchResultDTO;
import searchengine.entity.Page;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {

    

    // Поиск страниц по леммам и сайту (оптимизированный запрос с JOIN)
    @Query("SELECT p FROM Page p " +
           "JOIN Index i ON p.id = i.pageId " +
           "JOIN Lemma l ON i.lemmaId = l.id " +
           "WHERE p.site.id = :siteId AND l.lemmaText IN :lemmas")
    List<Page> findPagesByLemmasAndSite(@Param("lemmas") List<String> lemmas, @Param("siteId") int siteId);

    // Поиск страниц по леммам (без фильтра по сайту)
    @Query("SELECT p FROM Page p " +
           "JOIN Index i ON p.id = i.pageId " +
           "JOIN Lemma l ON i.lemmaId = l.id " +
           "WHERE l.lemmaText IN :lemmas")
    List<Page> findPagesByLemmas(@Param("lemmas") List<String> lemmas);

    // Новый метод: поиск с возвратом DTO (по сайту)
    @Query("SELECT new searchengine.dto.statistics.SearchResultDTO(p.path, p.title, '', 0.0, p.site.name) " +
           "FROM Page p " +
           "JOIN Index i ON p.id = i.pageId " +
           "JOIN Lemma l ON i.lemmaId = l.id " +
           "WHERE p.site.id = :siteId AND l.lemmaText IN :lemmas")
    List<SearchResultDTO> findSearchResultDTOsByLemmasAndSite(@Param("lemmas") List<String> lemmas, @Param("siteId") int siteId);

    // Новый метод: поиск с возвратом DTO (без фильтра по сайту)
    @Query("SELECT new searchengine.dto.statistics.SearchResultDTO(p.path, p.title, '', 0.0, p.site.name) " +
           "FROM Page p " +
           "JOIN Index i ON p.id = i.pageId " +
           "JOIN Lemma l ON i.lemmaId = l.id " +
           "WHERE l.lemmaText IN :lemmas")
    List<SearchResultDTO> findSearchResultDTOsByLemmas(@Param("lemmas") List<String> lemmas);
    
    // Метод для поиска страницы по пути и ID сайта
    Optional<Page> findByPathAndSiteId(String path, Integer siteId);
}






