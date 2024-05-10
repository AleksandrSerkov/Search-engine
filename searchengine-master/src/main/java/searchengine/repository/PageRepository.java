package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;
import searchengine.entity.Page;

@Repository
public interface PageRepository extends JpaRepository<Page, Long> {

}
