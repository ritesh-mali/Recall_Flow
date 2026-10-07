package com.recallflow.app.repository;

import com.recallflow.app.entity.Revision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RevisionRepository extends JpaRepository<Revision, Long> {

    List<Revision> findByTopicIdOrderByRevisionDateDesc(Long topicId);

    List<Revision> findByUserIdOrderByRevisionDateDesc(Long userId);

    @Query("SELECT r FROM Revision r WHERE r.user.id = :userId AND r.revisionDate = :date")
    List<Revision> findByUserIdAndRevisionDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Query("SELECT COUNT(r) FROM Revision r WHERE r.user.id = :userId AND r.revisionDate >= :since")
    long countRevisionsSince(@Param("userId") Long userId, @Param("since") LocalDate since);

    @Query("SELECT r FROM Revision r WHERE r.topic.id = :topicId ORDER BY r.revisionDate DESC")
    List<Revision> findLastRevisionsForTopic(@Param("topicId") Long topicId);

    @Query("SELECT AVG(r.quizScore) FROM Revision r WHERE r.topic.id = :topicId AND r.quizScore IS NOT NULL")
    Double findAverageQuizScoreForTopic(@Param("topicId") Long topicId);

    @Query("SELECT COUNT(r) FROM Revision r WHERE r.user.id = :userId AND r.revisionDate = :date")
    long countByUserIdAndRevisionDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    List<Revision> findByUserIdAndRevisionDateBetweenOrderByRevisionDateAsc(
            Long userId, LocalDate start, LocalDate end);
}
