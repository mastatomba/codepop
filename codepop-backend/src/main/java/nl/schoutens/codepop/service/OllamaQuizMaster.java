package nl.schoutens.codepop.service;

import nl.schoutens.codepop.entity.Question;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Stub implementation of QuizMaster that returns no questions.
 * Actual Ollama LLM integration will be implemented later.
 */
@Service
@Primary
public class OllamaQuizMaster implements QuizMaster {

    @Override
    public List<Question> generateQuestions(String topic, int count, List<String> existingQuestionTexts) {
        // TODO: Implement actual Ollama API integration
        // For now, return empty list (no questions generated)
        return Collections.emptyList();
    }
}
