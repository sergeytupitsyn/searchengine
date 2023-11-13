package searchengine;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import searchengine.services.LemmaSearch;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class Main {

    static String text = "Повторное появление леопарда в Осетии позволяет предположить,\n" +
            "что леопард постоянно обитает в некоторых районах Северного\n" +
            "Кавказа.";

    public static void main(String[] args) throws IOException {
        LemmaSearch lemmaSearch = new LemmaSearch();
        HashMap<String, Integer> lemmas = lemmaSearch.splitToLemmas(text);
        for (String lemma : lemmas.keySet()) {
            System.out.println(lemma + " = " + lemmas.get(lemma));
        }
    }
}
