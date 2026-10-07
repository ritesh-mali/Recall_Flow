package com.recallflow.app.controller;

import com.recallflow.app.dto.QuizDTO;
import com.recallflow.app.entity.Question;
import com.recallflow.app.entity.Topic;
import com.recallflow.app.service.QuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @GetMapping("/{topicId}/questions")
    public ResponseEntity<List<QuizDTO.QuestionResponse>> getQuizQuestions(
            @PathVariable Long topicId,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(quizService.getQuizQuestions(topicId, limit));
    }

    @GetMapping("/{topicId}/all-questions")
    public ResponseEntity<List<QuizDTO.QuestionResponse>> getAllQuestions(@PathVariable Long topicId) {
        return ResponseEntity.ok(quizService.getAllQuestionsForTopic(topicId));
    }

    @PostMapping("/submit")
    public ResponseEntity<QuizDTO.QuizResult> submitQuiz(@Valid @RequestBody QuizDTO.SubmitRequest request) {
        return ResponseEntity.ok(quizService.submitQuiz(request));
    }

    @GetMapping("/{topicId}/history")
    public ResponseEntity<List<QuizDTO.AttemptHistoryResponse>> getAttemptHistory(@PathVariable Long topicId) {
        return ResponseEntity.ok(quizService.getAttemptHistory(topicId));
    }

    @PostMapping("/{topicId}/questions")
    public ResponseEntity<Map<String, Object>> addQuestion(
            @PathVariable Long topicId,
            @RequestBody Question question) {
        Question saved = quizService.addQuestion(topicId, question);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", saved.getId(),
                "message", "Question added successfully"
        ));
    }
}
