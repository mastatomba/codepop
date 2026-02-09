package nl.schoutens.codepop.dto;

import java.util.List;

public record QuizDTO(String topic, Integer totalQuestions, List<QuestionDTO> questions) {}
