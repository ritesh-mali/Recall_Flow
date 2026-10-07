package com.recallflow.app.dto;

import com.recallflow.app.entity.Topic;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class TopicDTO {

    @Data
    public static class CreateRequest {
        @NotBlank(message = "Topic name is required")
        @Size(max = 100)
        private String name;

        @NotBlank(message = "Subject is required")
        @Size(max = 100)
        private String subject;

        @NotNull(message = "Difficulty is required")
        private Topic.Difficulty difficulty;

        @Size(max = 500)
        private String description;

        @Size(max = 255)
        private String tags;
    }

    @Data
    public static class UpdateRequest {
        @Size(max = 100)
        private String name;

        @Size(max = 100)
        private String subject;

        private Topic.Difficulty difficulty;

        @Size(max = 500)
        private String description;

        @Size(max = 255)
        private String tags;
    }

    @Data
    public static class MarkLearnedRequest {
        @NotNull(message = "Confidence level is required")
        @Min(1) @Max(5)
        private Integer confidenceLevel;

        private String notes;
    }

    @Data
    public static class Response {
        private Long id;
        private String name;
        private String subject;
        private Topic.Difficulty difficulty;
        private Topic.TopicStatus status;
        private LocalDate dateLearned;
        private LocalDate lastRevisedDate;
        private LocalDate nextRevisionDate;
        private Integer revisionCount;
        private Integer confidenceLevel;
        private Double latestQuizScore;
        private Double averageQuizScore;
        private Integer totalMistakes;
        private Integer consecutiveSuccessfulRevisions;
        private Topic.RiskLevel forgettingRisk;
        private Double revisionPriorityScore;
        private Double memoryStrength;
        private String priorityReason;
        private String description;
        private String tags;
        private Long questionCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        // Days since last revision (computed)
        private Long daysSinceRevision;
    }

    @Data
    public static class TodayRevisionItem {
        private Long topicId;
        private String topicName;
        private String subject;
        private Topic.Difficulty difficulty;
        private Double priorityScore;
        private Topic.RiskLevel riskLevel;
        private String reason;
        private LocalDate lastRevisedDate;
        private Long daysSince;
        private Integer confidenceLevel;
        private Double latestQuizScore;
    }
}
