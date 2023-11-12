package searchengine.services;

import searchengine.dto.indexing.IndexingResponse;

public interface IndexingService {

    IndexingResponse getStartResponse();
    IndexingResponse getStopResponse();
    IndexingResponse getIndexPageResponse(String url);
}
