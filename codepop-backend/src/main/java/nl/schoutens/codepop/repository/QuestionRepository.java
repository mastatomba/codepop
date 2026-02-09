package nl.schoutens.codepop.repository;

import nl.schoutens.codepop.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    
    List<Question> findByTopicId(Long topicId);
    
    @Query("SELECT q FROM Question q WHERE q.topic.id = :topicId AND LOWER(q.subtopic) LIKE LOWER(CONCAT('%', :subtopic, '%'))")
    List<Question> findByTopicIdAndSubtopicContainingIgnoreCase(@Param("topicId") Long topicId, @Param("subtopic") String subtopic);
}
