package com.recallflow.app.repository;

import com.recallflow.app.entity.Topic;
import com.recallflow.app.entity.Topic.RiskLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {

    List<Topic> findByUserId(Long userId);

    List<Topic> findByUserIdAndSubject(Long userId, String subject);

    Optional<Topic> findByIdAndUserId(Long id, Long userId);

    List<Topic> findByUserIdAndStatus(Long userId, Topic.TopicStatus status);

    @Query("SELECT t FROM Topic t WHERE t.user.id = :userId AND t.status != 'NOT_STARTED' " +
           "ORDER BY t.revisionPriorityScore DESC")
    List<Topic> findTopicsByUserIdOrderByPriority(@Param("userId") Long userId);

    @Query("SELECT t FROM Topic t WHERE t.user.id = :userId AND " +
           "(t.nextRevisionDate IS NULL OR t.nextRevisionDate <= :today) AND " +
           "t.status IN ('LEARNED', 'UNDER_REVISION') " +
           "ORDER BY t.revisionPriorityScore DESC")
    List<Topic> findDueForRevision(@Param("userId") Long userId, @Param("today") LocalDate today);

    @Query("SELECT t FROM Topic t WHERE t.user.id = :userId AND t.forgettingRisk IN :risks")
    List<Topic> findByUserIdAndForgettingRiskIn(@Param("userId") Long userId, @Param("risks") List<RiskLevel> risks);

    @Query("SELECT DISTINCT t.subject FROM Topic t WHERE t.user.id = :userId")
    List<String> findDistinctSubjectsByUserId(@Param("userId") Long userId);

    @Query("SELECT t FROM Topic t WHERE t.user.id = :userId AND " +
           "(t.averageQuizScore < 70 OR t.totalMistakes >= 3 OR t.confidenceLevel <= 2) " +
           "AND t.status IN ('LEARNED', 'UNDER_REVISION') " +
           "ORDER BY t.revisionPriorityScore DESC")
    List<Topic> findWeakTopics(@Param("userId") Long userId);

    long countByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, Topic.TopicStatus status);
}
