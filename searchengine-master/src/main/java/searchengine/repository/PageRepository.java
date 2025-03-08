package searchengine.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import searchengine.entity.Page;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {
    
    @Query(value = "SELECT i.level FROM idx i " +
                   "WHERE i.page_id = :id AND i.lemma = :lemma", nativeQuery = true)
    double findLemmaRank(@Param("id") Integer id, @Param("lemma") String lemma);

    // Поиск страниц по леммам и сайту (без связей, используя ID)
    @Query("SELECT p FROM Page p " +
           "WHERE p.site.id = :siteId AND p.id IN " +
           "(SELECT i.pageId FROM Index i WHERE i.lemma IN :lemmas)")
    List<Page> findPagesByLemmasAndSite(@Param("lemmas") List<String> lemmas, @Param("siteId") int siteId);

    // Поиск страниц по леммам (без связей, используя ID)
    @Query("SELECT p FROM Page p " +
           "WHERE p.id IN (SELECT i.pageId FROM Index i WHERE i.lemma IN :lemmas)")
    List<Page> findPagesByLemmas(@Param("lemmas") List<String> lemmas);
}




