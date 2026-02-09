package nl.schoutens.codepop.dto;

import java.util.List;

public record QuestionDTO(
    Long id, String text, String difficulty, String explanation, List<OptionDTO> options) {}
