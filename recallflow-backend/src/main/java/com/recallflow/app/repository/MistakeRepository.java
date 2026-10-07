package com.recallflow.app.repository;

import com.recallflow.app.entity.Mistake;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MistakeRepository extends JpaRepository<Mistake, Long> {

    List<Mistake> findByUserIdOrderByMistakeDateDesc(Long userId);

    List<Mistake> findByUserIdAndTopicId(Long userId, Long topicId);

    @Query("SELECT m FROM Mistake m WHERE m.user.id = :userId AND m.question.id = :questionId")
    List<Mistake> findByUserIdAndQuestionId(@Param("userId") Long userId, @Param("questionId") Long questionId);

    @Query("SELECT COUNT(m) FROM Mistake m WHERE m.user.id = :userId AND m.topic.id = :topicId")
    long countByUserIdAndTopicId(@Param("userId") Long userId, @Param("topicId") Long topicId);

    @Query("SELECT m.topic.id, COUNT(m) as mistakeCount FROM Mistake m WHERE m.user.id = :userId " +
           "GROUP BY m.topic.id ORDER BY mistakeCount DESC")
    List<Object[]> findMistakeCountGroupedByTopic(@Param("userId") Long userId);

    @Query("SELECT m FROM Mistake m WHERE m.user.id = :userId AND m.question.id = :questionId ORDER BY m.mistakeDate DESC")
    List<Mistake> findRepeatMistakesForQuestion(@Param("userId") Long userId, @Param("questionId") Long questionId);

    long countByUserId(Long userId);

    @Query("SELECT m FROM Mistake m WHERE m.user.id = :userId AND m.reviewed = false ORDER BY m.mistakeDate DESC")
    List<Mistake> findUnreviewedMistakes(@Param("userId") Long userId);
}
