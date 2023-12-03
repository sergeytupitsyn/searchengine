package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

import static searchengine.model.IndexingStatus.*;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final PageRepository pageRepository;
    private final WebsiteRepository websiteRepository;
    private final LemmaRepository lemmaRepository;
    private final SearchIndexRepository searchIndexRepository;
    private final SitesList sites;
    private boolean isIndexingStarted = false;
    ArrayList<ForkJoinPool> forkJoinPoolList = new ArrayList<>();
    static ArrayList<Page> pageList = new ArrayList<>();

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
        forkJoinPoolList.forEach(ForkJoinPool::shutdown);
        if (isIndexingStarted) {
            sites.getSites().forEach(site -> finishIndexing(site, FAILED, "Индексация остановлена пользователем"));
            return new IndexingResponseTrue();
        } else {
            return new IndexingResponseFalse("Индексация не запущена");
        }
    }

    @Override
    public IndexingResponse getIndexPageResponse(String url) {
        System.out.println(url);
        return new IndexingResponseTrue();
    }

    public void startIndexing() {
        isIndexingStarted = true;
        for (Site site : sites.getSites()) {
            Website oldWebsite = websiteRepository.findWebsiteByUrl(site.getUrl());
            if(oldWebsite != null ) {
                removeSiteDataFromBD(oldWebsite);
            }
            Website website = new Website(INDEXING, LocalDateTime.now(), site.getUrl(), site.getName());
            websiteRepository.save(website);
            ForkJoinPool forkJoinPool = new ForkJoinPool();
            forkJoinPoolList.add(forkJoinPool);
            RecursiveSearch recursiveSearch = new RecursiveSearch(website, site.getUrl());
            forkJoinPool.invoke(recursiveSearch);
        }
        saveIndexingDataInDB(INDEXED, "");
    }

    synchronized public static ArrayList<Page> getPageList() {
        ArrayList<Page> pageListClone = new ArrayList<>(pageList);
        pageList.clear();
        return pageListClone;
    }

    synchronized static void writeInPageList(Page page) {
        pageList.add(page);
    }

    public void saveIndexingDataInDB(IndexingStatus status, String lastError) {
        while (isIndexingStarted) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException ignored) {}
            ArrayList<Page> pageListToAddToDB = getPageList();
            if (pageListToAddToDB.isEmpty()) {
                isIndexingStarted = false;
                sites.getSites().forEach(site -> finishIndexing(site, status, lastError));
                continue;
            }
            for (Page page : pageListToAddToDB) {
                if (isPageInDB(page)) {
                    continue;
                }
                pageRepository.save(page);
                saveLemmaInDB(page);
                Website website = page.getWebsite();
                website.setStatusTime(LocalDateTime.now());
                websiteRepository.save(website);
            }
        }
    }

    public boolean isPageInDB(Page page) {
        ArrayList<Page> pagesInDB = pageRepository.findAllPageByPath(page.getPath());
        ArrayList<Integer> websitesId = new ArrayList<>();
        pagesInDB.forEach(pageInDB-> websitesId.add(pageInDB.getWebsite().getId()));
        return !pagesInDB.isEmpty() && websitesId.contains(page.getWebsite().getId());
    }

    public boolean isLemmaInDB(String lemmaString) {
        Lemma lemma = lemmaRepository.findByLemma(lemmaString);
        return lemma != null;
    }

    public void saveLemmaInDB(Page page) {
        HashMap<String, Integer> lemmas = null;
        try {
            lemmas = new LemmaSearch().splitToLemmas(page.getContent());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (String lemmaString : lemmas.keySet()) {
            Lemma lemma;
            if (!isLemmaInDB(lemmaString)) {
                lemma = new Lemma(page.getWebsite(), lemmaString, 1);
            } else {
                lemma = lemmaRepository.findByLemma(lemmaString);
                lemma.setFrequency(lemma.getFrequency() + 1);
            }
            lemmaRepository.save(lemma);
            searchIndexRepository.save(new SearchIndex(page, lemma, lemmas.get(lemmaString)));
        }
    }

    public void removeSiteDataFromBD(Website website) {
        int id = website.getId();
        ArrayList<Page> pages = pageRepository.findAllPageByWebsite(website);
        pages.forEach(searchIndexRepository::deleteByPage);
        lemmaRepository.deleteByWebsite(website);
        pageRepository.deleteByWebsite(website);
        websiteRepository.deleteById(id);
    }

    public void finishIndexing(Site site, IndexingStatus status, String lastError) {
        Website website = websiteRepository.findWebsiteByUrl(site.getUrl());
        website.setStatusTime(LocalDateTime.now());
        website.setStatus(status);
        website.setLastError(lastError);
        websiteRepository.save(website);
    }
}
