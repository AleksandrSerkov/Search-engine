package searchengine.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.entity.Lemma;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Long> {
    Lemma findByLemmaText(String lemmaText);
}
