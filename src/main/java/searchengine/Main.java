package searchengine;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class Main {

    static String text = "Повторное появление леопарда в Осетии позволяет предположить,\n" +
            "что леопард постоянно обитает в некоторых районах Северного\n" +
            "Кавказа.";

    public static void main(String[] args) throws IOException {
        /*LuceneMorphology luceneMorph =
                new RussianLuceneMorphology();
        List<String> wordBaseForms =
                luceneMorph.getNormalForms("леса");
        wordBaseForms.forEach(System.out::println);*/
        HashMap<String,Integer> lemmas = splitToLemmas(text);
        for (String lemma : lemmas.keySet()) {
            System.out.println(lemma + " " + lemmas.get(lemma));
        }
    }

    public static HashMap<String, Integer> splitToLemmas(String text) throws IOException {

        String regex = "[0-9,.;:!?]";
        text = text.replaceAll(regex,"");
        text = text.toLowerCase();
        String[] words = text.split("\\s+");
        LuceneMorphology luceneMorph = new RussianLuceneMorphology();
        HashMap<String, Integer> lemmas = new HashMap<>();
        for (int i = 0; i < words.length; i++) {
            List<String> wordBaseForms = luceneMorph.getNormalForms(words[i]);
            wordBaseForms.forEach(word -> {
                if (lemmas.containsKey(word)) {
                    int lemmasCount = lemmas.get(word);
                    lemmas.replace(word, lemmasCount, lemmasCount + 1 );
                } else lemmas.put(word, 1);
            });
        }
        return lemmas;
    }
}
