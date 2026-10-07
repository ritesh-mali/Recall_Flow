package com.recallflow.app.service;

import com.recallflow.app.dto.TopicDTO;
import com.recallflow.app.entity.Topic;
import com.recallflow.app.entity.User;
import com.recallflow.app.exception.BadRequestException;
import com.recallflow.app.exception.ResourceNotFoundException;
import com.recallflow.app.repository.QuestionRepository;
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
public class TopicService {

    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;
    private final RevisionPriorityService priorityService;
    private final AuthService authService;

    @Transactional
    public TopicDTO.Response createTopic(TopicDTO.CreateRequest request) {
        User user = authService.getCurrentUser();

        Topic topic = Topic.builder()
                .name(request.getName())
                .subject(request.getSubject())
                .difficulty(request.getDifficulty())
                .description(request.getDescription())
                .tags(request.getTags())
                .status(Topic.TopicStatus.NOT_STARTED)
                .user(user)
                .build();

        topic = topicRepository.save(topic);
        log.info("Topic created: '{}' for user '{}'", topic.getName(), user.getUsername());
        return mapToResponse(topic);
    }

    @Transactional(readOnly = true)
    public List<TopicDTO.Response> getAllTopics() {
        User user = authService.getCurrentUser();
        return topicRepository.findByUserId(user.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TopicDTO.Response getTopicById(Long topicId) {
        User user = authService.getCurrentUser();
        Topic topic = findTopicForUser(topicId, user.getId());
        return mapToResponse(topic);
    }

    @Transactional
    public TopicDTO.Response updateTopic(Long topicId, TopicDTO.UpdateRequest request) {
        User user = authService.getCurrentUser();
        Topic topic = findTopicForUser(topicId, user.getId());

        if (request.getName() != null) topic.setName(request.getName());
        if (request.getSubject() != null) topic.setSubject(request.getSubject());
        if (request.getDifficulty() != null) topic.setDifficulty(request.getDifficulty());
        if (request.getDescription() != null) topic.setDescription(request.getDescription());
        if (request.getTags() != null) topic.setTags(request.getTags());

        topic = topicRepository.save(topic);
        return mapToResponse(topic);
    }

    @Transactional
    public void deleteTopic(Long topicId) {
        User user = authService.getCurrentUser();
        Topic topic = findTopicForUser(topicId, user.getId());
        topicRepository.delete(topic);
        log.info("Topic deleted: '{}' by user '{}'", topic.getName(), user.getUsername());
    }

    @Transactional
    public TopicDTO.Response markAsLearned(Long topicId, TopicDTO.MarkLearnedRequest request) {
        User user = authService.getCurrentUser();
        Topic topic = findTopicForUser(topicId, user.getId());

        if (topic.getStatus() != Topic.TopicStatus.NOT_STARTED) {
            throw new BadRequestException("Topic is already in learning or learned state");
        }

        topic.setStatus(Topic.TopicStatus.LEARNED);
        topic.setDateLearned(LocalDate.now());
        topic.setConfidenceLevel(request.getConfidenceLevel());

        // Schedule first revision
        int nextDays = priorityService.calculateNextRevisionDays(0.0, request.getConfidenceLevel());
        topic.setNextRevisionDate(LocalDate.now().plusDays(nextDays));

        // Calculate initial priority
        topic.setRevisionPriorityScore(priorityService.calculatePriorityScore(topic));
        topic.setForgettingRisk(priorityService.calculateForgettingRisk(topic));

        topic = topicRepository.save(topic);
        log.info("Topic '{}' marked as learned with confidence {}", topic.getName(), request.getConfidenceLevel());
        return mapToResponse(topic);
    }

    @Transactional
    public void recalculateAllPriorityScores() {
        User user = authService.getCurrentUser();
        List<Topic> topics = topicRepository.findByUserId(user.getId());
        for (Topic topic : topics) {
            if (topic.getStatus() != Topic.TopicStatus.NOT_STARTED) {
                topic.setRevisionPriorityScore(priorityService.calculatePriorityScore(topic));
                topic.setForgettingRisk(priorityService.calculateForgettingRisk(topic));
            }
        }
        topicRepository.saveAll(topics);
    }

    @Transactional(readOnly = true)
    public List<TopicDTO.TodayRevisionItem> getTodayRevisionQueue() {
        User user = authService.getCurrentUser();
        List<Topic> dueTopics = topicRepository.findDueForRevision(user.getId(), LocalDate.now());

        // Also include high-risk topics not yet flagged as due
        List<Topic> allTopics = topicRepository.findTopicsByUserIdOrderByPriority(user.getId());

        // Merge and deduplicate — top 10 by priority score
        return allTopics.stream()
                .filter(t -> t.getStatus() != Topic.TopicStatus.NOT_STARTED)
                .sorted((a, b) -> Double.compare(b.getRevisionPriorityScore(), a.getRevisionPriorityScore()))
                .limit(10)
                .map(t -> {
                    TopicDTO.TodayRevisionItem item = new TopicDTO.TodayRevisionItem();
                    item.setTopicId(t.getId());
                    item.setTopicName(t.getName());
                    item.setSubject(t.getSubject());
                    item.setDifficulty(t.getDifficulty());
                    item.setPriorityScore(t.getRevisionPriorityScore());
                    item.setRiskLevel(t.getForgettingRisk());
                    item.setReason(priorityService.generatePriorityReason(t));
                    item.setLastRevisedDate(t.getLastRevisedDate());
                    item.setConfidenceLevel(t.getConfidenceLevel());
                    item.setLatestQuizScore(t.getLatestQuizScore());
                    if (t.getLastRevisedDate() != null) {
                        item.setDaysSince(ChronoUnit.DAYS.between(t.getLastRevisedDate(), LocalDate.now()));
                    } else if (t.getDateLearned() != null) {
                        item.setDaysSince(ChronoUnit.DAYS.between(t.getDateLearned(), LocalDate.now()));
                    }
                    return item;
                })
                .collect(Collectors.toList());
    }

    // ────────────────────────────────────────────────────────────────────────
    // Helper methods
    // ────────────────────────────────────────────────────────────────────────

    public Topic findTopicForUser(Long topicId, Long userId) {
        return topicRepository.findByIdAndUserId(topicId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Topic not found with id: " + topicId));
    }

    public TopicDTO.Response mapToResponse(Topic topic) {
        TopicDTO.Response response = new TopicDTO.Response();
        response.setId(topic.getId());
        response.setName(topic.getName());
        response.setSubject(topic.getSubject());
        response.setDifficulty(topic.getDifficulty());
        response.setStatus(topic.getStatus());
        response.setDateLearned(topic.getDateLearned());
        response.setLastRevisedDate(topic.getLastRevisedDate());
        response.setNextRevisionDate(topic.getNextRevisionDate());
        response.setRevisionCount(topic.getRevisionCount());
        response.setConfidenceLevel(topic.getConfidenceLevel());
        response.setLatestQuizScore(topic.getLatestQuizScore());
        response.setAverageQuizScore(topic.getAverageQuizScore());
        response.setTotalMistakes(topic.getTotalMistakes());
        response.setConsecutiveSuccessfulRevisions(topic.getConsecutiveSuccessfulRevisions());
        response.setForgettingRisk(topic.getForgettingRisk());
        response.setRevisionPriorityScore(topic.getRevisionPriorityScore());
        response.setDescription(topic.getDescription());
        response.setTags(topic.getTags());
        response.setCreatedAt(topic.getCreatedAt());
        response.setUpdatedAt(topic.getUpdatedAt());

        // Computed fields
        response.setMemoryStrength(priorityService.estimateMemoryStrength(topic));
        response.setPriorityReason(priorityService.generatePriorityReason(topic));
        response.setQuestionCount(questionRepository.countByTopicId(topic.getId()));

        if (topic.getLastRevisedDate() != null) {
            response.setDaysSinceRevision(
                    ChronoUnit.DAYS.between(topic.getLastRevisedDate(), LocalDate.now()));
        } else if (topic.getDateLearned() != null) {
            response.setDaysSinceRevision(
                    ChronoUnit.DAYS.between(topic.getDateLearned(), LocalDate.now()));
        }

        return response;
    }
}
