package searchengine.dto.indexing;

import lombok.Data;

@Data
public class IndexingResponseTrue extends IndexingResponse {
    private boolean result = true;
}
