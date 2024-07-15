package searchengine.repository;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import searchengine.entity.Site;

@Repository
public interface SiteRepository extends JpaRepository<Site, Integer> {

    @Override
    List<Site> findAll();

}



