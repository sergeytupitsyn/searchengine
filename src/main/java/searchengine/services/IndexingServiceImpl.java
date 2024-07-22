package searchengine.services;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import searchengine.Application;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.indexing.IndexingResponseFalse;
import searchengine.dto.indexing.IndexingResponseTrue;
import searchengine.model.*;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SearchIndexRepository;
import searchengine.repositories.WebsiteRepository;
import static searchengine.model.IndexingStatus.*;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final PageRepository pageRepository;
    private final WebsiteRepository websiteRepository;
    private final LemmaRepository lemmaRepository;
    private final SearchIndexRepository searchIndexRepository;
    private final SitesList sites;
    static boolean isIndexingStarted = false;
    Map<ForkJoinPool, Site> forkJoinPoolList = new HashMap<>();
    static List<String> parsedLinksList = new ArrayList<>();

    @Override
    public IndexingResponse getStartResponse() {
        if (!isIndexingStarted) {
            Executor executor = Executors.newSingleThreadExecutor();
            executor.execute(this::startIndexing);
            return new IndexingResponseTrue();
        } else {
            return new IndexingResponseFalse("Индексация уже запущена");
        }
    }

    @Override
    public IndexingResponse getStopResponse() {
        forkJoinPoolList.keySet().forEach(ForkJoinPool::shutdownNow);
        if (isIndexingStarted) {
            isIndexingStarted = false;
            try {
                Thread.sleep(60000);
            } catch (InterruptedException e) {
                Logger logger = LoggerFactory.getLogger(Application.class);
                logger.error(e.getMessage());
                Thread.currentThread().interrupt();
            }
            sites.getSites().forEach(site -> {
                try {
                    finishIndexing(site, FAILED,
                            "Индексация остановлена пользователем");
                } catch (SQLException e) {
                    Logger logger = LoggerFactory.getLogger(Application.class);
                    logger.error(e.getSQLState());
                }
            });
            return new IndexingResponseTrue();
        } else {
            return new IndexingResponseFalse("Индексация не запущена");
        }
    }

    public void startIndexing() {
        isIndexingStarted = true;
        parsedLinksList.clear();
        for (Site site : sites.getSites()) {
            Website oldWebsite = websiteRepository.findWebsiteByUrl(site.getUrl());
            if(oldWebsite != null ) {
                try {
                    removeSiteDataFromBD(oldWebsite);
                } catch (SQLException e) {
                    Logger logger = LoggerFactory.getLogger(Application.class);
                    logger.error(e.getSQLState());
                }
            }
            Website website = new Website(INDEXING, LocalDateTime.now(), "", site.getUrl(), site.getName());
            websiteRepository.save(website);
            ForkJoinPool forkJoinPool = new ForkJoinPool();
            forkJoinPoolList.put(forkJoinPool, site);
            RecursiveSearch recursiveSearch = new RecursiveSearch(pageRepository, websiteRepository,
                    lemmaRepository, searchIndexRepository, website, site.getUrl());
            forkJoinPool.invoke(recursiveSearch);
        }
        try {
            waitingForIndexingToFinish();
        } catch (SQLException e) {
            Logger logger = LoggerFactory.getLogger(Application.class);
            logger.error(e.getSQLState());
        }

    }

    public void waitingForIndexingToFinish() throws SQLException {
        while (isIndexingStarted) {
            try {
                Thread.sleep(60000);
            } catch (InterruptedException e) {
                Logger logger = LoggerFactory.getLogger(Application.class);
                logger.error(e.getMessage());
                Thread.currentThread().interrupt();
            }
            if (!isIndexingStarted) {
                break;
            }
            boolean allSitesAreIndexed = true;
            for (ForkJoinPool forkJoinPool : forkJoinPoolList.keySet()) {

                if (forkJoinPool.getPoolSize() == 0) {
                    finishIndexing(forkJoinPoolList.get(forkJoinPool), INDEXED, "");
                } else {
                    allSitesAreIndexed = false;
                }
            }
            if (allSitesAreIndexed) {
                isIndexingStarted = false;
            }
        }
    }

    @Override
    public IndexingResponse getIndexPageResponse(String url) {
        for (Site site : sites.getSites()) {
            if (url.startsWith(site.getUrl())) {
                try {
                    indexingPage(url, site);
                } catch (SQLException e) {
                    Logger logger = LoggerFactory.getLogger(Application.class);
                    logger.error(e.getSQLState());
                }
                return new IndexingResponseTrue();
            }
        }
        return new IndexingResponseFalse("Данная страница находится за пределами сайтов, " +
                "указанных в конфигурационном файле");
    }

    public void removeSiteDataFromBD(Website website) throws SQLException{
        int id = website.getId();
        List<Page> pages = pageRepository.findAllPageByWebsite(website);
        List<Lemma> lemmas = lemmaRepository.findAllByWebsite(website);
        pages.forEach(searchIndexRepository::deleteAllByPage);
        lemmas.forEach(searchIndexRepository::deleteAllByLemma);
        lemmaRepository.deleteByWebsite(website);
        pageRepository.deleteByWebsite(website);
        websiteRepository.deleteById(id);
    }

    public void finishIndexing(Site site, IndexingStatus status, String lastError) throws SQLException{
        Website website = websiteRepository.findWebsiteByUrl(site.getUrl());
        website.setStatusTime(LocalDateTime.now());
        website.setStatus(status);
        website.setLastError(lastError);
        websiteRepository.save(website);
    }

    public void indexingPage(String url, Site site) throws SQLException{
        isIndexingStarted = true;
        Website website= websiteRepository.findWebsiteByUrl(site.getUrl());
        Page newlyIndexedPage = pageRepository.findPageByPathAndWebsite(url.substring(
                website.getUrl().length() - 1), website);
        removePageDataFromBD(newlyIndexedPage);
        RecursiveSearch recursiveSearch= new RecursiveSearch(pageRepository, websiteRepository, lemmaRepository, searchIndexRepository, website, site.getUrl());
        recursiveSearch.pageParser(url);
        waitingForIndexingToFinish();
    }

    public void removePageDataFromBD(Page page) throws SQLException {
        searchIndexRepository.deleteAllByPage(page);
        Map<String, Integer> lemmas = new LemmaSearch().splitToLemmas(page.getContent());
        for (String lemmaStr : lemmas.keySet()) {
            Lemma lemmaToDeleted = lemmaRepository.findByLemmaAndWebsite(lemmaStr, page.getWebsite());
            lemmaToDeleted.setFrequency(lemmaToDeleted.getFrequency() - 1);
            if (lemmaToDeleted.getFrequency() == 0) {
                lemmaRepository.delete(lemmaToDeleted);
                continue;
            }
            lemmaRepository.save(lemmaToDeleted);
        }
        pageRepository.delete(page);
    }
}
