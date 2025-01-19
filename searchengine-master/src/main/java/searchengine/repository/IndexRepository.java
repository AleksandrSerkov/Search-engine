package searchengine.repository;
import org.springframework.data.jpa.repository.JpaRepository;

import searchengine.entity.Index;

public interface IndexRepository extends JpaRepository<Index, Long> {
    Index findByPageIdAndLemma(Integer pageId, String lemma);
}



