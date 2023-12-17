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
        ArrayList<String> linksThisPage;
        try {
            linksThisPage = pageParser(parentLink);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (!linksThisPage.isEmpty()) {
            for (String link : linksThisPage) {
                RecursiveSearch action = new RecursiveSearch(website, link);
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
        IndexingServiceImpl.writeInPageList(new Page(website, path, responseCode, content));
        return linkList;
    }
}
