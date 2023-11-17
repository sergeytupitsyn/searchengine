package searchengine.services;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.boot.context.properties.ConfigurationProperties;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SearchIndex;
import searchengine.model.Website;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SearchIndexRepository;
import searchengine.repositories.WebsiteRepository;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.RecursiveAction;
import static java.lang.Thread.sleep;

public class RecursiveSearch extends RecursiveAction {
    private final Website website;
    private final String parentLink;
    private final PageRepository pageRepository;
    private final WebsiteRepository websiteRepository;
    private final LemmaRepository lemmaRepository;
    private final SearchIndexRepository searchIndexRepository;

    public RecursiveSearch(Website website, String parentLink, PageRepository pageRepository, WebsiteRepository websiteRepository,
                           LemmaRepository lemmaRepository, SearchIndexRepository searchIndexRepository) {
        this.website = website;
        this.parentLink = parentLink;
        this.pageRepository = pageRepository;
        this.websiteRepository = websiteRepository;
        this.lemmaRepository = lemmaRepository;
        this.searchIndexRepository = searchIndexRepository;
    }

    @Override
    protected void compute() {
        ArrayList<String> linksThisPage;
        try {
            linksThisPage = pageParser(parentLink);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (!linksThisPage.isEmpty()) {
            for (String link : linksThisPage) {
                RecursiveSearch action = new RecursiveSearch(website, link, pageRepository,
                        websiteRepository, lemmaRepository, searchIndexRepository);
                action.fork();
            }
        }
    }

    @ConfigurationProperties(prefix = "jsoup-setting")
    public ArrayList<String> pageParser(String parentLink) throws IOException, InterruptedException {
        ArrayList<String> linkList = new ArrayList<String>();
        String path = parentLink.substring(website.getUrl().length() - 1);
        sleep(100);
        Connection.Response response = Jsoup.connect(parentLink).execute();
        int responseCode = response.statusCode();
        String content = "";
        if (responseCode == 200) {
            Document doc = response.parse();
            content = doc.outerHtml();
            Elements elements = doc.select("a[href]");
            elements.forEach(element -> {
                String link = element.attr("abs:href");
                if (link.startsWith(parentLink) && !linkList.contains(link) && !link.equals(parentLink) && !link.endsWith("#")) {
                    linkList.add(link);
                }
            });
        }
        Page page = new Page(website, path, responseCode, content);
        savePage(page);
        saveLemmasFromCodeInDB(page);
        website.setStatusTime(LocalDateTime.now());
        websiteRepository.save(website);
        return linkList;
    }

    public boolean isPageInDB(String path) {
        ArrayList<Page> pagesInDB = pageRepository.findAllPageByPath(path);
        ArrayList<Integer> websitesId = new ArrayList<>();
        pagesInDB.forEach(page -> websitesId.add(page.getWebsite().getId()));
        return !pagesInDB.isEmpty() && websitesId.contains(website.getId());
    }

    public synchronized void savePage(Page page) {
        if (!isPageInDB(page.getPath())) {
            pageRepository.save(page);
        }
    }

    public void saveLemmasFromCodeInDB(Page page) throws IOException {
        HashMap<String, Integer> lemmas = new LemmaSearch().splitToLemmas(page.getContent());
        for (String string : lemmas.keySet()) {
            Lemma lemmasInDB = lemmaRepository.findByLemma(string);

            if (lemmasInDB == null) {
                saveLemma(new Lemma(website, string, 1));
                continue;
            }
            lemmasInDB.setFrequency(lemmasInDB.getFrequency() + 1);
            saveLemma(lemmasInDB);
        }
    }

    public synchronized void saveLemma(Lemma lemma) {
        lemmaRepository.save(lemma);
    }

    public synchronized void saveSearchIndex(SearchIndex searchIndex) {
        searchIndexRepository.save(searchIndex);
    }
}
