package searchengine.services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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

    public ArrayList<String> pageParser(String parentLink) throws IOException, InterruptedException {
        ArrayList<String> linkList = new ArrayList<String>();
        sleep(10000);
        Document doc = Jsoup.connect(parentLink).timeout(10000)
                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                .referrer("http://www.google.com")
                .ignoreHttpErrors(true)
                .ignoreContentType(true)
                .followRedirects(false).get();
        Elements allElements = doc.getAllElements();
        StringBuilder content = new StringBuilder();
        for (Element element : allElements) {
            content.append(element.data()).append("\n");
            if (content.capacity() > 10000) {
                break;
            }
        }
        Page page = new Page(website, parentLink, 200, content.toString());
        pageRepository.save(page);
        Elements elements = doc.select("a[href]");
        for (Element element : elements) {
            String link = element.attr("abs:href");
            if (!isPageInDB(link) && link.startsWith(parentLink) && !linkList.contains(link) && !link.equals(parentLink)) {
                linkList.add(link);
            }
        }
        return linkList;
    }

    public boolean isPageInDB(String link) {
        Page page = pageRepository.findPageByPath(link);
        return page != null;
    }
}
