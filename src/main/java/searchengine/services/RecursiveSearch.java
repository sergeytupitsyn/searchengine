package searchengine.services;

import lombok.NoArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import searchengine.Application;
import searchengine.model.*;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SearchIndexRepository;
import searchengine.repositories.WebsiteRepository;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RecursiveAction;
import static java.lang.Thread.sleep;

@NoArgsConstructor
public class RecursiveSearch extends RecursiveAction {
    private PageRepository pageRepository;
    private WebsiteRepository websiteRepository;
    private LemmaRepository lemmaRepository;
    private SearchIndexRepository searchIndexRepository;
    private Website website;
    private String parentLink;

    public RecursiveSearch(PageRepository pageRepository, WebsiteRepository websiteRepository, LemmaRepository lemmaRepository, SearchIndexRepository searchIndexRepository, Website website, String parentLink) {
        this.pageRepository = pageRepository;
        this.websiteRepository = websiteRepository;
        this.lemmaRepository = lemmaRepository;
        this.searchIndexRepository = searchIndexRepository;
        this.website = website;
        this.parentLink = parentLink;
    }

    @Override
    protected void compute() {
        List<String> linksThisPage;
        linksThisPage = pageParser(parentLink);
        if (!linksThisPage.isEmpty()) {
            for (String link : linksThisPage) {
                RecursiveSearch action = new RecursiveSearch(pageRepository, websiteRepository, lemmaRepository, searchIndexRepository, website, link);
                action.fork();
            }
        }
    }

    @ConfigurationProperties(prefix = "jsoup-setting")
    public List<String> pageParser(String link) {
        try {
            sleep(1200);
        } catch (InterruptedException e) {
            Logger logger = LoggerFactory.getLogger(Application.class);
            logger.error(e.getMessage());
            Thread.currentThread().interrupt();
        }
        List<String> linkList = new ArrayList<>();
        String path = link.substring(website.getUrl().length() - 1);
        Connection.Response response = null;
        try {
            response = Jsoup.connect(link).execute();
        } catch (IOException e) {
            Logger logger = LoggerFactory.getLogger(Application.class);
            logger.error(e.getMessage());
        }
        int responseCode = response != null ? response.statusCode() : 404;
        String content = "";
        if (responseCode == 200) {
            Document doc = null;
            Elements elements = null;
            try {
                doc = response.parse();
            } catch (IOException e) {
                Logger logger = LoggerFactory.getLogger(Application.class);
                logger.error(e.getMessage());
            }
            if (doc != null) {
                content = LemmaSearch.clearCodeFromTags(doc.outerHtml());
                elements = doc.select("a[href]");
            }
            if (elements != null) {
                elements.forEach(element -> {
                    String childLink = element.attr("abs:href");
                    if (childLink.startsWith(website.getUrl())
                            && !IndexingServiceImpl.parsedLinksList.contains(childLink)
                            && isLinkCorrect(childLink)) {
                        linkList.add(childLink);
                        IndexingServiceImpl.parsedLinksList.add(childLink);
                    }
                });
            }
        }
        Map<String, Integer> lemmas = new LemmaSearch().splitToLemmas(content);
        try {
            saveIndexingDataInDB(IndexingStatus.INDEXING, "", new Page(website, path, responseCode, content), lemmas);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return linkList;
    }

    public void saveIndexingDataInDB(IndexingStatus status, String lastError, Page page, Map<String, Integer> lemmas) throws SQLException {
        if (!isPageInDB(page) && IndexingServiceImpl.isIndexingStarted) {
            pageRepository.save(page);
            saveLemmaInDB(page, lemmas);
            Website website = page.getWebsite();
            website.setStatusTime(LocalDateTime.now());
            websiteRepository.save(website);
        }
    }

    public void saveLemmaInDB(Page page, Map<String, Integer> lemmas) throws SQLException{
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

    public boolean isPageInDB(Page page) throws SQLException {
        List<Page> pagesInDB = pageRepository.findAllPageByPath(page.getPath());
        List<Integer> websitesId = new ArrayList<>();
        pagesInDB.forEach(pageInDB-> websitesId.add(pageInDB.getWebsite().getId()));
        return !pagesInDB.isEmpty() && websitesId.contains(page.getWebsite().getId());
    }

    public boolean isLemmaInDB(String lemmaString) throws SQLException{
        Lemma lemma = lemmaRepository.findByLemma(lemmaString);
        return lemma != null;
    }

    public boolean isLinkCorrect(String link) {
        String[] extension = {"method=", "jpg", "png", "mp4", "jpeg", "pdf"};
        if (link.endsWith("#")) {
            return false;
        }
        for (String string : extension) {
            if (link.contains(string)) {
                return false;
            }
        }
        return true;
    }
}
