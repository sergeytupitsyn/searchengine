package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SearchIndex;

@Repository
public interface SearchIndexRepository extends JpaRepository<SearchIndex, Integer> {

    @Transactional
    void deleteAllByPage(Page page);

    @Transactional
    void deleteAllByLemma(Lemma lemma);
}
