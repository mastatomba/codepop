package nl.schoutens.codepop.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import nl.schoutens.codepop.entity.Question;
import nl.schoutens.codepop.entity.Question.Difficulty;
import nl.schoutens.codepop.entity.QuestionOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Ollama LLM implementation of QuizMaster. Generates quiz questions using Ollama API via Spring AI
 * ChatClient. Only active in non-test profiles.
 */
@Service
@Primary
@Profile("!test")
public class OllamaQuizMaster implements QuizMaster {

  private static final Logger logger = LoggerFactory.getLogger(OllamaQuizMaster.class);
  private static final ObjectMapper objectMapper = new ObjectMapper();

  private final ChatClient chatClient;

  public OllamaQuizMaster(ChatClient.Builder builder) {
    this.chatClient =
        builder
            .defaultAdvisors(new SimpleLoggerAdvisor())
            .defaultOptions(
                org.springframework.ai.chat.prompt.ChatOptions.builder().temperature(0.8).build())
            .build();
  }

  @Override
  public List<Question> generateQuestions(
      String topic, int count, List<String> existingQuestionTexts) {
    logger.info("Generating {} questions for topic: {}", count, topic);

    String prompt = buildPrompt(topic, count, existingQuestionTexts);
    String response;

    try {
      response = chatClient.prompt(prompt).call().content();
      logger.debug("LLM response received: {}", response);
    } catch (Exception e) {
      logger.error("Failed to call Ollama LLM", e);
      return List.of();
    }

    return parseResponse(response);
  }

  private String buildPrompt(String topic, int count, List<String> existingQuestionTexts) {
    // Calculate difficulty distribution (40% easy, 40% medium, 20% hard)
    int easyCount = (int) Math.ceil(count * 0.4);
    int hardCount = (int) Math.ceil(count * 0.2);
    int mediumCount = count - easyCount - hardCount;

    StringBuilder prompt = new StringBuilder();
    prompt.append("You are a quiz master specialized in coding topics. ");
    prompt.append("Generate ").append(count).append(" multiple-choice quiz questions about: ");
    prompt.append(topic).append("\n\n");

    prompt.append("Requirements:\n");
    prompt.append("- Generate exactly ").append(easyCount).append(" easy, ");
    prompt.append(mediumCount).append(" medium, and ");
    prompt.append(hardCount).append(" hard questions\n");
    prompt.append("- Each question must have exactly 4 options\n");
    prompt.append("- Exactly one option must be marked [CORRECT]\n");
    prompt.append("- Create factual, verifiable questions (no opinions or ambiguous questions)\n");
    prompt.append("- Include explanations for each question\n");
    prompt.append("- Make the incorrect options plausible distractors\n");
    prompt.append(
        "- ENCOURAGED: Include code snippets in questions using markdown code blocks\n\n");

    if (existingQuestionTexts != null && !existingQuestionTexts.isEmpty()) {
      prompt.append(
          "IMPORTANT: Avoid generating questions similar to these "
              + existingQuestionTexts.size()
              + " existing ones:\n");
      for (String existingText : existingQuestionTexts) {
        prompt.append("- ").append(existingText).append("\n");
      }
      prompt.append(
          "\nGenerate questions on DIFFERENT aspects of "
              + topic
              + " that are NOT covered above.\n\n");
    }

    prompt.append("RESPONSE FORMAT:\n");
    prompt.append("Use the delimiter-based format below. Do NOT use JSON.\n");
    prompt.append(
        "Code snippets can be included directly with no escaping - use markdown code blocks.\n\n");

    prompt.append("### QUESTION 1 ###\n");
    prompt.append("DIFFICULTY: easy\n");
    prompt.append("QUESTION: What is the output of `print(2 + 2)`?\n");
    prompt.append("OPTION: 4 [CORRECT]\n");
    prompt.append("OPTION: 22\n");
    prompt.append("OPTION: TypeError\n");
    prompt.append("OPTION: SyntaxError\n");
    prompt.append("EXPLANATION: The print function outputs the result of 2+2\n\n");

    prompt.append("### QUESTION 2 ###\n");
    prompt.append("DIFFICULTY: medium\n");
    prompt.append("QUESTION: What does this method return?\n");
    prompt.append("```java\n");
    prompt.append("public int add(int a, int b) {\n");
    prompt.append("  return a + b;\n");
    prompt.append("}\n");
    prompt.append("add(2, 3)\n");
    prompt.append("```\n");
    prompt.append("OPTION: 5 [CORRECT]\n");
    prompt.append("OPTION: 23\n");
    prompt.append("OPTION: Error\n");
    prompt.append("OPTION: null\n");
    prompt.append("EXPLANATION: The method adds two integers and returns 5\n\n");

    prompt.append("CRITICAL RULES:\n");
    prompt.append("- Start each question with ### QUESTION N ### (where N is 1, 2, 3...)\n");
    prompt.append("- Use DIFFICULTY: easy/medium/hard\n");
    prompt.append("- Use QUESTION: for the question text\n");
    prompt.append("- Use OPTION: for each option (mark correct one with [CORRECT])\n");
    prompt.append("- Use EXPLANATION: for the explanation\n");
    prompt.append("- Multi-line content (code blocks) can span multiple lines naturally\n");
    prompt.append("- Generate ALL ").append(count).append(" questions in this format\n");

    return prompt.toString();
  }

  private List<Question> parseResponse(String response) {
    List<Question> questions = new ArrayList<>();

    // Log raw response for debugging
    logger.debug("Raw LLM response length: {} chars", response.length());
    logger.debug("Raw LLM response:\n{}", response);

    // Strategy 1: Try delimiter format first (new preferred format)
    if (response.contains("### QUESTION")) {
      logger.debug("Detected delimiter format, using delimiter parser");
      questions = parseDelimitedFormat(response);
      if (!questions.isEmpty()) {
        logger.info("Successfully parsed {} questions using delimiter format", questions.size());
        return questions;
      } else {
        logger.warn("Delimiter format detected but parsing failed, trying JSON fallback");
      }
    }

    // Strategy 2: Fall back to JSON parsing (backward compatibility)
    logger.debug("Using JSON parser (fallback or no delimiter format detected)");

    // Clean response - try to extract JSON from various formats
    String cleanedResponse = extractJson(response);

    // Log cleaned response for debugging
    if (cleanedResponse == null || cleanedResponse.isEmpty()) {
      logger.error(
          "Failed to extract JSON from LLM response. First 200 chars: {}",
          response.length() > 200 ? response.substring(0, 200) : response);
      return questions;
    }

    logger.debug("Cleaned JSON response:\n{}", cleanedResponse);

    try {
      JsonNode root = objectMapper.readTree(cleanedResponse);
      JsonNode questionsNode = root.get("questions");

      if (questionsNode == null || !questionsNode.isArray()) {
        logger.error("Invalid JSON structure: missing 'questions' array");
        return questions;
      }

      for (JsonNode questionNode : questionsNode) {
        try {
          Question question = parseQuestion(questionNode);
          if (validateQuestion(question)) {
            questions.add(question);
          }
        } catch (Exception e) {
          logger.warn("Failed to parse individual question, skipping", e);
        }
      }

      logger.info("Successfully parsed {} valid questions", questions.size());
    } catch (JsonProcessingException e) {
      logger.error("Failed to parse JSON response", e);
    }

    return questions;
  }

  /**
   * Extract JSON from LLM response using multiple strategies: 1. Remove outer markdown code blocks
   * 2. Find JSON object boundaries 3. Balance braces to handle nested objects 4. Validate JSON
   * structure
   */
  private String extractJson(String response) {
    if (response == null || response.isEmpty()) {
      return null;
    }

    String cleaned = response.trim();

    // Strategy 1: Remove outer markdown code blocks (```json or ```)
    if (cleaned.startsWith("```json")) {
      cleaned = cleaned.substring(7);
    } else if (cleaned.startsWith("```")) {
      cleaned = cleaned.substring(3);
    }
    if (cleaned.endsWith("```")) {
      cleaned = cleaned.substring(0, cleaned.length() - 3);
    }
    cleaned = cleaned.trim();

    // Strategy 2: Find JSON object boundaries
    int jsonStart = cleaned.indexOf('{');
    if (jsonStart == -1) {
      logger.error("No opening brace found in response");
      return null;
    }

    // Strategy 3: Balance braces to find matching closing brace
    // This handles nested objects (e.g., code snippets with { } inside strings)
    int jsonEnd = findMatchingBrace(cleaned, jsonStart);
    if (jsonEnd == -1) {
      logger.error("No matching closing brace found");
      return null;
    }

    cleaned = cleaned.substring(jsonStart, jsonEnd + 1);

    // Strategy 4: Validate it looks like JSON
    if (!cleaned.startsWith("{") || !cleaned.endsWith("}")) {
      logger.error("Extracted content doesn't look like JSON: {}", cleaned.substring(0, 100));
      return null;
    }

    return cleaned;
  }

  /**
   * Find the matching closing brace for the opening brace at startIndex. Handles nested braces and
   * escaped characters inside strings.
   */
  private int findMatchingBrace(String text, int startIndex) {
    int depth = 0;
    boolean inString = false;
    boolean escaped = false;

    for (int i = startIndex; i < text.length(); i++) {
      char c = text.charAt(i);

      if (escaped) {
        escaped = false;
        continue;
      }

      if (c == '\\') {
        escaped = true;
        continue;
      }

      if (c == '"') {
        inString = !inString;
        continue;
      }

      if (!inString) {
        if (c == '{') {
          depth++;
        } else if (c == '}') {
          depth--;
          if (depth == 0) {
            return i;
          }
        }
      }
    }

    return -1; // No matching brace found
  }

  /**
   * Parse questions from delimiter-based format. Format: ### QUESTION N ### DIFFICULTY: easy
   * QUESTION: text OPTION: option1 [CORRECT] OPTION: option2 EXPLANATION: text
   *
   * <p>This format is more robust than JSON for LLM generation - no escaping needed for code
   * snippets.
   */
  private List<Question> parseDelimitedFormat(String response) {
    List<Question> questions = new ArrayList<>();

    // Split into question blocks using the delimiter
    String[] blocks = response.split("###\\s*QUESTION\\s+\\d+\\s*###");

    for (int i = 1; i < blocks.length; i++) { // Start at 1 to skip any preamble
      String block = blocks[i].trim();
      if (block.isEmpty()) {
        continue;
      }

      try {
        Question question = parseDelimitedQuestion(block);
        if (validateQuestion(question)) {
          questions.add(question);
        }
      } catch (Exception e) {
        logger.warn("Failed to parse delimited question block {}, skipping: {}", i, e.getMessage());
      }
    }

    logger.info("Successfully parsed {} questions from delimited format", questions.size());
    return questions;
  }

  /**
   * Parse a single question from a delimited block. Extracts DIFFICULTY, QUESTION, OPTIONs, and
   * EXPLANATION.
   */
  private Question parseDelimitedQuestion(String block) {
    String difficulty = null;
    StringBuilder questionText = new StringBuilder();
    List<String> options = new ArrayList<>();
    List<Boolean> correctFlags = new ArrayList<>();
    StringBuilder explanation = new StringBuilder();

    String[] lines = block.split("\n");
    String currentField = null;

    for (String line : lines) {
      String trimmed = line.trim();

      if (trimmed.startsWith("DIFFICULTY:")) {
        difficulty = trimmed.substring("DIFFICULTY:".length()).trim().toLowerCase();
        currentField = "DIFFICULTY";
      } else if (trimmed.startsWith("QUESTION:")) {
        questionText.append(trimmed.substring("QUESTION:".length()).trim());
        currentField = "QUESTION";
      } else if (trimmed.startsWith("OPTION:")) {
        String optionText = trimmed.substring("OPTION:".length()).trim();
        boolean isCorrect = optionText.contains("[CORRECT]");
        if (isCorrect) {
          optionText = optionText.replace("[CORRECT]", "").trim();
        }
        options.add(optionText);
        correctFlags.add(isCorrect);
        currentField = "OPTION";
      } else if (trimmed.startsWith("EXPLANATION:")) {
        explanation.append(trimmed.substring("EXPLANATION:".length()).trim());
        currentField = "EXPLANATION";
      } else if (!trimmed.isEmpty()) {
        // Multi-line content - append to current field
        if ("QUESTION".equals(currentField)) {
          questionText.append("\n").append(line); // Preserve original formatting for code blocks
        } else if ("EXPLANATION".equals(currentField)) {
          explanation.append("\n").append(line);
        }
      }
    }

    // Validate required fields
    if (difficulty == null || questionText.length() == 0 || options.isEmpty()) {
      throw new IllegalArgumentException(
          "Missing required fields: difficulty="
              + difficulty
              + ", questionText="
              + (questionText.length() > 0)
              + ", options="
              + options.size());
    }

    // Build Question entity
    Question question = new Question();
    question.setQuestionText(questionText.toString().trim());
    question.setDifficulty(parseDifficulty(difficulty));
    question.setExplanation(explanation.length() > 0 ? explanation.toString().trim() : null);

    // Add options
    for (int i = 0; i < options.size(); i++) {
      QuestionOption option = new QuestionOption();
      option.setOptionText(options.get(i));
      option.setIsCorrect(correctFlags.get(i));
      question.addOption(option);
    }

    return question;
  }

  private Question parseQuestion(JsonNode node) {
    String questionText = node.get("question").asText();
    String difficultyStr = node.get("difficulty").asText().toLowerCase();
    String explanation = node.has("explanation") ? node.get("explanation").asText() : null;

    Difficulty difficulty = parseDifficulty(difficultyStr);

    Question question = new Question();
    question.setQuestionText(questionText);
    question.setDifficulty(difficulty);
    question.setExplanation(explanation);
    // Note: Topic and subtopic are set by QuizService after generation

    // Parse options
    JsonNode optionsNode = node.get("options");
    int correctIndex = node.get("correct_index").asInt();

    if (optionsNode != null && optionsNode.isArray()) {
      for (int i = 0; i < optionsNode.size(); i++) {
        String optionText = optionsNode.get(i).asText();
        boolean isCorrect = (i == correctIndex);

        QuestionOption option = new QuestionOption();
        option.setOptionText(optionText);
        option.setIsCorrect(isCorrect);
        question.addOption(option); // Use helper method to set bidirectional relationship
      }
    }

    return question;
  }

  private Difficulty parseDifficulty(String difficultyStr) {
    return switch (difficultyStr) {
      case "easy" -> Difficulty.EASY;
      case "medium" -> Difficulty.MEDIUM;
      case "hard" -> Difficulty.HARD;
      default -> {
        logger.warn("Unknown difficulty: {}, defaulting to MEDIUM", difficultyStr);
        yield Difficulty.MEDIUM;
      }
    };
  }

  private boolean validateQuestion(Question question) {
    if (question.getQuestionText() == null || question.getQuestionText().trim().isEmpty()) {
      logger.warn("Question has empty text");
      return false;
    }

    if (question.getDifficulty() == null) {
      logger.warn("Question has no difficulty level");
      return false;
    }

    // Note: QuestionOption entities are created but not in a collection yet
    // Validation of options count and correct answer will happen when persisted
    return true;
  }
}
