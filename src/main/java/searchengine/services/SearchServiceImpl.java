package searchengine.services;

import lombok.RequiredArgsConstructor;
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
        ArrayList<SearchIndex> searchingIndexes = new ArrayList<>();
        searchingIndexes.addAll(searchIndexRepository.findAllByLemma(lemmasListFromQuery.get(0)));
        for (int i = 1; i < lemmasListFromQuery.size(); i++) {
            ArrayList<SearchIndex>  indexListForCurrentLemma = searchIndexRepository.findAllByLemma(lemmasListFromQuery.get(i));
            Iterator<SearchIndex> iterator = searchingIndexes.iterator();
            while (iterator.hasNext()) {
                if (!indexListForCurrentLemma.contains(iterator.next())) {
                    iterator.remove();
                }
            }
        }
        ArrayList<SearchData> searchingPages = new ArrayList<>();
        for (SearchIndex index : searchingIndexes) {
            SearchData searchData = new SearchData();
            searchData.setSite(index.getPage().getWebsite().getUrl());
            searchData.setSiteName(index.getPage().getWebsite().getName());
            searchData.setUri("/path/to/page/6784");
            searchData.setTitle("Заголовок страницы, которую выводим");
            searchData.setSnippet("Фрагмент текста, в котором найдены совпадения, <b>выделенные жирным</b>, в формате HTML");
            searchData.setRelevance(Math.random());
            searchingPages.add(searchData);
        }
        SearchResponseTrue searchResponseTrue = new SearchResponseTrue();
        searchResponseTrue.setCount(searchingPages.size());
        searchResponseTrue.setDates(searchingPages);
        return searchResponseTrue;
    }
}
