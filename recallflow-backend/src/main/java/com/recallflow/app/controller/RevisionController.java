package com.recallflow.app.controller;

import com.recallflow.app.dto.RevisionDTO;
import com.recallflow.app.service.RevisionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/revision")
@RequiredArgsConstructor
public class RevisionController {

    private final RevisionService revisionService;

    @GetMapping("/{topicId}")
    public ResponseEntity<RevisionDTO.SessionInfo> getRevisionSession(@PathVariable Long topicId) {
        return ResponseEntity.ok(revisionService.getRevisionSession(topicId));
    }

    @PostMapping("/{topicId}/complete")
    public ResponseEntity<RevisionDTO.Response> completeRevision(
            @PathVariable Long topicId,
            @Valid @RequestBody RevisionDTO.CompleteRequest request,
            @RequestParam(required = false) Double quizScore) {
        return ResponseEntity.ok(revisionService.completeRevision(topicId, request, quizScore));
    }

    @GetMapping("/{topicId}/history")
    public ResponseEntity<List<RevisionDTO.Response>> getRevisionHistory(@PathVariable Long topicId) {
        return ResponseEntity.ok(revisionService.getRevisionHistory(topicId));
    }

    @GetMapping
    public ResponseEntity<List<RevisionDTO.Response>> getAllRevisions() {
        return ResponseEntity.ok(revisionService.getAllRevisions());
    }
}
