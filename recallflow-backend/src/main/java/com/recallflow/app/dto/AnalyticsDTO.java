package com.recallflow.app.dto;

import com.recallflow.app.entity.Topic;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class AnalyticsDTO {

    @Data
    public static class DashboardStats {
        private long totalTopics;
        private long learnedTopics;
        private long revisedTopics;
        private long atRiskTopics;
        private long highRiskTopics;
        private long mediumRiskTopics;
        private long lowRiskTopics;
        private Double averageQuizScore;
        private int currentStreak;
        private int longestStreak;
        private long totalRevisions;
        private long totalMistakes;
        private long totalQuizAttempts;
        private long todayRevisionCount;
    }

    @Data
    public static class SubjectPerformance {
        private String subject;
        private double averageScore;
        private int topicCount;
        private int revisedCount;
        private long mistakeCount;
    }

    @Data
    public static class WeakTopic {
        private Long topicId;
        private String topicName;
        private String subject;
        private Double averageAccuracy;
        private Integer mistakeCount;
        private Integer confidenceLevel;
        private Double priorityScore;
        private Topic.RiskLevel riskLevel;
        private String weaknessReason;
    }

    @Data
    public static class Recommendation {
        private Long topicId;
        private String topicName;
        private String subject;
        private String message;
        private String type;  // REVISE_NOW, PERFORMING_WELL, INTERVAL_EXTENDED, NEEDS_ATTENTION
        private Double priorityScore;
        private Topic.RiskLevel riskLevel;
    }

    @Data
    public static class QuizPerformancePoint {
        private LocalDate date;
        private Double averageScore;
        private Integer attemptCount;
    }

    @Data
    public static class RevisionActivityPoint {
        private LocalDate date;
        private Integer revisionCount;
    }

    @Data
    public static class FullAnalytics {
        private DashboardStats stats;
        private List<SubjectPerformance> subjectPerformances;
        private List<WeakTopic> weakTopics;
        private List<Recommendation> recommendations;
        private List<QuizPerformancePoint> quizPerformanceOverTime;
        private List<RevisionActivityPoint> revisionActivity;
        private Map<String, Long> riskDistribution;
        private Map<String, Double> confidenceVsQuizScore;
    }

    @Data
    public static class MistakeResponse {
        private Long id;
        private Long topicId;
        private String topicName;
        private String subject;
        private String questionText;
        private String userAnswer;
        private String correctAnswer;
        private String explanation;
        private java.time.LocalDateTime mistakeDate;
        private Integer attemptNumber;
        private Boolean reviewed;
    }

    @Data
    public static class MistakeSummary {
        private Long topicId;
        private String topicName;
        private String subject;
        private Long mistakeCount;
        private Double accuracyRate;
    }
}
