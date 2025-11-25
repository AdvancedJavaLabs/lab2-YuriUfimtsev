package processor;

import model.ResultMessage;
import model.TaskMessage;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class TextProcessor {
    private static final Pattern NAME_PATTERN =
            Pattern.compile("\\b[A-ZА-Я][a-zа-я]+\\b");

    public ResultMessage process(TaskMessage taskMessage, int topNCount, String replacement) {
        var text = taskMessage.text();

        var words = tokenize(text);
        var wordCount = words.size();

        var frequencies = countFrequencies(words);
        var topWords = topN(frequencies, topNCount);

        int sentiment = computeSentiment(words);

        var replaced = replaceNames(text, replacement);

        var sortedSentences = sortSentences(text);

        return new ResultMessage(
                taskMessage.taskId(),
                taskMessage.sectionId(),
                taskMessage.totalSections(),
                wordCount,
                topWords,
                sentiment,
                replaced,
                sortedSentences
        );

    }

    private List<String> tokenize(String text) {
        return Arrays.stream(text.toLowerCase().split("\\W+"))
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private int computeSentiment(List<String> words) {
        var score = 0;
        for (var w : words) {
            if (WordsInfo.POSITIVE.contains(w)) score++;
            else if (WordsInfo.NEGATIVE.contains(w)) score--;
        }
        return score;
    }

    private Map<String, Integer> countFrequencies(List<String> words) {
        Map<String, Integer> map = new HashMap<>();
        for (var word : words) {
            if (!map.containsKey(word)) {
                map.put(word, 1);
            } else {
                map.put(word, map.get(word) + 1);
            }
        }

        return map;
    }

    private Map<String, Integer> topN(Map<String, Integer> frequencies, int n) {
        return frequencies.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(n)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    private String replaceNames(String text, String replacement) {
        return NAME_PATTERN.matcher(text).replaceAll(replacement);
    }

    private List<String> sortSentences(String text) {
        return Arrays.stream(text.split("[.!?]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .sorted(Comparator.comparingInt(String::length))
                .toList();
    }
}
