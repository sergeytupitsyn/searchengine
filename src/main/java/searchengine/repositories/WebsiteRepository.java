package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Website;

@Repository
public interface WebsiteRepository extends JpaRepository<Website, Integer> {

    @Transactional
    Website findWebsiteByUrl(String url);
}
