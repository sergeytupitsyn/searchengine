package searchengine.dto.search;

import lombok.Data;

@Data
public class SearchResponseFalse extends SearchResponse{
    private boolean result = false;
    private String error = "Задан пустой поисковый запрос";
}
