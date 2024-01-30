package searchengine.services;

import lombok.Getter;

@Getter
public class Word {

    private final String word;
    private String normalForms;

    public Word(String word) {
        this.word = word;
    }

    public void setNormalForms(String normalForms) {
        this.normalForms = normalForms;
    }
}
