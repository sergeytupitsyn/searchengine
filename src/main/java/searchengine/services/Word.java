package searchengine.services;

public class Word {

    private final String word;
    private final String normalForms;
    private final int textPosition;

    public Word(String word, int textPosition) {
        this.word = word;
        this.textPosition = textPosition;
        normalForms = new LemmaSearch().wordToLemmaString(word);
    }

    public String getWord() {
        return word;
    }

    public String getNormalForms() {
        return normalForms;
    }

    public int getTextPosition() {
        return textPosition;
    }
}
