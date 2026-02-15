package nl.schoutens.codepop.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import nl.schoutens.codepop.dto.OptionDTO;
import nl.schoutens.codepop.dto.QuestionDTO;
import nl.schoutens.codepop.dto.QuizDTO;
import nl.schoutens.codepop.entity.Question;
import nl.schoutens.codepop.entity.Topic;
import nl.schoutens.codepop.repository.QuestionRepository;
import nl.schoutens.codepop.repository.TopicRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class QuizService {

  private final TopicRepository topicRepository;
  private final QuestionRepository questionRepository;
  private final QuizMaster quizMaster;
  private final TransactionalOperations txOps;

  public QuizService(
      TopicRepository topicRepository,
      QuestionRepository questionRepository,
      QuizMaster quizMaster,
      TransactionalOperations txOps) {
    this.topicRepository = topicRepository;
    this.questionRepository = questionRepository;
    this.quizMaster = quizMaster;
    this.txOps = txOps;
  }

  /**
   * Inner component for transactional operations. Spring can proxy this as a separate bean,
   * making @Transactional work properly.
   */
  @Component
  public static class TransactionalOperations {
    private final QuestionRepository questionRepository;
    private final TopicRepository topicRepository;

    public TransactionalOperations(
        QuestionRepository questionRepository, TopicRepository topicRepository) {
      this.questionRepository = questionRepository;
      this.topicRepository = topicRepository;
    }

    @Transactional(readOnly = true)
    public List<Question> fetchQuestions(Long topicId, String subtopic) {
      if (subtopic != null && !subtopic.isEmpty()) {
        return questionRepository.findByTopicIdAndSubtopicContainingIgnoreCase(topicId, subtopic);
      }
      return questionRepository.findByTopicId(topicId);
    }

    @Transactional(readOnly = true)
    public Topic findTopicByName(String name) {
      return topicRepository.findByNameIgnoreCase(name).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<Topic> findAllTopics() {
      return topicRepository.findAll();
    }

    @Transactional
    public void saveQuestions(List<Question> questions) {
      questionRepository.saveAll(questions);
    }
  }

  public QuizDTO getQuiz(String userInput, List<Long> excludeQuestionIds) {
    log.info(
        "[QuizService] getQuiz called with topic: "
            + userInput
            + ", excludeQuestionIds: "
            + excludeQuestionIds);

    // Parse user input and find topic (short read transaction)
    ParsedTopicResult parsed = parseAndFindTopic(userInput);
    if (parsed.topic == null) {
      throw new IllegalArgumentException("Topic not found: " + userInput);
    }

    // 1. Fetch existing questions (short read transaction)
    List<Question> allQuestions = txOps.fetchQuestions(parsed.topic.getId(), parsed.subtopic);
    log.info("[QuizService] Found " + allQuestions.size() + " total questions");

    // 2. Filter out excluded questions (user already saw these)
    List<Question> availableQuestions =
        allQuestions.stream()
            .filter(q -> excludeQuestionIds == null || !excludeQuestionIds.contains(q.getId()))
            .collect(Collectors.toList());
    log.info(
        "[QuizService] After filtering: " + availableQuestions.size() + " available questions");

    // 3. Check if we need more questions
    if (availableQuestions.size() < 5) {
      // Extract ALL question texts (including excluded ones) so LLM doesn't duplicate
      List<String> existingQuestionTexts =
          allQuestions.stream().map(Question::getQuestionText).collect(Collectors.toList());

      log.info(
          "[QuizService] Passing "
              + existingQuestionTexts.size()
              + " existing question texts to LLM to avoid duplicates");

      int neededCount = 5 - availableQuestions.size();

      // Call LLM to generate new questions (NO TRANSACTION - can take as long as needed)
      List<Question> newQuestions =
          quizMaster.generateQuestions(userInput, neededCount, existingQuestionTexts);

      // Save new questions (short write transaction)
      if (!newQuestions.isEmpty()) {
        for (Question newQ : newQuestions) {
          newQ.setTopic(parsed.topic);
          if (parsed.subtopic != null) {
            newQ.setSubtopic(parsed.subtopic);
          }
        }
        txOps.saveQuestions(newQuestions);

        // Re-fetch all questions (short read transaction)
        allQuestions = txOps.fetchQuestions(parsed.topic.getId(), parsed.subtopic);
        availableQuestions =
            allQuestions.stream()
                .filter(q -> excludeQuestionIds == null || !excludeQuestionIds.contains(q.getId()))
                .collect(Collectors.toList());
      }
    }

    // 4. Randomize and select up to 5 questions
    List<Question> selectedQuestions;
    if (availableQuestions.size() > 5) {
      Collections.shuffle(availableQuestions);
      selectedQuestions = availableQuestions.subList(0, 5);
    } else {
      selectedQuestions = availableQuestions;
    }

    // 5. Convert to DTOs
    List<QuestionDTO> questionDTOs =
        selectedQuestions.stream().map(this::convertToQuestionDTO).collect(Collectors.toList());

    return new QuizDTO(userInput, questionDTOs.size(), questionDTOs);
  }

  /**
   * Parse user input and find the matching topic in one operation. Avoids duplicate database
   * queries by combining parsing and lookup.
   *
   * <p>Strategy: 1. Try to match each word in input against topics (exact match) 2. If no match,
   * try fuzzy matching on the full input 3. Return the found topic along with extracted subtopic
   *
   * <p>Examples: "Java records" -> Topic(Java), subtopic="records" "React hooks" -> Topic(React),
   * subtopic="hooks" "Javascript React" -> Topic(React), subtopic="Javascript" "Java" ->
   * Topic(Java), subtopic=null
   */
  private ParsedTopicResult parseAndFindTopic(String input) {
    String normalized = input.trim();
    String[] words = normalized.split("\\s+");

    // Strategy 1: Check if any word is an exact topic match
    for (int i = 0; i < words.length; i++) {
      Topic topic = txOps.findTopicByName(words[i]);
      if (topic != null) {
        // Found main topic, extract subtopic from remaining words
        String subtopic = extractSubtopic(words, i);
        return new ParsedTopicResult(topic, subtopic);
      }
    }

    // Strategy 2: Try fuzzy matching on full input
    Topic topic = fuzzyFindTopic(normalized);
    if (topic != null) {
      // Fuzzy match found, no subtopic extraction
      return new ParsedTopicResult(topic, null);
    }

    // Strategy 3: Try fuzzy matching on first word
    if (words.length > 1) {
      topic = fuzzyFindTopic(words[0]);
      if (topic != null) {
        // Found topic in first word, rest is subtopic
        String subtopic = String.join(" ", Arrays.copyOfRange(words, 1, words.length));
        return new ParsedTopicResult(topic, subtopic);
      }
    }

    // No topic found
    return new ParsedTopicResult(null, null);
  }

  /** Extract subtopic from words array, excluding the word at topicIndex. */
  private String extractSubtopic(String[] words, int topicIndex) {
    if (words.length == 1) {
      return null;
    }

    List<String> remaining = new ArrayList<>();
    for (int j = 0; j < words.length; j++) {
      if (j != topicIndex) {
        remaining.add(words[j]);
      }
    }

    return remaining.isEmpty() ? null : String.join(" ", remaining);
  }

  /** Fuzzy match: check if input contains topic name or vice versa. */
  private Topic fuzzyFindTopic(String input) {
    List<Topic> allTopics = txOps.findAllTopics();
    String inputLower = input.toLowerCase();

    for (Topic t : allTopics) {
      String topicLower = t.getName().toLowerCase();
      if (inputLower.contains(topicLower) || topicLower.contains(inputLower)) {
        return t;
      }
    }

    return null;
  }

  /** Helper class to return both topic and subtopic from parsing. */
  private static class ParsedTopicResult {
    final Topic topic;
    final String subtopic;

    ParsedTopicResult(Topic topic, String subtopic) {
      this.topic = topic;
      this.subtopic = subtopic;
    }
  }

  /**
   * Fetch questions for a topic, optionally filtered by subtopic. - If subtopic provided: only
   * return questions matching that subtopic (may be empty, triggering LLM) - If no subtopic: return
   * all questions for main topic
   */
  private QuestionDTO convertToQuestionDTO(Question question) {
    List<OptionDTO> optionDTOs =
        question.getOptions().stream()
            .map(opt -> new OptionDTO(opt.getId(), opt.getOptionText(), opt.getIsCorrect()))
            .collect(Collectors.toList());

    return new QuestionDTO(
        question.getId(),
        question.getQuestionText(),
        question.getDifficulty().name(),
        question.getExplanation(),
        optionDTOs);
  }
}
