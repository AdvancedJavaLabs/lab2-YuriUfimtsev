package model;

import java.io.Serializable;

public record TaskMessage(
        String taskId,
        int sectionId,
        int totalSections,
        String text
) implements Serializable {}