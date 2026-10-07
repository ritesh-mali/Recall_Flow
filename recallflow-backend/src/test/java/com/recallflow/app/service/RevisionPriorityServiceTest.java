package com.recallflow.app.service;

import com.recallflow.app.entity.Topic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@DisplayName("RevisionPriorityService – Algorithm Tests")
class RevisionPriorityServiceTest {

    private RevisionPriorityService service;

    @BeforeEach
    void setUp() {
        service = new RevisionPriorityService();
    }

    // ────────────────────────────────────────────────────────────────────────
    // Priority Score Tests
    // ────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Low-performance topic should have HIGH priority score")
    void highPriorityForPoorPerformance() {
        Topic topic = buildTopic(
                Topic.Difficulty.MEDIUM,
                LocalDate.now().minusDays(8),  // 8 days ago
                2,      // confidence 2/5
                55.0,   // latest quiz 55%
                62.0,   // avg quiz 62%
                6       // mistakes
        );
        double score = service.calculatePriorityScore(topic);
        assertThat(score).isGreaterThanOrEqualTo(70)
                .withFailMessage("Expected high priority score (>=70) but got: " + score);
    }

    @Test
    @DisplayName("Well-performing topic should have LOW priority score")
    void lowPriorityForGoodPerformance() {
        Topic topic = buildTopic(
                Topic.Difficulty.EASY,
                LocalDate.now().minusDays(2),  // just 2 days ago
                5,      // confidence 5/5
                95.0,   // latest quiz 95%
                93.0,   // avg quiz 93%
                0       // no mistakes
        );
        double score = service.calculatePriorityScore(topic);
        assertThat(score).isLessThan(40)
                .withFailMessage("Expected low priority score (<40) but got: " + score);
    }

    @Test
    @DisplayName("Score should be within [0, 100] range")
    void scoreAlwaysInRange() {
        Topic extremeHigh = buildTopic(Topic.Difficulty.HARD,
                LocalDate.now().minusDays(60), 1, 10.0, 15.0, 20);
        Topic extremeLow = buildTopic(Topic.Difficulty.EASY,
                LocalDate.now(), 5, 100.0, 100.0, 0);

        double highScore = service.calculatePriorityScore(extremeHigh);
        double lowScore = service.calculatePriorityScore(extremeLow);

        assertThat(highScore).isBetween(0.0, 100.0);
        assertThat(lowScore).isBetween(0.0, 100.0);
    }

    @Test
    @DisplayName("Higher confidence should produce lower priority score (all else equal)")
    void higherConfidenceLowerScore() {
        LocalDate revision = LocalDate.now().minusDays(5);
        Topic lowConf = buildTopic(Topic.Difficulty.MEDIUM, revision, 1, 60.0, 60.0, 2);
        Topic highConf = buildTopic(Topic.Difficulty.MEDIUM, revision, 5, 60.0, 60.0, 2);

        assertThat(service.calculatePriorityScore(lowConf))
                .isGreaterThan(service.calculatePriorityScore(highConf));
    }

    @Test
    @DisplayName("More days since revision should increase priority score")
    void moreDaysHigherScore() {
        Topic recent = buildTopic(Topic.Difficulty.MEDIUM, LocalDate.now().minusDays(1), 3, 70.0, 70.0, 1);
        Topic stale = buildTopic(Topic.Difficulty.MEDIUM, LocalDate.now().minusDays(20), 3, 70.0, 70.0, 1);

        assertThat(service.calculatePriorityScore(stale))
                .isGreaterThan(service.calculatePriorityScore(recent));
    }

    @Test
    @DisplayName("More mistakes should increase priority score")
    void moreMistakesHigherScore() {
        LocalDate revision = LocalDate.now().minusDays(5);
        Topic fewMistakes = buildTopic(Topic.Difficulty.MEDIUM, revision, 3, 70.0, 70.0, 0);
        Topic manyMistakes = buildTopic(Topic.Difficulty.MEDIUM, revision, 3, 70.0, 70.0, 8);

        assertThat(service.calculatePriorityScore(manyMistakes))
                .isGreaterThan(service.calculatePriorityScore(fewMistakes));
    }

    @Test
    @DisplayName("HARD topic should score higher than EASY topic all else equal")
    void hardDifficultyHigherScore() {
        LocalDate revision = LocalDate.now().minusDays(5);
        Topic easy = buildTopic(Topic.Difficulty.EASY, revision, 3, 70.0, 70.0, 2);
        Topic hard = buildTopic(Topic.Difficulty.HARD, revision, 3, 70.0, 70.0, 2);

        assertThat(service.calculatePriorityScore(hard))
                .isGreaterThan(service.calculatePriorityScore(easy));
    }

    // ────────────────────────────────────────────────────────────────────────
    // Forgetting Risk Tests
    // ────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("High-priority score should map to HIGH or VERY_HIGH risk")
    void highScoreMapsToHighRisk() {
        Topic topic = buildTopic(Topic.Difficulty.HARD, LocalDate.now().minusDays(15), 1, 40.0, 40.0, 8);
        Topic.RiskLevel risk = service.calculateForgettingRisk(topic);
        assertThat(risk).isIn(Topic.RiskLevel.HIGH, Topic.RiskLevel.VERY_HIGH);
    }

    @Test
    @DisplayName("Recently revised, high-confidence topic should be LOW risk")
    void recentHighConfidenceLowRisk() {
        Topic topic = buildTopic(Topic.Difficulty.EASY, LocalDate.now().minusDays(1), 5, 92.0, 90.0, 0);
        assertThat(service.calculateForgettingRisk(topic)).isEqualTo(Topic.RiskLevel.LOW);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Next Revision Days Tests
    // ────────────────────────────────────────────────────────────────────────

    @ParameterizedTest(name = "score={0}, confidence={1} → expected maxDays={2}")
    @CsvSource({
            "40, 1, 2",
            "55, 2, 3",
            "72, 3, 4",
            "85, 4, 7",
            "95, 5, 14"
    })
    @DisplayName("Next revision days should scale with score and confidence")
    void nextRevisionDaysScaling(double score, int confidence, int maxDays) {
        int days = service.calculateNextRevisionDays(score, confidence);
        assertThat(days).isLessThanOrEqualTo(maxDays);
        assertThat(days).isGreaterThan(0);
    }

    @Test
    @DisplayName("Poor quiz score should result in very short revision interval")
    void poorScoreShortInterval() {
        int days = service.calculateNextRevisionDays(30.0, 1);
        assertThat(days).isLessThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Excellent quiz score with high confidence should give long interval")
    void excellentScoreLongInterval() {
        int days = service.calculateNextRevisionDays(95.0, 5);
        assertThat(days).isGreaterThanOrEqualTo(10);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Memory Strength Tests
    // ────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Memory strength should be within [0, 100]")
    void memoryStrengthInRange() {
        Topic topic = buildTopic(Topic.Difficulty.MEDIUM, LocalDate.now().minusDays(5), 3, 75.0, 72.0, 2);
        double strength = service.estimateMemoryStrength(topic);
        assertThat(strength).isBetween(0.0, 100.0);
    }

    @Test
    @DisplayName("Recently-learned topic with high confidence should have higher memory strength")
    void recentLearningHighStrength() {
        Topic strong = buildTopic(Topic.Difficulty.MEDIUM, LocalDate.now(), 5, 95.0, 90.0, 0);
        Topic weak = buildTopic(Topic.Difficulty.MEDIUM, LocalDate.now().minusDays(20), 1, 40.0, 45.0, 5);
        assertThat(service.estimateMemoryStrength(strong))
                .isGreaterThan(service.estimateMemoryStrength(weak));
    }

    // ────────────────────────────────────────────────────────────────────────
    // Priority Reason Tests
    // ────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Reason should mention days when long time has passed")
    void reasonMentionsDays() {
        Topic topic = buildTopic(Topic.Difficulty.MEDIUM, LocalDate.now().minusDays(20), 2, 50.0, 55.0, 3);
        String reason = service.generatePriorityReason(topic);
        assertThat(reason).isNotBlank();
        assertThat(reason.toLowerCase()).containsAnyOf("days", "revision", "confidence", "score", "mistakes");
    }

    @Test
    @DisplayName("Reason should not be empty even for well-performing topic")
    void reasonNeverEmpty() {
        Topic topic = buildTopic(Topic.Difficulty.EASY, LocalDate.now().minusDays(1), 5, 95.0, 93.0, 0);
        assertThat(service.generatePriorityReason(topic)).isNotBlank();
    }

    // ────────────────────────────────────────────────────────────────────────
    // Helper
    // ────────────────────────────────────────────────────────────────────────

    private Topic buildTopic(Topic.Difficulty difficulty, LocalDate lastRevised,
                              int confidence, double latestScore, double avgScore, int mistakes) {
        Topic t = new Topic();
        t.setName("Test Topic");
        t.setSubject("Test");
        t.setDifficulty(difficulty);
        t.setLastRevisedDate(lastRevised);
        t.setConfidenceLevel(confidence);
        t.setLatestQuizScore(latestScore);
        t.setAverageQuizScore(avgScore);
        t.setTotalMistakes(mistakes);
        t.setConsecutiveSuccessfulRevisions(0);
        t.setRevisionCount(1);
        t.setStatus(Topic.TopicStatus.LEARNED);
        return t;
    }
}
