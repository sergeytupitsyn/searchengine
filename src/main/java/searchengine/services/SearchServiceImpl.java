package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.search.*;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SearchIndex;
import searchengine.model.Website;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SearchIndexRepository;
import searchengine.repositories.WebsiteRepository;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final PageRepository pageRepository;
    private final WebsiteRepository websiteRepository;
    private final LemmaRepository lemmaRepository;
    private final SearchIndexRepository searchIndexRepository;;

    @Override
    public SearchResponse getSearchResponse(String query, String sait, int offset, int limit) {
        if (query.isEmpty()) {
            return new SearchResponseFalse("Задан пустой поисковый запрос");
        }
        ArrayList<Lemma> lemmasListFromQuery = getLemmaList(query);
        if (lemmasListFromQuery.isEmpty()) {
            return new SearchResponseFalse("Указанная страница не найдена");
        }
        ArrayList<Page> pageListToResponse = getPageListByLemmaList(lemmasListFromQuery, sait);
        if (pageListToResponse.isEmpty()) {
            return new SearchResponseFalse("Указанная страница не найдена");
        }
        ArrayList<Page> trimmedPageListToResponse = trimToLimit(pageListToResponse, limit, offset);
        HashMap<Page, Float> pageListWithRelevance = getPageListWithRelevance(lemmasListFromQuery, trimmedPageListToResponse);
        ArrayList<SearchData> data = new ArrayList<>();
        for (Page page : pageListWithRelevance.keySet()) {
            SearchData searchData = new SearchData();
            searchData.setSite(page.getWebsite().getUrl());
            searchData.setSiteName(page.getWebsite().getName());
            searchData.setUri(page.getPath().substring(1));
            searchData.setTitle(getTitle(page));
            searchData.setSnippet(new SnippetSearch(page.getContent(), lemmasListFromQuery).getSnippet());
            searchData.setRelevance(pageListWithRelevance.get(page));
            data.add(searchData);
        }
        data.sort(Comparator.comparing(SearchData :: getRelevance).reversed());
        SearchResponseTrue searchResponseTrue = new SearchResponseTrue();
        searchResponseTrue.setCount(pageListToResponse.size());
        searchResponseTrue.setData(data);
        return searchResponseTrue;
    }

    public String getTitle(Page page) {
        try {
            Document document = Jsoup.connect(page.getWebsite().getUrl() + page.getPath().substring(1)).get();
            Element element = document.selectFirst("title");
            if (element != null) {
                return element.text();
            }
            return "";
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public HashMap<Page, Float> getPageListWithRelevance (ArrayList<Lemma> lemmasListFromQuery, ArrayList<Page> pagesList) {
        HashMap<Page, Float> pageListWithRelevance = new HashMap<>();
        float maxRank = 0;
        for (Page page : pagesList) {
            float absRank = 0;
            for (Lemma lemma : lemmasListFromQuery) {
                absRank = absRank + searchIndexRepository.findByLemmaAndPage(lemma, page).getRank();
            }
            if (absRank > maxRank) {
                maxRank = absRank;
            }
            pageListWithRelevance.put(page, absRank);
        }
        for (Page page : pageListWithRelevance.keySet()) {
            float absRank = pageListWithRelevance.get(page);
            pageListWithRelevance.put(page, absRank / maxRank);
        }
        return pageListWithRelevance;
    }

    public ArrayList<Page> getPagesByLemma(Lemma lemma, String saitToSearch) {
        ArrayList<SearchIndex> indexList = searchIndexRepository.findAllByLemma(lemma);
        ArrayList<Page> pages = new ArrayList<>();
        if (saitToSearch == null) {
            indexList.forEach(searchIndex -> pages.add(searchIndex.getPage()));
        } else {
            for (SearchIndex searchIndex : indexList) {
                String url = searchIndex.getPage().getWebsite().getUrl();
                if (url.equals(saitToSearch)) {
                    pages.add(searchIndex.getPage());
                }
            }
        }
        return pages;
    }

    public ArrayList<Page> getPageListByLemmaList(ArrayList<Lemma> lemmaList, String sait) {
        ArrayList<Page> pagesList = getPagesByLemma(lemmaList.get(0), sait);
        for (int i = 1; i < lemmaList.size(); i++) {
            ArrayList<Page> pagesForItemLemma = getPagesByLemma(lemmaList.get(i), sait);
            Iterator<Page> iterator = pagesList.iterator();
            while (iterator.hasNext()) {
                if (!pagesForItemLemma.contains(iterator.next())) {
                    iterator.remove();
                }
            }
        }
        return pagesList;
    }

    public ArrayList<Page> trimToLimit(ArrayList<Page> pageList, int limit, int offset) {
    int size = pageList.size();
    if (size <= limit) {
        return pageList;
    }
        ArrayList<Page> pageArrayList = new ArrayList<>(pageList);
    pageArrayList.subList(0, offset).clear();
    pageArrayList.subList(limit, size - offset).clear();
    return pageArrayList;
    }

    public ArrayList<Lemma> getLemmaList(String query) {
        HashMap<String, Integer> lemmasStrFromQuery = new LemmaSearch().splitToLemmas(query);
        ArrayList<Lemma> lemmasListFromQuery = new ArrayList<>();
        for (String lemmaStr : lemmasStrFromQuery.keySet()) {
            Lemma lemma = lemmaRepository.findByLemma(lemmaStr);
            if (lemma == null) {
                continue;
            }
            if ((double) lemma.getFrequency() / pageRepository.count() > 0.95) {
                continue;
            }
            lemmasListFromQuery.add(lemma);
        }
        lemmasListFromQuery.sort(Comparator.comparing(Lemma::getFrequency));
        return lemmasListFromQuery;
    }
}
