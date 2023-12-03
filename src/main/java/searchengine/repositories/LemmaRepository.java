package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Lemma;
import searchengine.model.Website;

import java.util.ArrayList;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {

    @Transactional
    Lemma findByLemma(String lemma);

    @Transactional
    void deleteByWebsite(Website website);
}
