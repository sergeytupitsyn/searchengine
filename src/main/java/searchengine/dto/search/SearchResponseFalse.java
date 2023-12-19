package searchengine.dto.search;

public class SearchResponseFalse extends SearchResponse{
    private boolean result = false;
    private String error = "Задан пустой поисковый запрос";
}
