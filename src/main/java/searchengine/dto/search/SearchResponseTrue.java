package searchengine.dto.search;

import lombok.Data;

import java.util.ArrayList;
@Data
public class SearchResponseTrue extends SearchResponse{
    private boolean result = true;
    private int count;
    private ArrayList<SearchData> data;
}
