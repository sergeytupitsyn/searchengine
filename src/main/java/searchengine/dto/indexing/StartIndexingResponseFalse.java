package searchengine.dto.indexing;

import lombok.Data;

@Data
public class StartIndexingResponseFalse extends StartIndexingResponse {
    private boolean result = false;
    private String error = "Индексация уже запущена";
}
