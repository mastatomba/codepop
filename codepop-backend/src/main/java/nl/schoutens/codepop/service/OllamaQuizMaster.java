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
    prompt.append("- Exactly one option must be correct\n");
    prompt.append("- Create factual, verifiable questions (no opinions or ambiguous questions)\n");
    prompt.append("- Include explanations for each question\n");
    prompt.append("- Make the incorrect options plausible distractors\n");
    prompt.append(
        "- ENCOURAGED: Include code snippets in questions (e.g., 'What is the output of: int x = 5;')\n");
    prompt.append(
        "- When including code, use proper JSON string escaping (escape quotes and special characters)\n\n");

    if (existingQuestionTexts != null && !existingQuestionTexts.isEmpty()) {
      prompt.append("IMPORTANT: Avoid generating questions similar to these existing ones:\n");
      for (int i = 0; i < Math.min(existingQuestionTexts.size(), 10); i++) {
        prompt.append("- ").append(existingQuestionTexts.get(i)).append("\n");
      }
      prompt.append("\n");
    }

    prompt.append(
        "CRITICAL: Return ONLY raw JSON. Do not wrap the ENTIRE response in markdown ```.\n");
    prompt.append("Do not include explanatory text outside the JSON.\n");
    prompt.append("Your response must start with { and end with }\n\n");
    prompt.append(
        "HOWEVER: You SHOULD use markdown code blocks INSIDE question strings for better readability.\n");
    prompt.append(
        "For multi-line code, use escaped newlines (\\n) and code blocks (```language\\ncode\\n```).\n\n");
    prompt.append("Format examples:\n");
    prompt.append("{\n");
    prompt.append("  \"questions\": [\n");
    prompt.append("    {\n");
    prompt.append("      \"question\": \"What is the output of: print(2 + 2)?\",\n");
    prompt.append("      \"options\": [\"4\", \"22\", \"TypeError\", \"SyntaxError\"],\n");
    prompt.append("      \"correct_index\": 0,\n");
    prompt.append("      \"difficulty\": \"easy\",\n");
    prompt.append("      \"explanation\": \"Basic arithmetic\"\n");
    prompt.append("    },\n");
    prompt.append("    {\n");
    prompt.append(
        "      \"question\": \"What does this method return?\\n```java\\npublic int add(int a, int b) {\\n  return a + b;\\n}\\nadd(2, 3)\\n```\",\n");
    prompt.append("      \"options\": [\"5\", \"23\", \"Error\", \"null\"],\n");
    prompt.append("      \"correct_index\": 0,\n");
    prompt.append("      \"difficulty\": \"easy\",\n");
    prompt.append("      \"explanation\": \"The method adds two integers\"\n");
    prompt.append("    }\n");
    prompt.append("  ]\n");
    prompt.append("}\n");

    return prompt.toString();
  }

  private List<Question> parseResponse(String response) {
    List<Question> questions = new ArrayList<>();

    // Log raw response for debugging
    logger.debug("Raw LLM response length: {} chars", response.length());

    // Clean response - try to extract JSON from various formats
    String cleanedResponse = extractJson(response);

    // Log cleaned response for debugging
    if (cleanedResponse == null || cleanedResponse.isEmpty()) {
      logger.error(
          "Failed to extract JSON from LLM response. First 200 chars: {}",
          response.length() > 200 ? response.substring(0, 200) : response);
      return questions;
    }

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
   * Extract JSON from LLM response, handling various formats: - Plain JSON - Markdown code blocks
   * (```json or ```) - JSON embedded in explanatory text
   */
  private String extractJson(String response) {
    if (response == null || response.isEmpty()) {
      return null;
    }

    String cleaned = response.trim();

    // Strategy 1: Remove markdown code blocks
    if (cleaned.startsWith("```json")) {
      cleaned = cleaned.substring(7);
    } else if (cleaned.startsWith("```")) {
      cleaned = cleaned.substring(3);
    }
    if (cleaned.endsWith("```")) {
      cleaned = cleaned.substring(0, cleaned.length() - 3);
    }
    cleaned = cleaned.trim();

    // Strategy 2: Try to find JSON object boundaries if embedded in text
    int jsonStart = cleaned.indexOf('{');
    int jsonEnd = cleaned.lastIndexOf('}');

    if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
      cleaned = cleaned.substring(jsonStart, jsonEnd + 1);
    }

    return cleaned.trim();
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
