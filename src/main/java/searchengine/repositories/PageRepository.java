package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Page;
import searchengine.model.Website;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {

    @Transactional
    void deleteByWebsite(Website website);

    @Transactional
    Page findPageByPath(String path);
}
