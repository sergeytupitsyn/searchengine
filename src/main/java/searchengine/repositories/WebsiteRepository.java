package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Website;

@Repository
public interface WebsiteRepository extends JpaRepository<Website, Integer> {
}
