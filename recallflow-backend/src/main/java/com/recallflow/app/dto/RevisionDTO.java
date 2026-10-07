package com.recallflow.app.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class RevisionDTO {

    @Data
    public static class CompleteRequest {
        @NotNull(message = "Confidence level is required")
        @Min(1) @Max(5)
        private Integer confidenceAfter;

        private String notes;
        private Integer durationMinutes;
    }

    @Data
    public static class Response {
        private Long id;
        private Long topicId;
        private String topicName;
        private LocalDate revisionDate;
        private Integer confidenceBefore;
        private Integer confidenceAfter;
        private Double quizScore;
        private Integer durationMinutes;
        private Boolean wasSuccessful;
        private Integer nextRevisionDays;
        private String riskAtRevision;
        private String notes;
        private LocalDateTime createdAt;
    }

    @Data
    public static class SessionInfo {
        private Long topicId;
        private String topicName;
        private String subject;
        private String difficulty;
        private LocalDate lastRevisedDate;
        private Long daysSinceRevision;
        private Integer previousConfidence;
        private Double previousQuizScore;
        private Double averageQuizScore;
        private String currentRisk;
        private Double priorityScore;
        private Integer revisionCount;
        private Integer totalMistakes;
        private Double memoryStrength;
    }
}
