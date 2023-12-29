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
    private final SearchIndexRepository searchIndexRepository;
    private final SitesList sites;

    @Override
    public SearchResponse getSearchResponse(String query, String url, int offset, int limit) {
        if (query.isEmpty()) {
            return new SearchResponseFalse("Задан пустой поисковый запрос");
        }
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
        if (lemmasListFromQuery.isEmpty()) {
            return new SearchResponseFalse("Указанная страница не найдена");
        }
        lemmasListFromQuery.forEach(lemma -> System.out.println(lemma.getLemma() + lemma.getFrequency()));
        ArrayList<Page> pagesList = getPagesByLemma(lemmasListFromQuery.get(0));
        for (int i = 1; i < lemmasListFromQuery.size(); i++) {
            ArrayList<Page> pagesForItemLemma = getPagesByLemma(lemmasListFromQuery.get(i));
            Iterator<Page> iterator = pagesList.iterator();
            while (iterator.hasNext()) {
                if (!pagesForItemLemma.contains(iterator.next())) {
                    iterator.remove();
                }
            }
        }
        if (pagesList.isEmpty()) {
            return new SearchResponseFalse("Указанная страница не найдена");
        }
        HashMap<Page, Float> pageListWithRelevance = getPageListWithRelevance(lemmasListFromQuery, pagesList);
        ArrayList<SearchData> data = new ArrayList<>();
        for (Page page : pageListWithRelevance.keySet()) {
            SearchData searchData = new SearchData();
            searchData.setSite(page.getWebsite().getUrl());
            searchData.setSiteName(page.getWebsite().getName());
            searchData.setUri(page.getPath().substring(1));
            searchData.setTitle(getTitle(page));
            searchData.setSnippet("Фрагмент текста, в котором найдены совпадения, <b>выделенные жирным</b>, в формате HTML");
            searchData.setRelevance(pageListWithRelevance.get(page));
            data.add(searchData);
        }
        data.sort(new ComparatorByRelevance().reversed());
        SearchResponseTrue searchResponseTrue = new SearchResponseTrue();
        searchResponseTrue.setCount(pagesList.size());
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

    public ArrayList<Page> getPagesByLemma(Lemma lemma) {
        ArrayList<SearchIndex> searchIndices = searchIndexRepository.findAllByLemma(lemma);
        ArrayList<Page> pages = new ArrayList<>();
        searchIndices.forEach(searchIndex -> pages.add(searchIndex.getPage()));
        return pages;
    }

    public String getSnippet (ArrayList<Lemma> lemmasListFromQuery, Page page) {

        return " ";
    }
}
