package searchengine.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LemmaSearchTests {

    LemmaSearch lemmaSearch = new LemmaSearch();

    @Test
    @DisplayName("Test split to lemmas")
    public void testSplitToLemmas() {
        String text = "появление леопарда в Осетии позволяет предположить, что леопард обитает";
        Map<String, Integer> lemmas = lemmaSearch.splitToLemmas(text);
        assertEquals(6, lemmas.size());
        assertTrue(lemmas.containsKey("появление") && lemmas.containsKey("осетия") &&
                lemmas.containsKey("позволять") && lemmas.containsKey("предположить") && lemmas.containsKey("обитать"));
        assertEquals(2, lemmas.get("леопард"));
    }

    @Test
    @DisplayName("Test is function words")
    public void testIsFunctionWords() {
        String[] words = {"или", "над", "под", "ох", "и", "ой"};
        for (String word : words) {
            assertTrue(lemmaSearch.isFunctionWords(word));
        }
    }

    @Test
    @DisplayName("Test clear code from tags")
    public void testClearCodeFromTags() {
        String content = "<ul><li>Междометие&#160;— это разнородный по составу синтаксический класс, стоящий вне " +
                "деления слов по <a href=\"/wiki/%D0%A7%D0%B0%D1%81%D1%82%D0%B8_%D1%80%D0%B5%D1%87%D0%B8_%D0%B2_" +
                "%D1%80%D1%83%D1%81%D1%81%D0%BA%D0%BE%D0%BC_%D1%8F%D0%B7%D1%8B%D0%BA%D0%B5\" title=\"Части речи " +
                "в русском языке\">частям</a> речи.</li>\n<li>Междометия входят в систему частей речи, но стоят в" +
                " ней изолированно<sup id=\"cite_ref-2\" class=\"reference\"><a href=\"#cite_note-2\"><span class=\"" +
                "cite-bracket\">&#91;</span>2<span class=\"cite-bracket\">&#93;</span></a></sup>.</li>";
        String text = "Междометие — это разнородный по составу синтаксический класс, стоящий вне деления слов по " +
                "частям речи. Междометия входят в систему частей речи, но стоят в ней изолированно[2].";
        assertEquals(text, LemmaSearch.clearCodeFromTags(content));
    }

    @Test
    @DisplayName("Test word to lemma string")
    public void testWordToLemmaString() {
        String[] words = {"повторное", "обитает", "районам", "северного", "кавказа"};
        String[] lemmas = {"повторный", "обитать", "район", "северный", "кавказ"};
        for (int i = 0; i < 4; i++) {
            assertEquals(lemmas[i], lemmaSearch.wordToLemmaString(words[i]));
        }
    }

}
