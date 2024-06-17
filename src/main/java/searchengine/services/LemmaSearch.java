package searchengine.services;

import lombok.Setter;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import searchengine.Application;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Setter
public class LemmaSearch {

    private LuceneMorphology luceneMorphology;

    private static final String[] functionWords = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ"};

    public LemmaSearch() {
        try {
            luceneMorphology = new RussianLuceneMorphology();
        } catch (IOException e) {
            Logger logger = LoggerFactory.getLogger(Application.class);
            logger.error(e.getMessage());
        }
    }

    public Map<String, Integer> splitToLemmas(String text) {

        String[] words = text.toLowerCase().replaceAll("([^а-я\\s])",
                "").trim().split("\\s+");
        Map<String, Integer> lemmas = new HashMap<>();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            List<String> wordMorphInfo = luceneMorphology.getMorphInfo(word);
            if (isFunctionWords(wordMorphInfo)) {
                continue;
            }
            List<String> wordBaseForms = luceneMorphology.getNormalForms(word);
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

    public static String clearCodeFromTags (String content) {
        return Jsoup.parse(content).text();
    }

    public String wordToLemmaString (String word) {
        word = word.toLowerCase().replaceAll("([^а-я])","");
        List<String> wordBaseForms = luceneMorphology.getNormalForms(word);
        if (wordBaseForms.isEmpty()) {
            return "";
        }
        return wordBaseForms.get(0);
    }
}
