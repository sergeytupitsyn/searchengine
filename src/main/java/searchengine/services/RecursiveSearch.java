package searchengine.services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.boot.context.properties.ConfigurationProperties;
import searchengine.model.Page;
import searchengine.model.Website;
import searchengine.repositories.PageRepository;
import searchengine.repositories.WebsiteRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.RecursiveAction;
import static java.lang.Thread.sleep;

public class RecursiveSearch extends RecursiveAction {
    private final Website website;
    private final String parentLink;
    private final PageRepository pageRepository;
    private final WebsiteRepository websiteRepository;
    public RecursiveSearch(Website website, String parentLink, PageRepository pageRepository, WebsiteRepository websiteRepository) {
        this.website = website;
        this.parentLink = parentLink;
        this.pageRepository = pageRepository;
        this.websiteRepository = websiteRepository;
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
                RecursiveSearch action = new RecursiveSearch(website, link, pageRepository, websiteRepository);
                action.fork();
            }
        }
    }

    @ConfigurationProperties(prefix = "jsoup-setting")
    public ArrayList<String> pageParser(String parentLink) throws IOException, InterruptedException {
        ArrayList<String> linkList = new ArrayList<String>();
        sleep(100);
        int responseCode = Jsoup.connect(parentLink).execute().statusCode();
        StringBuilder content = new StringBuilder();
        if (responseCode == 200) {
            Document doc = Jsoup.connect(parentLink).get();
            Elements allElements = doc.getAllElements();
            allElements.forEach(element -> content.append(element.data()).append("\n"));
            Elements elements = doc.select("a[href]");
            elements.forEach(element -> {
                String link = element.attr("abs:href");
                if (!isPageInDB(link) && link.startsWith(parentLink) && !linkList.contains(link) && !link.equals(parentLink)) {
                    linkList.add(link);
                }
            });
        }
        Page page = new Page(website, parentLink,responseCode, content.toString());
        pageRepository.save(page);
        return linkList;
    }

    public boolean isPageInDB(String link) {
        Page page = pageRepository.findPageByPath(link);
        return page != null;
    }
}
