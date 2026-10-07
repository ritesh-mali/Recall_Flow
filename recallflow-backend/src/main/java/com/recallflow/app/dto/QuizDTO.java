package com.recallflow.app.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class QuizDTO {

    @Data
    public static class QuestionResponse {
        private Long id;
        private String questionText;
        private String optionA;
        private String optionB;
        private String optionC;
        private String optionD;
        private String difficulty;
        private String subject;
        // correctAnswer is NOT included in response (only in submit)
    }

    @Data
    public static class SubmitRequest {
        @NotNull
        private Long topicId;

        @NotNull
        private List<AnswerItem> answers;

        private Integer durationSeconds;

        @Data
        public static class AnswerItem {
            @NotNull
            private Long questionId;
            @NotBlank
            private String selectedAnswer; // A, B, C, or D
            private Integer timeTakenSeconds;
        }
    }

    @Data
    public static class QuizResult {
        private Long attemptId;
        private Long topicId;
        private String topicName;
        private Integer totalQuestions;
        private Integer correctAnswers;
        private Integer wrongAnswers;
        private Double scorePercentage;
        private Integer durationSeconds;
        private LocalDateTime attemptDate;
        private List<AnswerDetail> answerDetails;
        private String performanceMessage;
        private Integer nextRevisionDays;
    }

    @Data
    public static class AnswerDetail {
        private Long questionId;
        private String questionText;
        private String selectedAnswer;
        private String correctAnswer;
        private Boolean isCorrect;
        private String explanation;
    }

    @Data
    public static class AttemptHistoryResponse {
        private Long id;
        private Long topicId;
        private String topicName;
        private Integer totalQuestions;
        private Integer correctAnswers;
        private Double scorePercentage;
        private LocalDateTime attemptDate;
    }
}
