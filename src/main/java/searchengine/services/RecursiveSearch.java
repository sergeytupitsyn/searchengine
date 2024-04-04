package searchengine.services;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.boot.context.properties.ConfigurationProperties;
import searchengine.model.Page;
import searchengine.model.Website;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveAction;
import static java.lang.Thread.sleep;

public class RecursiveSearch extends RecursiveAction {
    private final Website website;
    private final String parentLink;

    public RecursiveSearch(Website website, String parentLink) {
        this.website = website;
        this.parentLink = parentLink;
    }

    @Override
    protected void compute() {
        try {
            sleep(1500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        List<String> linksThisPage;
        linksThisPage = pageParser(parentLink);
        if (!linksThisPage.isEmpty()) {
            for (String link : linksThisPage) {
                RecursiveSearch action = new RecursiveSearch(website, link);
                action.fork();
            }
        }
    }

    @ConfigurationProperties(prefix = "jsoup-setting")
    public List<String> pageParser(String link) {
        try {
            sleep(1200);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        List<String> linkList = new ArrayList<>();
        String path = link.substring(website.getUrl().length() - 1);
        Connection.Response response = null;
        try {
            response = Jsoup.connect(link).execute();
        } catch (IOException e) {
            System.out.println("ошибка при Jsoup.connect(link).execute() " + e);
        }
        int responseCode = response != null ? response.statusCode() : 404;
        String content = "";
        if (responseCode == 200) {
            Document doc = null;
            try {
                doc = response.parse();
            } catch (IOException e) {
                System.out.println("ошибка при response.parse() " + e);
            }
            content = LemmaSearch.clearCodeFromTags(doc.outerHtml());
            Elements elements = doc.select("a[href]");
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
        IndexingServiceImpl.writeInPageList(new Page(website, path, responseCode, content));
        return linkList;
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
