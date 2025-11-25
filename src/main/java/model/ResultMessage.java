package model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public record ResultMessage(
        String taskId,
        int sectionId,
        int totalSections,
        int wordCount,
        Map<String, Integer> topN,
        int sentiment,
        String replacedText,
        List<String> sortedSentences
) implements Serializable {}
