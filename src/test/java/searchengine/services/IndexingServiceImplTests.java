package searchengine.services;

import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponseFalse;
import searchengine.dto.indexing.IndexingResponseTrue;
import searchengine.model.IndexingStatus;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Website;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SearchIndexRepository;
import searchengine.repositories.WebsiteRepository;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class IndexingServiceImplTests {

    private final PageRepository pageRepository = Mockito.mock(PageRepository.class);
    private final WebsiteRepository websiteRepository = Mockito.mock(WebsiteRepository.class);
    private final LemmaRepository lemmaRepository = Mockito.mock(LemmaRepository.class);
    private final SearchIndexRepository searchIndexRepository = Mockito.mock(SearchIndexRepository.class);
    private final SitesList sitesList = new SitesList();
    private final IndexingServiceImpl indexingService = new IndexingServiceImpl(pageRepository, websiteRepository,
            lemmaRepository, searchIndexRepository, sitesList);

    @Test
    @Order(1)
    @DisplayName("Test get start response if indexing has not started")
    public void testGetStartResponseIfIndexingNotStarted() {
        assertEquals(new IndexingResponseTrue(), indexingService.getStartResponse());
    }

    @Test
    @Order(2)
    @DisplayName("Test get start response if indexing has started")
    public void testGetStartResponseIfIndexingStarted() {
        assertEquals(new IndexingResponseFalse("Индексация уже запущена"), indexingService.getStartResponse());
    }

    @Test
    @Order(4)
    @DisplayName("Test get stop response if indexing has not started")
    public void testGetStopResponseIfIndexingNotStarted() {
        assertEquals(new IndexingResponseFalse("Индексация не запущена"), indexingService.getStopResponse());
    }

    @Test
    @Order(3)
    @DisplayName("Test get stop response if indexing has started")
    public void testGetStopResponseIfIndexingStarted() {
        List<Site> list = new ArrayList<>();
        sitesList.setSites(list);
        assertEquals(new IndexingResponseTrue(), indexingService.getStopResponse());
    }

    @Test
    @Order(5)
    @DisplayName("Test get index page response")
    public void testGetIndexPageResponse() throws SQLException {
        Site site = new Site();
        site.setUrl("https://skillbox.ru/");
        List<Site> list = new ArrayList<>();
        list.add(site);
        sitesList.setSites(list);
        IndexingServiceImpl indexingServiceSpy = Mockito.spy(indexingService);
        doNothing().when(indexingServiceSpy).indexingPage("https://skillbox.ru/", site);
        assertEquals(new IndexingResponseFalse("Данная страница находится за пределами сайтов, указанных в " +
                "конфигурационном файле"), indexingServiceSpy.getIndexPageResponse("https://lenta.ru/"));
        assertEquals(new IndexingResponseTrue(), indexingServiceSpy.getIndexPageResponse("https://skillbox.ru/"));
        verify(indexingServiceSpy, Mockito.times(1)).indexingPage("https://skillbox.ru/", site);
    }

    @Test
    @Order(6)
    @DisplayName("Test start indexing")
    public void TestStartIndexing () throws SQLException {
        IndexingServiceImpl indexingServiceSpy = Mockito.spy(indexingService);
        Site site = new Site();
        site.setUrl("https://skillbox.ru/");
        List<Site> list = new ArrayList<>();
        list.add(site);
        sitesList.setSites(list);
        Website website = new Website(IndexingStatus.INDEXED, LocalDateTime.now(), "",
                "https://skillbox.ru/", "Skillbox");
        when(websiteRepository.findWebsiteByUrl("https://skillbox.ru/")).thenReturn(website);
        doNothing().when(indexingServiceSpy).waitingForIndexingToFinish();
        indexingServiceSpy.startIndexing();
        for (ForkJoinPool forkJoinPool : indexingServiceSpy.forkJoinPoolList.keySet()) {
            assertEquals(site, indexingServiceSpy.forkJoinPoolList.get(forkJoinPool));
        }
        assertTrue(IndexingServiceImpl.isIndexingStarted);
    }

    @Test
    @Order(7)
    @DisplayName("Test waiting for indexing to finish")
    public void TestWaitingForIndexingToFinish() throws SQLException {
        IndexingServiceImpl.isIndexingStarted = true;
        IndexingServiceImpl indexingServiceSpy = Mockito.spy(indexingService);
        Site site = new Site();
        site.setUrl("https://skillbox.ru/");
        List<Site> list = new ArrayList<>();
        list.add(site);
        sitesList.setSites(list);
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        indexingServiceSpy.forkJoinPoolList.put(forkJoinPool, site);
        doNothing().when(indexingServiceSpy).finishIndexing(any(), any(), any());
        indexingServiceSpy.waitingForIndexingToFinish();
        verify(indexingServiceSpy, Mockito.times(1)).finishIndexing(any(),any(), any());
        assertFalse(IndexingServiceImpl.isIndexingStarted);
    }

    @Test
    @Order(8)
    @DisplayName("Test remove site data from BD")
    public void TestRemoveSiteDataFromBD() throws SQLException {
        Website website = new Website(IndexingStatus.INDEXED, LocalDateTime.now(), "",
                "https://skillbox.ru/", "Skillbox");
        Page page = new Page();
        ArrayList<Page> pages = new ArrayList<>();
        pages.add(page);
        Lemma lemma = new Lemma();
        ArrayList<Lemma> lemmas= new ArrayList<>();
        lemmas.add(lemma);
        when(pageRepository.findAllPageByWebsite(website)).thenReturn(pages);
        when(lemmaRepository.findAllByWebsite(website)).thenReturn(lemmas);
        indexingService.removeSiteDataFromBD(website);
        verify(searchIndexRepository).deleteAllByPage(any());
        verify(searchIndexRepository).deleteAllByLemma(any());
        verify(lemmaRepository).deleteByWebsite(any());
        verify(pageRepository).deleteByWebsite(any());
        verify(websiteRepository).deleteById(any());
    }

    @Test
    @Order(9)
    @DisplayName("Test finish indexing")
    public void TestFinishIndexing() throws SQLException {
        Website website = new Website(IndexingStatus.INDEXED, LocalDateTime.now(), "",
                "https://skillbox.ru/", "Skillbox");
        Site site = new Site();
        site.setUrl("https://skillbox.ru/");
        when(websiteRepository.findWebsiteByUrl("https://skillbox.ru/")).thenReturn(website);
        indexingService.finishIndexing(site, IndexingStatus.INDEXED, "");
        verify(websiteRepository).save(website);
    }

    @Test
    @Order(10)
    @DisplayName("Test indexing page")
    public void TestIndexingPage() throws SQLException {
        IndexingServiceImpl.isIndexingStarted = false;
        Website website = new Website(IndexingStatus.INDEXED, LocalDateTime.now(), "",
                "https://skillbox.ru/", "Skillbox");
        Page page = new Page();
        Site site = new Site();
        site.setUrl("https://skillbox.ru/");
        IndexingServiceImpl indexingServiceSpy = Mockito.spy(indexingService);
        when(websiteRepository.findWebsiteByUrl("https://skillbox.ru/")).thenReturn(website);
        when(pageRepository.findPageByPathAndWebsite(any(),any())).thenReturn(page);
        doNothing().when(indexingServiceSpy).removePageDataFromBD(page);
        doNothing().when(indexingServiceSpy).waitingForIndexingToFinish();
        indexingServiceSpy.indexingPage("https://skillbox.ru/", site);
        verify(indexingServiceSpy).removePageDataFromBD(page);
        verify(indexingServiceSpy).waitingForIndexingToFinish();
        assertTrue(IndexingServiceImpl.isIndexingStarted);
    }

    @Test
    @Order(11)
    @DisplayName("Test remove page data from BD")
    public void TestRemovePageDataFromBD() throws SQLException {
        Page page = new Page();
        page.setContent("Съешь еще этих мягких французских булок да выпей чаю");
        doNothing().when(searchIndexRepository).deleteAllByPage(page);
        Website website = new Website(IndexingStatus.INDEXED, LocalDateTime.now(), "",
                "https://skillbox.ru/", "Skillbox");
        Lemma lemma = new Lemma(website, "lemma", 10);
        when(lemmaRepository.findByLemmaAndWebsite(any(), any())).thenReturn(lemma);
        doNothing().when(lemmaRepository).delete(lemma);
        when(lemmaRepository.save(lemma)).thenReturn(lemma);
        doNothing().when(pageRepository).delete(page);
        indexingService.removePageDataFromBD(page);
        verify(searchIndexRepository).deleteAllByPage(page);
        verify(lemmaRepository, Mockito.times(0)).delete(lemma);
        assertEquals(2, lemma.getFrequency());
        verify(lemmaRepository, Mockito.times(8)). save(lemma);
        verify(pageRepository).delete(page);
    }
}


