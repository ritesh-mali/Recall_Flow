package com.recallflow.app.service;

import com.recallflow.app.dto.QuizDTO;
import com.recallflow.app.entity.*;
import com.recallflow.app.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizService {

    private final QuestionRepository questionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final MistakeRepository mistakeRepository;
    private final TopicRepository topicRepository;
    private final RevisionService revisionService;
    private final RevisionPriorityService priorityService;
    private final AuthService authService;
    private final TopicService topicService;

    @Transactional(readOnly = true)
    public List<QuizDTO.QuestionResponse> getQuizQuestions(Long topicId, int limit) {
        User user = authService.getCurrentUser();
        topicService.findTopicForUser(topicId, user.getId()); // validate ownership

        List<Question> questions = questionRepository.findRandomQuestionsByTopicId(topicId, limit);
        return questions.stream().map(this::mapQuestionToResponse).collect(Collectors.toList());
    }

    @Transactional
    public QuizDTO.QuizResult submitQuiz(QuizDTO.SubmitRequest request) {
        User user = authService.getCurrentUser();
        Topic topic = topicService.findTopicForUser(request.getTopicId(), user.getId());

        List<QuizDTO.AnswerDetail> answerDetails = new ArrayList<>();
        int correctCount = 0;

        // Create quiz attempt
        QuizAttempt attempt = QuizAttempt.builder()
                .user(user)
                .topic(topic)
                .totalQuestions(request.getAnswers().size())
                .attemptDate(LocalDateTime.now())
                .build();
        attempt = quizAttemptRepository.save(attempt);

        // Process each answer
        for (QuizDTO.SubmitRequest.AnswerItem answerItem : request.getAnswers()) {
            Question question = questionRepository.findById(answerItem.getQuestionId())
                    .orElseThrow();

            boolean isCorrect = question.getCorrectAnswer().equalsIgnoreCase(answerItem.getSelectedAnswer());
            if (isCorrect) correctCount++;

            // Save quiz answer
            QuizAnswer quizAnswer = QuizAnswer.builder()
                    .quizAttempt(attempt)
                    .question(question)
                    .selectedAnswer(answerItem.getSelectedAnswer().toUpperCase())
                    .correctAnswer(question.getCorrectAnswer())
                    .isCorrect(isCorrect)
                    .timeTakenSeconds(answerItem.getTimeTakenSeconds())
                    .build();
            attempt.getAnswers().add(quizAnswer);

            // Record mistake if wrong
            if (!isCorrect) {
                long existingMistakes = mistakeRepository.countByUserIdAndTopicId(user.getId(), topic.getId());
                Mistake mistake = Mistake.builder()
                        .user(user)
                        .topic(topic)
                        .question(question)
                        .userAnswer(answerItem.getSelectedAnswer().toUpperCase())
                        .correctAnswer(question.getCorrectAnswer())
                        .mistakeDate(LocalDateTime.now())
                        .attemptNumber((int) existingMistakes + 1)
                        .build();
                mistakeRepository.save(mistake);
                topic.setTotalMistakes(topic.getTotalMistakes() + 1);
            }

            // Build answer detail for response
            QuizDTO.AnswerDetail detail = new QuizDTO.AnswerDetail();
            detail.setQuestionId(question.getId());
            detail.setQuestionText(question.getQuestionText());
            detail.setSelectedAnswer(answerItem.getSelectedAnswer().toUpperCase());
            detail.setCorrectAnswer(question.getCorrectAnswer());
            detail.setIsCorrect(isCorrect);
            detail.setExplanation(question.getExplanation());
            answerDetails.add(detail);
        }

        // Calculate score
        int total = request.getAnswers().size();
        double scorePercent = total > 0 ? (correctCount * 100.0 / total) : 0.0;

        // Update attempt with results
        attempt.setCorrectAnswers(correctCount);
        attempt.setWrongAnswers(total - correctCount);
        attempt.setScorePercentage(scorePercent);
        attempt.setDurationSeconds(request.getDurationSeconds());
        quizAttemptRepository.save(attempt);

        // Update topic quiz stats
        topic.setLatestQuizScore(scorePercent);
        Double avg = quizAttemptRepository.findAverageScoreByTopicAndUser(topic.getId(), user.getId());
        topic.setAverageQuizScore(avg != null ? avg : scorePercent);

        // Recalculate risk and priority
        topic.setRevisionPriorityScore(priorityService.calculatePriorityScore(topic));
        topic.setForgettingRisk(priorityService.calculateForgettingRisk(topic));
        topicRepository.save(topic);

        // Determine next revision days
        int nextDays = priorityService.calculateNextRevisionDays(scorePercent, topic.getConfidenceLevel());

        // Performance message
        String message = generatePerformanceMessage(scorePercent);

        log.info("Quiz submitted for topic '{}': score={:.1f}%, correct={}/{}", 
                topic.getName(), scorePercent, correctCount, total);

        QuizDTO.QuizResult result = new QuizDTO.QuizResult();
        result.setAttemptId(attempt.getId());
        result.setTopicId(topic.getId());
        result.setTopicName(topic.getName());
        result.setTotalQuestions(total);
        result.setCorrectAnswers(correctCount);
        result.setWrongAnswers(total - correctCount);
        result.setScorePercentage(scorePercent);
        result.setDurationSeconds(request.getDurationSeconds());
        result.setAttemptDate(attempt.getAttemptDate());
        result.setAnswerDetails(answerDetails);
        result.setPerformanceMessage(message);
        result.setNextRevisionDays(nextDays);
        return result;
    }

    @Transactional(readOnly = true)
    public List<QuizDTO.AttemptHistoryResponse> getAttemptHistory(Long topicId) {
        User user = authService.getCurrentUser();
        return quizAttemptRepository.findByUserIdAndTopicIdOrderByAttemptDateDesc(user.getId(), topicId)
                .stream()
                .map(a -> {
                    QuizDTO.AttemptHistoryResponse h = new QuizDTO.AttemptHistoryResponse();
                    h.setId(a.getId());
                    h.setTopicId(a.getTopic().getId());
                    h.setTopicName(a.getTopic().getName());
                    h.setTotalQuestions(a.getTotalQuestions());
                    h.setCorrectAnswers(a.getCorrectAnswers());
                    h.setScorePercentage(a.getScorePercentage());
                    h.setAttemptDate(a.getAttemptDate());
                    return h;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public Question addQuestion(Long topicId, Question question) {
        User user = authService.getCurrentUser();
        Topic topic = topicService.findTopicForUser(topicId, user.getId());
        question.setTopic(topic);
        question.setSubject(topic.getSubject());
        return questionRepository.save(question);
    }

    @Transactional(readOnly = true)
    public List<QuizDTO.QuestionResponse> getAllQuestionsForTopic(Long topicId) {
        User user = authService.getCurrentUser();
        topicService.findTopicForUser(topicId, user.getId());
        return questionRepository.findByTopicId(topicId)
                .stream().map(this::mapQuestionToResponse).collect(Collectors.toList());
    }

    private QuizDTO.QuestionResponse mapQuestionToResponse(Question q) {
        QuizDTO.QuestionResponse r = new QuizDTO.QuestionResponse();
        r.setId(q.getId());
        r.setQuestionText(q.getQuestionText());
        r.setOptionA(q.getOptionA());
        r.setOptionB(q.getOptionB());
        r.setOptionC(q.getOptionC());
        r.setOptionD(q.getOptionD());
        r.setDifficulty(q.getDifficulty().name());
        r.setSubject(q.getSubject());
        return r;
    }

    private String generatePerformanceMessage(double score) {
        if (score >= 90) return "Excellent! Outstanding performance!";
        if (score >= 80) return "Great work! You're doing very well.";
        if (score >= 70) return "Good job! Keep revising to improve further.";
        if (score >= 60) return "Fair performance. More practice needed.";
        if (score >= 50) return "Below average. Focus on weak areas.";
        return "Needs significant improvement. Revise this topic soon!";
    }
}
