package searchengine.services;

import searchengine.model.Lemma;

import java.util.ArrayList;

public class SnippetSearch {

    private final String text;
    private final ArrayList<Lemma> lemmaListFromQuery;
    private final ArrayList<Word> wordList = new ArrayList<>();
    private int snippetSize;

    public SnippetSearch(String text, ArrayList<Lemma> lemmaListFromQuery) {
        this.text = text;
        this.lemmaListFromQuery = lemmaListFromQuery;
        String[] contentSplitIntoWords = text.toLowerCase().replaceAll("([^а-я\\s])","").trim().split("\\s+");
        for (int i = 0; i < contentSplitIntoWords.length; i++) {
            Word word = new Word(contentSplitIntoWords[i], i);
            wordList.add(word);
        }
        snippetSize = 20;
        if (snippetSize > wordList.size()) {
            snippetSize = wordList.size();
        }
    }

    public int[] getQueryPositionInTextForLemmaList(ArrayList<Lemma> lemmaList) {
        for (int searchBox = lemmaList.size(); searchBox < snippetSize; searchBox++) {
            for (int i = 0; i < wordList.size() - searchBox; i++) {
                if (isQueryInSearchBox(i, searchBox)) {
                    return new int[] {i, i + searchBox};
                }
            }
        }
        return null;
    }

    public boolean isQueryInSearchBox(int start, int searchBox) {
        ArrayList<String> wordInSearchBox = new ArrayList<>();
        for (int i = 0; i < searchBox; i++) {
            wordInSearchBox.add(wordList.get(i + start).getNormalForms());
        }
        for (Lemma lemma : lemmaListFromQuery) {
            if (!wordInSearchBox.contains(lemma.getLemma())) {
                 return false;
            }
        }
        return true;
    }

    public int[] getQueryPositionInText() {
        ArrayList<Lemma> lemmaList = lemmaListFromQuery;
        for (int i = 0; i < lemmaListFromQuery.size(); i++) {
            int[] queryPosition = getQueryPositionInTextForLemmaList(lemmaList);
            if (queryPosition == null) {
                lemmaList.remove(lemmaList.size() - 1);
                continue;
            } else {
                return queryPosition;
            }

        }
        return null;
    }

    public int[] getSnippetPosition() {
        int[] queryPosition = getQueryPositionInText();
        if (queryPosition == null) {
            return new int[] {0, 20};
        }
        int searchBox = queryPosition[1] - queryPosition[0] + 1;
        if (queryPosition[0] <= 10 - (searchBox / 2)) {
            return new int[] {0, 20};
        }
        if (queryPosition[1] >= wordList.size() - 11 +(searchBox / 2)) {
            return new int[] {wordList.size() - 21, wordList.size() - 1};
        }
        return new int[] {queryPosition[0] - 10 + (searchBox / 2), queryPosition[1] + 10 - (searchBox / 2)};
    }

    public String getSnippet() {
        int[] snippetPosition = getSnippetPosition();
        StringBuilder snippet = new StringBuilder();
        for (int i = snippetPosition[0]; i <= snippetPosition[1]; i++) {
            snippet.append(wordList.get(i).getWord()).append(" ");
        }
        return snippet.toString();
    }
}
