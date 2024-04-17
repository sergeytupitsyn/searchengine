package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.IndexingStatus;
import searchengine.model.Website;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.WebsiteRepository;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SitesList sites;
    private final PageRepository pageRepository;
    private final WebsiteRepository websiteRepository;
    private final LemmaRepository lemmaRepository;

    @Override
    public StatisticsResponse getStatistics() {

        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(isIndexing());

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Site> sitesList = sites.getSites();
        for (Site site : sitesList) {
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
            Website website = websiteRepository.findWebsiteByUrl(site.getUrl());
            if (website == null) {
                continue;
            }
            int pages = pageRepository.findAllPageByWebsite(website).size();
            int lemmas = lemmaRepository.findAllByWebsite(website).size();
            item.setPages(pages);
            item.setLemmas(lemmas);
            item.setStatus(website.getStatus().toString());
            item.setError(website.getLastError());
            item.setStatusTime(website.getStatusTime().atZone(ZoneId.of(
                    "Europe/Moscow")).toInstant().toEpochMilli());
            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
            detailed.add(item);
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }

    public boolean isIndexing() {
        List<Site> sitesList = sites.getSites();
        for (Site site: sitesList) {
            Website website = websiteRepository.findWebsiteByUrl(site.getUrl());
            if (website == null) {
                return true;
            }
            if (website.getStatus().equals(IndexingStatus.INDEXING)) {
                return true;
            }
        }
        return false;
    }
}
