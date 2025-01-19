package searchengine.repository;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import searchengine.entity.Lemma;
@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    // Поиск леммы по тексту
   

    Lemma findByLemmaText(String lemmaText);  
    List<Lemma> findByLemmaTextInAndSiteId(List<String> lemmas, int siteId);  
}
