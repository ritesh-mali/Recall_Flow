package com.recallflow.app.service;

import com.recallflow.app.entity.Topic;
import com.recallflow.app.entity.Topic.Difficulty;
import com.recallflow.app.entity.Topic.RiskLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * RevisionPriorityService – The Heart of RecallFlow
 *
 * <p>This service implements the Revision Priority Engine that determines which
 * topics a user should revise based on multiple cognitive factors.</p>
 *
 * <p>Algorithm Overview:</p>
 * <pre>
 *   Score = w1*TimeFactor + w2*ConfidenceFactor + w3*QuizFactor
 *         + w4*MistakeFactor + w5*DifficultyFactor + w6*IntervalExceededFactor
 *
 *   Final score normalized to [0, 100]
 * </pre>
 *
 * <p>Weights (must sum to ~100 when multiplied by max factor value):</p>
 * <ul>
 *   <li>Time since revision: 30%</li>
 *   <li>Confidence level:    20%</li>
 *   <li>Quiz performance:    25%</li>
 *   <li>Mistake count:       15%</li>
 *   <li>Topic difficulty:    10%</li>
 * </ul>
 */
@Service
@Slf4j
public class RevisionPriorityService {

    // --- Weight Constants ---
    private static final double WEIGHT_TIME = 0.30;
    private static final double WEIGHT_CONFIDENCE = 0.20;
    private static final double WEIGHT_QUIZ = 0.25;
    private static final double WEIGHT_MISTAKES = 0.15;
    private static final double WEIGHT_DIFFICULTY = 0.10;

    // --- Thresholds ---
    private static final int MAX_DAYS_REFERENCE = 30;   // 30+ days → max time factor
    private static final int MAX_MISTAKES_REFERENCE = 10; // 10+ mistakes → max mistake factor

    /**
     * Calculates the Revision Priority Score for a topic (0–100).
     * Higher score means the topic needs revision more urgently.
     *
     * @param topic the topic to score
     * @return priority score between 0 and 100
     */
    public double calculatePriorityScore(Topic topic) {
        double timeFactor = calculateTimeFactor(topic);
        double confidenceFactor = calculateConfidenceFactor(topic);
        double quizFactor = calculateQuizFactor(topic);
        double mistakeFactor = calculateMistakeFactor(topic);
        double difficultyFactor = calculateDifficultyFactor(topic);

        double rawScore = (WEIGHT_TIME * timeFactor
                + WEIGHT_CONFIDENCE * confidenceFactor
                + WEIGHT_QUIZ * quizFactor
                + WEIGHT_MISTAKES * mistakeFactor
                + WEIGHT_DIFFICULTY * difficultyFactor) * 100.0;

        // Apply interval-exceeded bonus
        rawScore += calculateIntervalExceededBonus(topic);

        // Clamp to [0, 100]
        double finalScore = Math.min(100.0, Math.max(0.0, rawScore));

        log.debug("Topic '{}' → time={:.2f}, confidence={:.2f}, quiz={:.2f}, mistakes={:.2f}, difficulty={:.2f} → score={:.1f}",
                topic.getName(), timeFactor, confidenceFactor, quizFactor, mistakeFactor, difficultyFactor, finalScore);

        return Math.round(finalScore * 10.0) / 10.0;
    }

    /**
     * Calculates the forgetting risk level.
     * Considers time, confidence, and quiz performance together.
     */
    public RiskLevel calculateForgettingRisk(Topic topic) {
        double score = calculatePriorityScore(topic);

        if (score >= 75) return RiskLevel.VERY_HIGH;
        if (score >= 55) return RiskLevel.HIGH;
        if (score >= 35) return RiskLevel.MEDIUM;
        return RiskLevel.LOW;
    }

    /**
     * Calculates the next recommended revision interval in days.
     *
     * <p>Based on a modified Leitner/SM-2 inspired algorithm:</p>
     * <ul>
     *   <li>Score &lt; 50% + Confidence ≤ 2 → 1 day</li>
     *   <li>Score 50–69% + Confidence 2–3 → 2 days</li>
     *   <li>Score 70–79% + Confidence 3     → 4 days</li>
     *   <li>Score 80–89% + Confidence 4     → 7 days</li>
     *   <li>Score 90–100% + Confidence 4–5  → 14 days</li>
     * </ul>
     */
    public int calculateNextRevisionDays(double quizScore, int confidence) {
        if (quizScore < 50 || confidence <= 2) {
            return 1;
        } else if (quizScore < 60 || confidence <= 2) {
            return 2;
        } else if (quizScore < 70 || confidence == 3) {
            return 3;
        } else if (quizScore < 80 || confidence == 3) {
            return 4;
        } else if (quizScore < 90 || confidence == 4) {
            return 7;
        } else {
            return 14;
        }
    }

    /**
     * Estimates memory strength as a percentage (0–100).
     * Higher = better memory retention.
     *
     * <p>Memory Strength = BaseStrength - TimeDecay</p>
     * <p>BaseStrength comes from confidence, quiz score, and successive revisions.</p>
     */
    public double estimateMemoryStrength(Topic topic) {
        double baseStrength = 0.0;

        // Confidence contribution (0–40 points)
        baseStrength += (topic.getConfidenceLevel() / 5.0) * 40.0;

        // Quiz performance contribution (0–35 points)
        if (topic.getAverageQuizScore() != null) {
            baseStrength += (topic.getAverageQuizScore() / 100.0) * 35.0;
        } else if (topic.getLatestQuizScore() != null) {
            baseStrength += (topic.getLatestQuizScore() / 100.0) * 25.0;
        }

        // Consecutive successes boost (0–10 points)
        int consecutiveSuccesses = Math.min(topic.getConsecutiveSuccessfulRevisions(), 5);
        baseStrength += consecutiveSuccesses * 2.0;

        // Mistakes penalty (-0 to -15 points)
        int mistakes = Math.min(topic.getTotalMistakes(), 10);
        baseStrength -= mistakes * 1.5;

        // Time decay (exponential forgetting curve approximation)
        double timeDecay = 0.0;
        if (topic.getLastRevisedDate() != null) {
            long daysSince = ChronoUnit.DAYS.between(topic.getLastRevisedDate(), LocalDate.now());
            // Ebbinghaus forgetting curve approximation: decay = days^0.5 * 3
            timeDecay = Math.min(50.0, Math.pow(daysSince, 0.6) * 3.0);
        } else if (topic.getDateLearned() != null) {
            long daysSince = ChronoUnit.DAYS.between(topic.getDateLearned(), LocalDate.now());
            timeDecay = Math.min(60.0, Math.pow(daysSince, 0.6) * 4.0);
        } else {
            timeDecay = 40.0; // unknown — assume significant decay
        }

        double strength = baseStrength - timeDecay;
        return Math.min(100.0, Math.max(0.0, strength));
    }

    /**
     * Generates a human-readable reason string for why the topic has this priority.
     */
    public String generatePriorityReason(Topic topic) {
        StringBuilder reasons = new StringBuilder();

        long daysSince = getDaysSinceRevision(topic);
        if (daysSince >= 14) {
            reasons.append("Not revised in ").append(daysSince).append(" days. ");
        } else if (daysSince >= 7) {
            reasons.append("7+ days since last revision. ");
        }

        if (topic.getConfidenceLevel() != null && topic.getConfidenceLevel() <= 2) {
            reasons.append("Low confidence (").append(topic.getConfidenceLevel()).append("/5). ");
        }

        if (topic.getLatestQuizScore() != null && topic.getLatestQuizScore() < 60) {
            reasons.append(String.format("Poor quiz score (%.0f%%). ", topic.getLatestQuizScore()));
        }

        if (topic.getTotalMistakes() != null && topic.getTotalMistakes() >= 4) {
            reasons.append(topic.getTotalMistakes()).append(" recorded mistakes. ");
        }

        if (topic.getDifficulty() == Difficulty.HARD) {
            reasons.append("Hard difficulty topic. ");
        }

        if (isIntervalExceeded(topic)) {
            reasons.append("Revision interval exceeded. ");
        }

        return reasons.length() > 0 ? reasons.toString().trim() : "Scheduled revision due.";
    }

    // ────────────────────────────────────────────────────────────────────────
    // Private factor calculations
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Time factor (0–1): increases as more time passes without revision.
     * Uses a square-root curve to model the Ebbinghaus forgetting curve shape.
     */
    private double calculateTimeFactor(Topic topic) {
        long daysSince = getDaysSinceRevision(topic);

        if (daysSince <= 0) return 0.0;

        // Square-root growth curve — fast initial rise, then slower growth
        // Normalized so MAX_DAYS_REFERENCE maps to 1.0
        double factor = Math.sqrt((double) daysSince / MAX_DAYS_REFERENCE);
        return Math.min(1.0, factor);
    }

    /**
     * Confidence factor (0–1): lower confidence → higher factor.
     * Confidence is 1–5; we invert so 1=max factor, 5=min factor.
     */
    private double calculateConfidenceFactor(Topic topic) {
        int confidence = topic.getConfidenceLevel() != null ? topic.getConfidenceLevel() : 0;
        if (confidence <= 0) return 1.0; // unknown → treat as max
        // Invert: confidence 1→1.0, 2→0.75, 3→0.5, 4→0.25, 5→0.0
        return (5 - confidence) / 4.0;
    }

    /**
     * Quiz factor (0–1): lower quiz score → higher factor.
     * Uses both latest and average score for a blended view.
     */
    private double calculateQuizFactor(Topic topic) {
        Double latestScore = topic.getLatestQuizScore();
        Double avgScore = topic.getAverageQuizScore();

        if (latestScore == null && avgScore == null) {
            // Never quizzed → treat as low performance
            return 0.7;
        }

        double effectiveScore;
        if (latestScore != null && avgScore != null) {
            // Weight latest score more heavily (60/40)
            effectiveScore = 0.6 * latestScore + 0.4 * avgScore;
        } else if (latestScore != null) {
            effectiveScore = latestScore;
        } else {
            effectiveScore = avgScore;
        }

        // Invert: 0% → 1.0, 100% → 0.0
        return 1.0 - (effectiveScore / 100.0);
    }

    /**
     * Mistake factor (0–1): more mistakes → higher factor.
     */
    private double calculateMistakeFactor(Topic topic) {
        int mistakes = topic.getTotalMistakes() != null ? topic.getTotalMistakes() : 0;
        return Math.min(1.0, (double) mistakes / MAX_MISTAKES_REFERENCE);
    }

    /**
     * Difficulty factor (0–1): harder topics get a higher base factor.
     * EASY=0.2, MEDIUM=0.5, HARD=1.0
     */
    private double calculateDifficultyFactor(Topic topic) {
        return switch (topic.getDifficulty()) {
            case EASY -> 0.2;
            case MEDIUM -> 0.5;
            case HARD -> 1.0;
        };
    }

    /**
     * Bonus points (0–10) added when the topic's recommended revision interval
     * has been exceeded. This creates urgency when the user is overdue.
     */
    private double calculateIntervalExceededBonus(Topic topic) {
        if (!isIntervalExceeded(topic)) return 0.0;

        if (topic.getNextRevisionDate() == null) return 0.0;

        long daysOverdue = ChronoUnit.DAYS.between(topic.getNextRevisionDate(), LocalDate.now());
        // Linear bonus up to 10 points for being 7+ days overdue
        return Math.min(10.0, daysOverdue * 1.5);
    }

    private boolean isIntervalExceeded(Topic topic) {
        return topic.getNextRevisionDate() != null
                && LocalDate.now().isAfter(topic.getNextRevisionDate());
    }

    private long getDaysSinceRevision(Topic topic) {
        LocalDate reference = topic.getLastRevisedDate() != null
                ? topic.getLastRevisedDate()
                : topic.getDateLearned();

        if (reference == null) return MAX_DAYS_REFERENCE; // unknown → treat as very old
        return Math.max(0, ChronoUnit.DAYS.between(reference, LocalDate.now()));
    }
}
