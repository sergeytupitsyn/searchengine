package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.SearchIndex;

@Repository
public interface SearchIndexRepository extends JpaRepository<SearchIndex, Integer> {
}
