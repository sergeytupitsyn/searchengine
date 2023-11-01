package searchengine.dto.indexing;

import lombok.Data;

@Data
public class IndexingResponseFalse extends IndexingResponse {
    private boolean result = false;
    private String error;

    public IndexingResponseFalse(String error) {
        this.error = error;
    }
}
