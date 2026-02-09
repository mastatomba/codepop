package nl.schoutens.codepop.service;

import java.util.List;
import nl.schoutens.codepop.entity.Question;

/**
 * Interface for quiz question generation. Implementations should generate unique questions that
 * don't duplicate existing content.
 */
public interface QuizMaster {

  /**
   * Generate new quiz questions for a given topic.
   *
   * @param topic The topic for which to generate questions
   * @param count Number of questions to generate
   * @param existingQuestionTexts List of question texts that already exist in the database. The
   *     implementation should generate questions that are different from these.
   * @return List of newly generated Question entities (not yet persisted)
   */
  List<Question> generateQuestions(String topic, int count, List<String> existingQuestionTexts);
}
