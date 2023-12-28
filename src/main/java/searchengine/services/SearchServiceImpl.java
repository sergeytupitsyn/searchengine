package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.search.SearchResponseTrue;
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
    private final SearchIndexRepository searchIndexRepository;
    private final SitesList sites;

    @Override
    public SearchResponse getSearchResponse(String query, String url, int offset, int limit) {
        HashMap<String, Integer> lemmasStrFromQuery = new LemmaSearch().splitToLemmas(query);
        ArrayList<Lemma> lemmasListFromQuery = new ArrayList<>();
        for (String lemmaStr : lemmasStrFromQuery.keySet()) {
            Lemma lemma = lemmaRepository.findByLemma(lemmaStr);
            if (lemma == null) {
                continue;
            }
            if ((double) lemma.getFrequency() / pageRepository.count() > 0.8) {
                continue;
            }
            lemmasListFromQuery.add(lemma);
        }
        lemmasListFromQuery.sort(Comparator.comparing(Lemma::getFrequency));
        Collections.reverse(lemmasListFromQuery);
        ArrayList<Page> searchingPages  = new ArrayList<>();
        ArrayList<SearchIndex> searchingIndexes = new ArrayList<>(searchIndexRepository.findAllByLemma(lemmasListFromQuery.get(0)));
        searchingIndexes.forEach(searchIndex -> searchingPages.add(searchIndex.getPage()));
        for (int i = 1; i < lemmasListFromQuery.size(); i++) {
            ArrayList<SearchIndex>  indexListForCurrentLemma = searchIndexRepository.findAllByLemma(lemmasListFromQuery.get(i));
            ArrayList<Page> pagesForCurrentLemma = new ArrayList<>();
            indexListForCurrentLemma.forEach(searchIndex -> pagesForCurrentLemma.add(searchIndex.getPage()));
            Iterator<Page> iterator = searchingPages.iterator();
            while (iterator.hasNext()) {
                if (!pagesForCurrentLemma.contains(iterator.next())) {
                    iterator.remove();
                }
            }
        }
        ArrayList<SearchData> data = new ArrayList<>();
        for (Page page : searchingPages) {
            SearchData searchData = new SearchData();
            searchData.setSite(page.getWebsite().getUrl());
            searchData.setSiteName(page.getWebsite().getName());
            searchData.setUri(page.getPath().substring(1));
            searchData.setTitle(getTitle(page));
            searchData.setSnippet("Фрагмент текста, в котором найдены совпадения, <b>выделенные жирным</b>, в формате HTML");
            searchData.setRelevance(Math.random());
            data.add(searchData);
        }
        SearchResponseTrue searchResponseTrue = new SearchResponseTrue();
        searchResponseTrue.setCount(searchingPages.size());
        searchResponseTrue.setData(data);
        return searchResponseTrue;
    }

    public String getTitle(Page page) {
        try {
            Document document = Jsoup.connect(page.getWebsite().getUrl() + page.getPath().substring(1)).get();
            Element element = document.selectFirst("title");
            return element.text();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
