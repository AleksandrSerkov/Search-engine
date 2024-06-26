package searchengine.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.entity.Index;

@Repository
public interface IndexRepository extends JpaRepository<Index, Long> {
    // Дополнительные методы для работы с таблицей Index могут быть добавлены здесь
}
