package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import searchengine.entity.Site;
import searchengine.model.Status;

@Repository
public interface SiteRepository extends JpaRepository<Site, Integer> {

    // Удаляем метод поиска по URL, поскольку для уникальной идентификации используем findById
    // Optional<Site> findByUrl(String url);

    // Проверка наличия сайта с заданным статусом
    boolean existsByUrlAndStatus(String url, Status status);

    // Проверка наличия хотя бы одного сайта с указанным статусом
    boolean existsByStatus(Status status);
}




