package searchengine.repository;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import searchengine.entity.Site;
import searchengine.model.Status;

@Repository
public interface SiteRepository extends JpaRepository<Site, Integer> {
    Optional<Site> findByUrl(String url);
    List<Site> findAllByUrl(String url);

    // Проверка наличия сайта с заданным статусом
    boolean existsByUrlAndStatus(String url, Status status);

    // Проверка наличия хотя бы одного сайта с указанным статусом
    boolean existsByStatus(Status status);
}




