package com.recallflow.app.service;

import com.recallflow.app.dto.AnalyticsDTO;
import com.recallflow.app.entity.*;
import com.recallflow.app.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final TopicRepository topicRepository;
    private final RevisionRepository revisionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final MistakeRepository mistakeRepository;
    private final RevisionPriorityService priorityService;
    private final AuthService authService;

    @Transactional(readOnly = true)
    public AnalyticsDTO.DashboardStats getDashboardStats() {
        User user = authService.getCurrentUser();
        Long userId = user.getId();

        AnalyticsDTO.DashboardStats stats = new AnalyticsDTO.DashboardStats();
        stats.setTotalTopics(topicRepository.countByUserId(userId));
        stats.setLearnedTopics(topicRepository.countByUserIdAndStatus(userId, Topic.TopicStatus.LEARNED)
                + topicRepository.countByUserIdAndStatus(userId, Topic.TopicStatus.UNDER_REVISION));
        stats.setRevisedTopics(topicRepository.countByUserIdAndStatus(userId, Topic.TopicStatus.UNDER_REVISION));

        List<Topic> allTopics = topicRepository.findByUserId(userId);
        long high = allTopics.stream().filter(t -> t.getForgettingRisk() == Topic.RiskLevel.HIGH
                || t.getForgettingRisk() == Topic.RiskLevel.VERY_HIGH).count();
        long medium = allTopics.stream().filter(t -> t.getForgettingRisk() == Topic.RiskLevel.MEDIUM).count();
        long low = allTopics.stream().filter(t -> t.getForgettingRisk() == Topic.RiskLevel.LOW).count();
        stats.setAtRiskTopics(high + medium);
        stats.setHighRiskTopics(high);
        stats.setMediumRiskTopics(medium);
        stats.setLowRiskTopics(low);

        Double avgScore = quizAttemptRepository.findOverallAverageScoreByUserId(userId);
        stats.setAverageQuizScore(avgScore != null ? Math.round(avgScore * 10.0) / 10.0 : 0.0);
        stats.setCurrentStreak(user.getCurrentStreak());
        stats.setLongestStreak(user.getLongestStreak());
        stats.setTotalRevisions(revisionRepository.countRevisionsSince(userId, LocalDate.of(2000, 1, 1)));
        stats.setTotalMistakes(mistakeRepository.countByUserId(userId));
        stats.setTotalQuizAttempts(quizAttemptRepository.countByUserId(userId));
        stats.setTodayRevisionCount(revisionRepository.countByUserIdAndRevisionDate(userId, LocalDate.now()));

        return stats;
    }

    @Transactional(readOnly = true)
    public List<AnalyticsDTO.SubjectPerformance> getSubjectPerformance() {
        User user = authService.getCurrentUser();
        List<String> subjects = topicRepository.findDistinctSubjectsByUserId(user.getId());

        return subjects.stream().map(subject -> {
            List<Topic> topics = topicRepository.findByUserIdAndSubject(user.getId(), subject);
            AnalyticsDTO.SubjectPerformance perf = new AnalyticsDTO.SubjectPerformance();
            perf.setSubject(subject);
            perf.setTopicCount(topics.size());
            perf.setRevisedCount((int) topics.stream()
                    .filter(t -> t.getStatus() == Topic.TopicStatus.UNDER_REVISION).count());

            OptionalDouble avg = topics.stream()
                    .filter(t -> t.getAverageQuizScore() != null)
                    .mapToDouble(Topic::getAverageQuizScore)
                    .average();
            perf.setAverageScore(avg.isPresent() ? Math.round(avg.getAsDouble() * 10.0) / 10.0 : 0.0);

            long mistakes = topics.stream().mapToLong(t ->
                    mistakeRepository.countByUserIdAndTopicId(user.getId(), t.getId())).sum();
            perf.setMistakeCount(mistakes);
            return perf;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AnalyticsDTO.WeakTopic> getWeakTopics() {
        User user = authService.getCurrentUser();
        List<Topic> weakTopics = topicRepository.findWeakTopics(user.getId());

        return weakTopics.stream().map(topic -> {
            AnalyticsDTO.WeakTopic wt = new AnalyticsDTO.WeakTopic();
            wt.setTopicId(topic.getId());
            wt.setTopicName(topic.getName());
            wt.setSubject(topic.getSubject());
            wt.setAverageAccuracy(topic.getAverageQuizScore() != null ? topic.getAverageQuizScore() : 0.0);
            wt.setMistakeCount(topic.getTotalMistakes());
            wt.setConfidenceLevel(topic.getConfidenceLevel());
            wt.setPriorityScore(topic.getRevisionPriorityScore());
            wt.setRiskLevel(topic.getForgettingRisk());
            wt.setWeaknessReason(buildWeaknessReason(topic));
            return wt;
        }).limit(10).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AnalyticsDTO.Recommendation> getRecommendations() {
        User user = authService.getCurrentUser();
        List<Topic> topics = topicRepository.findTopicsByUserIdOrderByPriority(user.getId());
        List<AnalyticsDTO.Recommendation> recommendations = new ArrayList<>();

        for (Topic topic : topics) {
            if (topic.getStatus() == Topic.TopicStatus.NOT_STARTED) continue;
            if (recommendations.size() >= 8) break;

            AnalyticsDTO.Recommendation rec = new AnalyticsDTO.Recommendation();
            rec.setTopicId(topic.getId());
            rec.setTopicName(topic.getName());
            rec.setSubject(topic.getSubject());
            rec.setPriorityScore(topic.getRevisionPriorityScore());
            rec.setRiskLevel(topic.getForgettingRisk());

            if (topic.getRevisionPriorityScore() >= 70) {
                rec.setType("REVISE_NOW");
                rec.setMessage(buildReviseNowMessage(topic));
            } else if (topic.getConsecutiveSuccessfulRevisions() >= 3) {
                rec.setType("PERFORMING_WELL");
                rec.setMessage(buildPerformingWellMessage(topic));
            } else if (topic.getTotalMistakes() >= 3) {
                rec.setType("NEEDS_ATTENTION");
                rec.setMessage(buildNeedsAttentionMessage(topic));
            } else {
                rec.setType("SCHEDULED");
                rec.setMessage("Scheduled revision for " + topic.getName() + ".");
            }
            recommendations.add(rec);
        }
        return recommendations;
    }

    @Transactional(readOnly = true)
    public List<AnalyticsDTO.QuizPerformancePoint> getQuizPerformanceOverTime(int days) {
        User user = authService.getCurrentUser();
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<QuizAttempt> attempts = quizAttemptRepository.findRecentAttempts(user.getId(), since);

        // Group by date
        Map<LocalDate, List<QuizAttempt>> byDate = attempts.stream()
                .collect(Collectors.groupingBy(a -> a.getAttemptDate().toLocalDate()));

        return byDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    AnalyticsDTO.QuizPerformancePoint p = new AnalyticsDTO.QuizPerformancePoint();
                    p.setDate(e.getKey());
                    p.setAttemptCount(e.getValue().size());
                    OptionalDouble avg = e.getValue().stream()
                            .mapToDouble(QuizAttempt::getScorePercentage).average();
                    p.setAverageScore(avg.isPresent() ? Math.round(avg.getAsDouble() * 10.0) / 10.0 : 0.0);
                    return p;
                }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AnalyticsDTO.MistakeResponse> getMistakes() {
        User user = authService.getCurrentUser();
        return mistakeRepository.findByUserIdOrderByMistakeDateDesc(user.getId())
                .stream()
                .map(this::mapMistakeToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AnalyticsDTO.MistakeSummary> getMistakeSummary() {
        User user = authService.getCurrentUser();
        List<Object[]> rows = mistakeRepository.findMistakeCountGroupedByTopic(user.getId());

        return rows.stream().map(row -> {
            Long topicId = (Long) row[0];
            Long count = (Long) row[1];
            Topic topic = topicRepository.findById(topicId).orElse(null);

            AnalyticsDTO.MistakeSummary s = new AnalyticsDTO.MistakeSummary();
            s.setTopicId(topicId);
            s.setMistakeCount(count);
            if (topic != null) {
                s.setTopicName(topic.getName());
                s.setSubject(topic.getSubject());
                Double avg = topic.getAverageQuizScore();
                s.setAccuracyRate(avg != null ? avg : 0.0);
            }
            return s;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AnalyticsDTO.RevisionActivityPoint> getRevisionActivity(int days) {
        User user = authService.getCurrentUser();
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days);
        List<Revision> revisions = revisionRepository
                .findByUserIdAndRevisionDateBetweenOrderByRevisionDateAsc(user.getId(), start, end);

        Map<LocalDate, Long> byDate = revisions.stream()
                .collect(Collectors.groupingBy(Revision::getRevisionDate, Collectors.counting()));

        List<AnalyticsDTO.RevisionActivityPoint> result = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            AnalyticsDTO.RevisionActivityPoint p = new AnalyticsDTO.RevisionActivityPoint();
            p.setDate(date);
            p.setRevisionCount(byDate.getOrDefault(date, 0L).intValue());
            result.add(p);
        }
        return result;
    }

    // ────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ────────────────────────────────────────────────────────────────────────

    private String buildWeaknessReason(Topic topic) {
        List<String> reasons = new ArrayList<>();
        if (topic.getAverageQuizScore() != null && topic.getAverageQuizScore() < 60)
            reasons.add(String.format("Low avg score (%.0f%%)", topic.getAverageQuizScore()));
        if (topic.getTotalMistakes() >= 5)
            reasons.add(topic.getTotalMistakes() + " mistakes");
        if (topic.getConfidenceLevel() != null && topic.getConfidenceLevel() <= 2)
            reasons.add("Low confidence (" + topic.getConfidenceLevel() + "/5)");
        return reasons.isEmpty() ? "Needs improvement" : String.join(", ", reasons);
    }

    private String buildReviseNowMessage(Topic topic) {
        if (topic.getLatestQuizScore() != null && topic.getAverageQuizScore() != null
                && topic.getLatestQuizScore() < topic.getAverageQuizScore() - 10) {
            return String.format("You should revise %s today. Your score dropped from %.0f%% to %.0f%% and you made %d mistakes in your last attempt.",
                    topic.getName(), topic.getAverageQuizScore(), topic.getLatestQuizScore(), topic.getTotalMistakes());
        }
        return "You should revise " + topic.getName() + " today. " + priorityService.generatePriorityReason(topic);
    }

    private String buildPerformingWellMessage(Topic topic) {
        return String.format("You are consistently performing well in %s. Your last %d revisions were successful. The next revision interval has been extended.",
                topic.getName(), topic.getConsecutiveSuccessfulRevisions());
    }

    private String buildNeedsAttentionMessage(Topic topic) {
        return String.format("%s needs attention. You have %d recorded mistakes. Consider practicing more questions on this topic.",
                topic.getName(), topic.getTotalMistakes());
    }

    private AnalyticsDTO.MistakeResponse mapMistakeToResponse(Mistake m) {
        AnalyticsDTO.MistakeResponse r = new AnalyticsDTO.MistakeResponse();
        r.setId(m.getId());
        r.setTopicId(m.getTopic().getId());
        r.setTopicName(m.getTopic().getName());
        r.setSubject(m.getTopic().getSubject());
        r.setQuestionText(m.getQuestion().getQuestionText());
        r.setUserAnswer(m.getUserAnswer());
        r.setCorrectAnswer(m.getCorrectAnswer());
        r.setExplanation(m.getQuestion().getExplanation());
        r.setMistakeDate(m.getMistakeDate());
        r.setAttemptNumber(m.getAttemptNumber());
        r.setReviewed(m.getReviewed());
        return r;
    }
}
