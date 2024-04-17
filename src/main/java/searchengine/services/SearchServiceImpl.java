package searchengine.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.search.SearchResponseFalse;
import searchengine.dto.search.SearchResponseTrue;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SearchIndex;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SearchIndexRepository;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final SearchIndexRepository searchIndexRepository;

    @Override
    public SearchResponse getSearchResponse(String query, String sait,
                                            int offset, int limit) {
        if (query.isEmpty()) {
            return new SearchResponseFalse("Задан пустой поисковый запрос");
        }
        List<Lemma> lemmasListFromQuery = getLemmaList(query);
        if (lemmasListFromQuery.isEmpty()) {
            return new SearchResponseFalse("Указанная страница не найдена");
        }
        List<Page> pageListToResponse = getPageListByLemmaList(lemmasListFromQuery, sait);
        if (pageListToResponse.isEmpty()) {
            return new SearchResponseFalse("Указанная страница не найдена");
        }
        List<Page> trimmedPageListToResponse = trimToLimit(pageListToResponse, limit, offset);
        Map<Page, Float> pageListWithRelevance = getPageListWithRelevance(lemmasListFromQuery,
                trimmedPageListToResponse);
        List<SearchData> data = new ArrayList<>();
        for (Page page : pageListWithRelevance.keySet()) {
            SearchData searchData = new SearchData();
            searchData.setSite(page.getWebsite().getUrl());
            searchData.setSiteName(page.getWebsite().getName());
            searchData.setUri(page.getPath().substring(1));
            searchData.setTitle(getTitle(page));
            searchData.setSnippet(new SnippetSearch(page.getContent(),
                    lemmasListFromQuery).getSnippet());
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
            Document document = Jsoup.connect(page.getWebsite().getUrl()
                    + page.getPath().substring(1)).get();
            Element element = document.selectFirst("title");
            if (element != null) {
                return element.text();
            }
            return "";
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<Page, Float> getPageListWithRelevance (List<Lemma> lemmasListFromQuery,
                                                      List<Page> pagesList) {
        Map<Page, Float> pageListWithRelevance = new HashMap<>();
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

    public List<Page> getPagesByLemma(Lemma lemma, String saitToSearch) {
        List<SearchIndex> indexList = searchIndexRepository.findAllByLemma(lemma);
        List<Page> pages = new ArrayList<>();
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

    public List<Page> getPageListByLemmaList(List<Lemma> lemmaList, String sait) {
        List<Page> pagesList = getPagesByLemma(lemmaList.get(0), sait);
        for (int i = 1; i < lemmaList.size(); i++) {
            List<Page> pagesForItemLemma = getPagesByLemma(lemmaList.get(i), sait);
            pagesList.removeIf(page -> !pagesForItemLemma.contains(page));
        }
        return pagesList;
    }

    public List<Page> trimToLimit(List<Page> pageList, int limit, int offset) {
    int size = pageList.size();
    if (size <= limit) {
        return pageList;
    }
        List<Page> pageArrayList = new ArrayList<>(pageList);
    pageArrayList.subList(0, offset).clear();
    pageArrayList.subList(limit, size - offset).clear();
    return pageArrayList;
    }

    public List<Lemma> getLemmaList(String query) {
        Map<String, Integer> lemmasStrFromQuery = new LemmaSearch().splitToLemmas(query);
        List<Lemma> lemmasListFromQuery = new ArrayList<>();
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
