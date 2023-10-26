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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;

import static java.lang.Thread.sleep;

public class RecursiveSearch extends RecursiveAction {
    private final Website website;
    private final String link;
    private final PageRepository pageRepository;
    private final WebsiteRepository websiteRepository;
    public RecursiveSearch(Website website, String link, PageRepository pageRepository, WebsiteRepository websiteRepository) {
        this.website = website;
        this.link = link;
        this.pageRepository = pageRepository;
        this.websiteRepository = websiteRepository;
    }

    @Override
    protected void compute() {

        ArrayList<String> linksThisPage;
        try {
            linksThisPage = pageParser(link);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (!linksThisPage.isEmpty()) {
            for (String link : linksThisPage) {
                RecursiveSearch action = new RecursiveSearch(website, link, pageRepository, websiteRepository);
                action.fork();
                action.join();
            }
        }
    }

    public ArrayList<String> pageParser(String link) throws IOException, InterruptedException {
        ArrayList<String> list = new ArrayList<String>();
        if (!isPageInDB(link)) {
            sleep(100);
            Document doc = Jsoup.connect(link).timeout(10000)
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("http://www.google.com")
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .followRedirects(false).get();
            Elements allElements = doc.getAllElements();
            StringBuilder content = new StringBuilder();
            for (Element element : allElements) {
                content.append(element.data()).append("\n");
            }
            String path = link.substring(website.getUrl().length());
            Page page = new Page(website, path, 200, content.toString());
            pageRepository.save(page);
            website.setStatusTime(LocalDateTime.now());
            websiteRepository.save(website);
            Elements elements = doc.select("a[href]");
            for (Element element : elements) {
                String link1 = element.attr("abs:href");
                if (link1.startsWith(link) && link1.endsWith("/") && !list.contains(link1) && !link.equals(link1)) {
                    list.add(link1);
                }
            }
        }
        return list;
    }

    public boolean isPageInDB(String link) {
        String path = link.substring(website.getUrl().length());
        Page page = pageRepository.findPageByPath(path);
        if (page == null){
            return false;
        }else {
            return true;
        }
    }
}
