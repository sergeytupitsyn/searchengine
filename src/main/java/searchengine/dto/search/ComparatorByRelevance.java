package searchengine.dto.search;

import java.util.Comparator;

public class ComparatorByRelevance implements Comparator<SearchData> {
    @Override
    public int compare(SearchData o1, SearchData o2) {
        return Double.compare(o1.getRelevance(), o2.getRelevance());
    }
}
