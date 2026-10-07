package com.recallflow.app.service;

import com.recallflow.app.dto.RevisionDTO;
import com.recallflow.app.entity.Revision;
import com.recallflow.app.entity.Topic;
import com.recallflow.app.entity.User;
import com.recallflow.app.repository.RevisionRepository;
import com.recallflow.app.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RevisionService {

    private final RevisionRepository revisionRepository;
    private final TopicRepository topicRepository;
    private final RevisionPriorityService priorityService;
    private final AuthService authService;
    private final TopicService topicService;

    @Transactional(readOnly = true)
    public RevisionDTO.SessionInfo getRevisionSession(Long topicId) {
        User user = authService.getCurrentUser();
        Topic topic = topicService.findTopicForUser(topicId, user.getId());

        RevisionDTO.SessionInfo info = new RevisionDTO.SessionInfo();
        info.setTopicId(topic.getId());
        info.setTopicName(topic.getName());
        info.setSubject(topic.getSubject());
        info.setDifficulty(topic.getDifficulty().name());
        info.setLastRevisedDate(topic.getLastRevisedDate());
        info.setPreviousConfidence(topic.getConfidenceLevel());
        info.setPreviousQuizScore(topic.getLatestQuizScore());
        info.setAverageQuizScore(topic.getAverageQuizScore());
        info.setCurrentRisk(topic.getForgettingRisk().name());
        info.setPriorityScore(topic.getRevisionPriorityScore());
        info.setRevisionCount(topic.getRevisionCount());
        info.setTotalMistakes(topic.getTotalMistakes());
        info.setMemoryStrength(priorityService.estimateMemoryStrength(topic));

        if (topic.getLastRevisedDate() != null) {
            info.setDaysSinceRevision(ChronoUnit.DAYS.between(topic.getLastRevisedDate(), LocalDate.now()));
        } else if (topic.getDateLearned() != null) {
            info.setDaysSinceRevision(ChronoUnit.DAYS.between(topic.getDateLearned(), LocalDate.now()));
        }

        return info;
    }

    /**
     * Called after a revision session is completed (and optionally after a quiz).
     * Updates the topic's priority score, risk level, and schedules the next revision.
     */
    @Transactional
    public RevisionDTO.Response completeRevision(Long topicId, RevisionDTO.CompleteRequest request,
                                                  Double quizScore) {
        User user = authService.getCurrentUser();
        Topic topic = topicService.findTopicForUser(topicId, user.getId());

        int confidenceBefore = topic.getConfidenceLevel() != null ? topic.getConfidenceLevel() : 0;
        int confidenceAfter = request.getConfidenceAfter();

        // Calculate next revision days
        int nextDays = priorityService.calculateNextRevisionDays(
                quizScore != null ? quizScore : 50.0, confidenceAfter);

        // Was this revision successful?
        boolean wasSuccessful = quizScore != null ? quizScore >= 70 : confidenceAfter >= 3;

        // Create revision record
        Revision revision = Revision.builder()
                .topic(topic)
                .user(user)
                .revisionDate(LocalDate.now())
                .confidenceBefore(confidenceBefore)
                .confidenceAfter(confidenceAfter)
                .quizScore(quizScore)
                .durationMinutes(request.getDurationMinutes())
                .notes(request.getNotes())
                .wasSuccessful(wasSuccessful)
                .nextRevisionDays(nextDays)
                .riskAtRevision(topic.getForgettingRisk())
                .build();

        revisionRepository.save(revision);

        // Update topic fields
        topic.setLastRevisedDate(LocalDate.now());
        topic.setConfidenceLevel(confidenceAfter);
        topic.setRevisionCount(topic.getRevisionCount() + 1);
        topic.setNextRevisionDate(LocalDate.now().plusDays(nextDays));
        topic.setStatus(Topic.TopicStatus.UNDER_REVISION);

        if (quizScore != null) {
            topic.setLatestQuizScore(quizScore);
            // Update average
            Double avg = revisionRepository.findAverageQuizScoreForTopic(topic.getId());
            topic.setAverageQuizScore(avg != null ? avg : quizScore);
        }

        // Consecutive successes tracker
        if (wasSuccessful) {
            topic.setConsecutiveSuccessfulRevisions(topic.getConsecutiveSuccessfulRevisions() + 1);
        } else {
            topic.setConsecutiveSuccessfulRevisions(0);
        }

        // Recalculate priority and risk
        topic.setRevisionPriorityScore(priorityService.calculatePriorityScore(topic));
        topic.setForgettingRisk(priorityService.calculateForgettingRisk(topic));

        topicRepository.save(topic);

        log.info("Revision completed for topic '{}': score={}, nextRevision={} days",
                topic.getName(), quizScore, nextDays);

        // Map response
        RevisionDTO.Response response = new RevisionDTO.Response();
        response.setId(revision.getId());
        response.setTopicId(topic.getId());
        response.setTopicName(topic.getName());
        response.setRevisionDate(revision.getRevisionDate());
        response.setConfidenceBefore(confidenceBefore);
        response.setConfidenceAfter(confidenceAfter);
        response.setQuizScore(quizScore);
        response.setDurationMinutes(request.getDurationMinutes());
        response.setWasSuccessful(wasSuccessful);
        response.setNextRevisionDays(nextDays);
        response.setNotes(request.getNotes());
        response.setCreatedAt(revision.getCreatedAt());
        return response;
    }

    @Transactional(readOnly = true)
    public List<RevisionDTO.Response> getRevisionHistory(Long topicId) {
        User user = authService.getCurrentUser();
        topicService.findTopicForUser(topicId, user.getId()); // validates ownership
        return revisionRepository.findByTopicIdOrderByRevisionDateDesc(topicId)
                .stream()
                .map(this::mapRevisionResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RevisionDTO.Response> getAllRevisions() {
        User user = authService.getCurrentUser();
        return revisionRepository.findByUserIdOrderByRevisionDateDesc(user.getId())
                .stream()
                .map(this::mapRevisionResponse)
                .collect(Collectors.toList());
    }

    private RevisionDTO.Response mapRevisionResponse(Revision r) {
        RevisionDTO.Response resp = new RevisionDTO.Response();
        resp.setId(r.getId());
        resp.setTopicId(r.getTopic().getId());
        resp.setTopicName(r.getTopic().getName());
        resp.setRevisionDate(r.getRevisionDate());
        resp.setConfidenceBefore(r.getConfidenceBefore());
        resp.setConfidenceAfter(r.getConfidenceAfter());
        resp.setQuizScore(r.getQuizScore());
        resp.setDurationMinutes(r.getDurationMinutes());
        resp.setWasSuccessful(r.getWasSuccessful());
        resp.setNextRevisionDays(r.getNextRevisionDays());
        resp.setNotes(r.getNotes());
        resp.setCreatedAt(r.getCreatedAt());
        if (r.getRiskAtRevision() != null) {
            resp.setRiskAtRevision(r.getRiskAtRevision().name());
        }
        return resp;
    }
}
