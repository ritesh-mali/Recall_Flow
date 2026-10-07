package com.recallflow.app.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "topics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Difficulty difficulty = Difficulty.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private TopicStatus status = TopicStatus.NOT_STARTED;

    @Column(name = "date_learned")
    private LocalDate dateLearned;

    @Column(name = "last_revised_date")
    private LocalDate lastRevisedDate;

    @Column(name = "next_revision_date")
    private LocalDate nextRevisionDate;

    @Column(name = "revision_count")
    @Builder.Default
    private Integer revisionCount = 0;

    @Column(name = "confidence_level")
    @Builder.Default
    private Integer confidenceLevel = 0;

    @Column(name = "latest_quiz_score")
    private Double latestQuizScore;

    @Column(name = "average_quiz_score")
    private Double averageQuizScore;

    @Column(name = "total_mistakes")
    @Builder.Default
    private Integer totalMistakes = 0;

    @Column(name = "consecutive_successful_revisions")
    @Builder.Default
    private Integer consecutiveSuccessfulRevisions = 0;

    @Column(name = "forgetting_risk")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private RiskLevel forgettingRisk = RiskLevel.LOW;

    @Column(name = "revision_priority_score")
    @Builder.Default
    private Double revisionPriorityScore = 0.0;

    @Column(length = 500)
    private String description;

    @Column(name = "tags", length = 255)
    private String tags;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Revision> revisions = new ArrayList<>();

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Mistake> mistakes = new ArrayList<>();

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Question> questions = new ArrayList<>();

    public enum Difficulty {
        EASY, MEDIUM, HARD
    }

    public enum TopicStatus {
        NOT_STARTED, LEARNING, LEARNED, UNDER_REVISION
    }

    public enum RiskLevel {
        LOW, MEDIUM, HIGH, VERY_HIGH
    }
}
