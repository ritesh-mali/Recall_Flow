package com.recallflow.app.controller;

import com.recallflow.app.dto.AnalyticsDTO;
import com.recallflow.app.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping
    public ResponseEntity<AnalyticsDTO.DashboardStats> getDashboardStats() {
        return ResponseEntity.ok(analyticsService.getDashboardStats());
    }

    @GetMapping("/subjects")
    public ResponseEntity<List<AnalyticsDTO.SubjectPerformance>> getSubjectPerformance() {
        return ResponseEntity.ok(analyticsService.getSubjectPerformance());
    }

    @GetMapping("/weak-topics")
    public ResponseEntity<List<AnalyticsDTO.WeakTopic>> getWeakTopics() {
        return ResponseEntity.ok(analyticsService.getWeakTopics());
    }

    @GetMapping("/recommendations")
    public ResponseEntity<List<AnalyticsDTO.Recommendation>> getRecommendations() {
        return ResponseEntity.ok(analyticsService.getRecommendations());
    }

    @GetMapping("/quiz-performance")
    public ResponseEntity<List<AnalyticsDTO.QuizPerformancePoint>> getQuizPerformance(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(analyticsService.getQuizPerformanceOverTime(days));
    }

    @GetMapping("/revision-activity")
    public ResponseEntity<List<AnalyticsDTO.RevisionActivityPoint>> getRevisionActivity(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(analyticsService.getRevisionActivity(days));
    }

    @GetMapping("/mistakes")
    public ResponseEntity<List<AnalyticsDTO.MistakeResponse>> getMistakes() {
        return ResponseEntity.ok(analyticsService.getMistakes());
    }

    @GetMapping("/mistakes/summary")
    public ResponseEntity<List<AnalyticsDTO.MistakeSummary>> getMistakeSummary() {
        return ResponseEntity.ok(analyticsService.getMistakeSummary());
    }
}
