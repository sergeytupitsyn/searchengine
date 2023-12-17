package searchengine.services;

import lombok.Setter;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

@Setter
public class LemmaSearch {
    private static final String[] functionWords = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ"};

    public HashMap<String, Integer> splitToLemmas(String text) {

        String[] words = text.toLowerCase().replaceAll("([^а-я\\s])","").trim().split("\\s+");
        LuceneMorphology luceneMorph = null;
        try {
            luceneMorph = new RussianLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        HashMap<String, Integer> lemmas = new HashMap<>();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            List<String> wordMorphInfo = luceneMorph.getMorphInfo(word);
            if (isFunctionWords(wordMorphInfo)) {
                continue;
            }
            List<String> wordBaseForms = luceneMorph.getNormalForms(word);
            if (wordBaseForms.isEmpty()) {
                continue;
            }
            String wordBaseForm = wordBaseForms.get(0);
            if (lemmas.containsKey(wordBaseForm)) {
                int lemmasCount = lemmas.get(wordBaseForm);
                lemmas.replace(wordBaseForm, lemmasCount, lemmasCount + 1 );
            } else lemmas.put(wordBaseForm, 1);
        }
        return lemmas;
    }

    public boolean isFunctionWords(List<String> wordMorphInfo) {
        for (String functionWord : functionWords) {
            if (wordMorphInfo.get(0).toUpperCase().contains(functionWord)) {
                return true;
            }
        }
        return false;
    }
}
