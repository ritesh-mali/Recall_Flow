package com.recallflow.app.repository;

import com.recallflow.app.entity.Question;
import com.recallflow.app.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByTopicId(Long topicId);

    List<Question> findByTopicIdAndDifficulty(Long topicId, Topic.Difficulty difficulty);

    @Query("SELECT q FROM Question q WHERE q.topic.id = :topicId ORDER BY RAND() LIMIT :limit")
    List<Question> findRandomQuestionsByTopicId(@Param("topicId") Long topicId, @Param("limit") int limit);

    @Query("SELECT q FROM Question q WHERE q.subject = :subject ORDER BY RAND() LIMIT :limit")
    List<Question> findRandomQuestionsBySubject(@Param("subject") String subject, @Param("limit") int limit);

    long countByTopicId(Long topicId);
}
