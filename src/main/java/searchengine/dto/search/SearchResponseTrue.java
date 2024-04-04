package searchengine.dto.search;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SearchResponseTrue extends SearchResponse{
    private boolean result = true;
    private int count;
    private List<SearchData> data;
}
