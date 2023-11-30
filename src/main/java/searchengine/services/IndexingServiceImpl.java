package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.indexing.IndexingResponseFalse;
import searchengine.dto.indexing.IndexingResponseTrue;
import searchengine.model.IndexingStatus;
import searchengine.model.Page;
import searchengine.model.Website;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SearchIndexRepository;
import searchengine.repositories.WebsiteRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
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
            finishIndexing(FAILED, "Индексация остановлена пользователем");
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
            Website website = new Website(INDEXING, LocalDateTime.now(), site.getUrl(), site.getName());
            Website websiteToDelete = websiteRepository.findWebsiteByUrl(website.getUrl());
            if(websiteToDelete != null ) {
                int websiteIdToDelete = websiteToDelete.getId();
                pageRepository.deleteByWebsite(websiteToDelete);
                websiteRepository.deleteById(websiteIdToDelete);
            }
            websiteRepository.save(website);
            ForkJoinPool forkJoinPool = new ForkJoinPool();
            forkJoinPoolList.add(forkJoinPool);
            RecursiveSearch recursiveSearch = new RecursiveSearch(website, site.getUrl());
            forkJoinPool.invoke(recursiveSearch);
        }
        finishIndexing(INDEXED, "");
    }

    /*public void finishIndexing(IndexingStatus status, String lastError) {
        while (isIndexingStarted) {
            long pageCount = pageRepository.count();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {}

            if (pageCount == pageRepository.count()) {
                isIndexingStarted = false;
            }
        }
        sites.getSites().forEach(site -> {
            Website website = websiteRepository.findWebsiteByUrl(site.getUrl());
            website.setStatusTime(LocalDateTime.now());
            website.setStatus(status);
            website.setLastError(lastError);
            websiteRepository.save(website);
        });
    }
     */

    synchronized public static ArrayList<Page> getPageList() {
        ArrayList<Page> pageListClone = new ArrayList<>();
        pageListClone.addAll(pageList);
        pageList.clear();
        return pageListClone;
    }

    synchronized static void writeInPageList(Page page) {
        pageList.add(page);
    }
    public void finishIndexing(IndexingStatus status, String lastError) {
        while (isIndexingStarted) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {}
            ArrayList<Page> pageListToAddToDB = getPageList();
            if (pageListToAddToDB.isEmpty()) {
                isIndexingStarted = false;
                sites.getSites().forEach(site -> {
                    Website website = websiteRepository.findWebsiteByUrl(site.getUrl());
                    website.setStatusTime(LocalDateTime.now());
                    website.setStatus(status);
                    website.setLastError(lastError);
                    websiteRepository.save(website);
                });
            }
            pageListToAddToDB.forEach(page -> {
                if (!isPageInDB(page)) {
                    pageRepository.save(page);
                }
            });
        }
    }

    public boolean isPageInDB(Page page) {
        ArrayList<Page> pagesInDB = pageRepository.findAllPageByPath(page.getPath());
        ArrayList<Integer> websitesId = new ArrayList<>();
        pagesInDB.forEach(pageInDB-> websitesId.add(pageInDB.getWebsite().getId()));
        return !pagesInDB.isEmpty() && websitesId.contains(page.getWebsite().getId());
    }
}
