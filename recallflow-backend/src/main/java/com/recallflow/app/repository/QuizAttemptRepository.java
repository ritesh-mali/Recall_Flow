package com.recallflow.app.repository;

import com.recallflow.app.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    List<QuizAttempt> findByUserIdOrderByAttemptDateDesc(Long userId);

    List<QuizAttempt> findByTopicIdOrderByAttemptDateDesc(Long topicId);

    List<QuizAttempt> findByUserIdAndTopicIdOrderByAttemptDateDesc(Long userId, Long topicId);

    @Query("SELECT AVG(qa.scorePercentage) FROM QuizAttempt qa WHERE qa.user.id = :userId")
    Double findOverallAverageScoreByUserId(@Param("userId") Long userId);

    @Query("SELECT AVG(qa.scorePercentage) FROM QuizAttempt qa WHERE qa.topic.id = :topicId AND qa.user.id = :userId")
    Double findAverageScoreByTopicAndUser(@Param("topicId") Long topicId, @Param("userId") Long userId);

    @Query("SELECT qa FROM QuizAttempt qa WHERE qa.user.id = :userId AND qa.attemptDate >= :since ORDER BY qa.attemptDate DESC")
    List<QuizAttempt> findRecentAttempts(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    long countByUserId(Long userId);
}
