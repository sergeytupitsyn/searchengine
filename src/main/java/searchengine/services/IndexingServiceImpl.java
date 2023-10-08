package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.StartIndexingResponse;
import searchengine.model.Page;
import searchengine.model.Website;
import searchengine.repositories.PageRepository;
import searchengine.repositories.WebsiteRepository;

import java.awt.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ForkJoinPool;

import static searchengine.model.IndexingStatus.INDEXING;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final PageRepository pageRepository;
    private final WebsiteRepository websiteRepository;
    private final SitesList sites;

    public void startIndexing() {
        for (Site site : sites.getSites()) {
            Website website = new Website(INDEXING, LocalDateTime.now(), site.getUrl(), site.getName());
            Website websiteToDelete = websiteRepository.findWebsiteByUrl(website.getUrl());
            if(websiteToDelete != null ) {
                int websiteIdToDelete = websiteToDelete.getId();
                pageRepository.deleteByWebsite(websiteToDelete);
                websiteRepository.deleteById(websiteIdToDelete);
            }

            websiteRepository.save(website);
            Boolean isIndexing = new ForkJoinPool().invoke(new RecursiveSearch(website, site.getUrl(), pageRepository));
        }
    }

    @Override
    public StartIndexingResponse getResponse() {
        return new StartIndexingResponse();
    }
}
