package com.recallflow.app.controller;

import com.recallflow.app.dto.TopicDTO;
import com.recallflow.app.entity.User;
import com.recallflow.app.service.AuthService;
import com.recallflow.app.service.TopicService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/topics")
@RequiredArgsConstructor
public class TopicController {

    private final TopicService topicService;
    private final AuthService authService;

    @PostMapping
    public ResponseEntity<TopicDTO.Response> createTopic(@Valid @RequestBody TopicDTO.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(topicService.createTopic(request));
    }

    @GetMapping
    public ResponseEntity<List<TopicDTO.Response>> getAllTopics() {
        return ResponseEntity.ok(topicService.getAllTopics());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TopicDTO.Response> getTopicById(@PathVariable Long id) {
        return ResponseEntity.ok(topicService.getTopicById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TopicDTO.Response> updateTopic(
            @PathVariable Long id, @Valid @RequestBody TopicDTO.UpdateRequest request) {
        return ResponseEntity.ok(topicService.updateTopic(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteTopic(@PathVariable Long id) {
        topicService.deleteTopic(id);
        return ResponseEntity.ok(Map.of("message", "Topic deleted successfully"));
    }

    @PostMapping("/{id}/learn")
    public ResponseEntity<TopicDTO.Response> markAsLearned(
            @PathVariable Long id, @Valid @RequestBody TopicDTO.MarkLearnedRequest request) {
        return ResponseEntity.ok(topicService.markAsLearned(id, request));
    }

    @PostMapping("/recalculate-priorities")
    public ResponseEntity<Map<String, String>> recalculatePriorities() {
        topicService.recalculateAllPriorityScores();
        return ResponseEntity.ok(Map.of("message", "Priority scores recalculated"));
    }

    @GetMapping("/revision/today")
    public ResponseEntity<List<TopicDTO.TodayRevisionItem>> getTodayRevisionQueue() {
        return ResponseEntity.ok(topicService.getTodayRevisionQueue());
    }
}
