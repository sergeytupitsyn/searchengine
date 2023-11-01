package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.StartIndexingResponse;
import searchengine.dto.indexing.StartIndexingResponseFalse;
import searchengine.dto.indexing.StartIndexingResponseTrue;
import searchengine.model.Website;
import searchengine.repositories.PageRepository;
import searchengine.repositories.WebsiteRepository;

import java.time.LocalDateTime;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

import static searchengine.model.IndexingStatus.INDEXED;
import static searchengine.model.IndexingStatus.INDEXING;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final PageRepository pageRepository;
    private final WebsiteRepository websiteRepository;
    private final SitesList sites;
    private boolean isIndexingStarted = false;

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
            RecursiveSearch recursiveSearch = new RecursiveSearch(website, site.getUrl(), pageRepository, websiteRepository);
            forkJoinPool.invoke(recursiveSearch);
        }
        finishIndexing();
    }

    @Override
    public StartIndexingResponse getResponse() {
        if (!isIndexingStarted) {
            Executor executor = Executors.newSingleThreadExecutor();
            executor.execute(this::startIndexing);
            return new StartIndexingResponseTrue();
        } else {
            return new StartIndexingResponseFalse();
        }
    }

    public void finishIndexing() {
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
            website.setStatus(INDEXED);
            websiteRepository.save(website);
        });
        System.out.println("индексация завершена");
    }
}
