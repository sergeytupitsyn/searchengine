package searchengine.dto.indexing;

import lombok.Data;

@Data
public class StartIndexingResponseTrue extends StartIndexingResponse{
    private boolean result = true;
}
