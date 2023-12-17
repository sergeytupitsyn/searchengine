package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Page;
import searchengine.model.Website;

import java.util.ArrayList;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {

    @Transactional
    void deleteByWebsite(Website website);

    @Transactional
    ArrayList<Page> findAllPageByPath(String path);

    @Transactional
    Page findPageByPath(String path);

    @Transactional
    ArrayList<Page> findAllPageByWebsite(Website website);

    @Transactional
    Page findPageByPathAndWebsite(String path, Website website);

    @Transactional
    long count();
}
