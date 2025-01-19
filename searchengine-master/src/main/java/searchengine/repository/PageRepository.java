package searchengine.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import searchengine.entity.Page;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {
    @Query(value = "SELECT p.lemma_rank FROM page p WHERE p.id = :id AND p.lemma = :lemma", nativeQuery = true)
    double findLemmaRank(@Param("id") Integer id, @Param("lemma") String lemma);

    

    // Поиск страниц по леммам и сайту
    @Query("SELECT p FROM Page p JOIN p.lemmas l WHERE p.site.id = :siteId AND l.lemmaText IN :lemmas")
List<Page> findPagesByLemmasAndSite(@Param("lemmas") List<String> lemmas, @Param("siteId") int siteId);



    // Поиск страниц по леммам
    @Query("SELECT p FROM Page p JOIN p.lemmas l WHERE l.lemmaText IN :lemmas")
List<Page> findPagesByLemmas(@Param("lemmas") List<String> lemmas);
}


