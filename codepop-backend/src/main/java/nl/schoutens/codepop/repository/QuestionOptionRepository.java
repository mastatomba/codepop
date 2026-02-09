package nl.schoutens.codepop.repository;

import java.util.List;
import nl.schoutens.codepop.entity.QuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Long> {

  List<QuestionOption> findByQuestionId(Long questionId);
}
