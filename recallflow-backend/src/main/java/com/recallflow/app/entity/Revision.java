package com.recallflow.app.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "revisions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Revision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "revision_date", nullable = false)
    private LocalDate revisionDate;

    @Column(name = "confidence_before")
    private Integer confidenceBefore;

    @Column(name = "confidence_after")
    private Integer confidenceAfter;

    @Column(name = "quiz_score")
    private Double quizScore;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "was_successful")
    @Builder.Default
    private Boolean wasSuccessful = false;

    @Column(name = "next_revision_days")
    private Integer nextRevisionDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_at_revision")
    private Topic.RiskLevel riskAtRevision;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
