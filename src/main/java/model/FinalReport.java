package model;

import java.util.List;
import java.util.Map;

public record FinalReport(
        String taskId,
        int totalWordCount,
        int sentiment,
        Map<String, Integer> globalTop,
        String replacedText,
        List<String> sortedSentences
) {}
