package nl.schoutens.codepop.repository;

import nl.schoutens.codepop.entity.QuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Long> {
    
    List<QuestionOption> findByQuestionId(Long questionId);
}
