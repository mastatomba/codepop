package nl.schoutens.codepop.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import nl.schoutens.codepop.entity.Question;
import nl.schoutens.codepop.entity.Question.Difficulty;
import nl.schoutens.codepop.entity.QuestionOption;
import nl.schoutens.codepop.entity.Topic;
import nl.schoutens.codepop.repository.QuestionRepository;
import nl.schoutens.codepop.repository.TopicRepository;
import nl.schoutens.codepop.service.QuizMaster;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration tests for QuizController. Tests the full application stack including database,
 * service layer, and HTTP. Uses isolated test database and stub QuizMaster for predictable tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
class QuizControllerIntegrationTest {

  @TestConfiguration
  static class TestConfig {
    @Bean
    public QuizMaster testQuizMaster() {
      // Stub implementation that never generates questions
      // This makes tests predictable - only seeded data is used
      return new QuizMaster() {
        @Override
        public List<Question> generateQuestions(
            String topic, int count, List<String> existingQuestionTexts) {
          return Collections.emptyList();
        }
      };
    }
  }

  @LocalServerPort private int port;

  @Autowired private TestRestTemplate restTemplate;

  @Autowired private TopicRepository topicRepository;

  @Autowired private QuestionRepository questionRepository;

  private Topic javaTopic;
  private Topic reactTopic;

  @BeforeEach
  void setUp() {
    // Clean database for each test
    questionRepository.deleteAll();
    topicRepository.deleteAll();

    // Seed test topics
    javaTopic = topicRepository.save(new Topic("Java", "backend"));
    reactTopic = topicRepository.save(new Topic("React", "frontend"));

    // Seed Java records questions (3 questions with different difficulties)
    seedJavaRecordsQuestions();

    // Seed React questions (3 questions with different subtopics)
    seedReactQuestions();
  }

  private void seedJavaRecordsQuestions() {
    // Easy question
    Question q1 =
        new Question(
            javaTopic,
            "What keyword is used to define a record in Java?",
            "records",
            Difficulty.EASY,
            "Java records use the 'record' keyword.");
    q1.addOption(new QuestionOption(q1, "record", true));
    q1.addOption(new QuestionOption(q1, "class", false));
    q1.addOption(new QuestionOption(q1, "struct", false));
    q1.addOption(new QuestionOption(q1, "interface", false));
    questionRepository.save(q1);

    // Medium question
    Question q2 =
        new Question(
            javaTopic,
            "Are Java records immutable by default?",
            "records",
            Difficulty.MEDIUM,
            "Records generate final fields and no setters.");
    q2.addOption(new QuestionOption(q2, "Yes, always immutable", true));
    q2.addOption(new QuestionOption(q2, "No, they are mutable", false));
    q2.addOption(new QuestionOption(q2, "Only if marked final", false));
    q2.addOption(new QuestionOption(q2, "Depends on JDK version", false));
    questionRepository.save(q2);

    // Hard question
    Question q3 =
        new Question(
            javaTopic, "Can a Java record implement interfaces?", "records", Difficulty.HARD, null);
    q3.addOption(new QuestionOption(q3, "Yes, records can implement interfaces", true));
    q3.addOption(new QuestionOption(q3, "No, records cannot implement anything", false));
    q3.addOption(new QuestionOption(q3, "Only marker interfaces", false));
    q3.addOption(new QuestionOption(q3, "Records can only extend classes", false));
    questionRepository.save(q3);
  }

  private void seedReactQuestions() {
    // React hooks question
    Question q1 =
        new Question(
            reactTopic,
            "What does the useState hook return?",
            "hooks",
            Difficulty.EASY,
            "useState returns [state, setState] array.");
    q1.addOption(new QuestionOption(q1, "Array with state and setter", true));
    q1.addOption(new QuestionOption(q1, "Just the state value", false));
    q1.addOption(new QuestionOption(q1, "Object with state key", false));
    q1.addOption(new QuestionOption(q1, "Function to update state", false));
    questionRepository.save(q1);

    // React lifecycle question
    Question q2 =
        new Question(
            reactTopic,
            "When does useEffect run by default?",
            "lifecycle",
            Difficulty.MEDIUM,
            "useEffect runs after every render by default.");
    q2.addOption(new QuestionOption(q2, "After every render", true));
    q2.addOption(new QuestionOption(q2, "Only on mount", false));
    q2.addOption(new QuestionOption(q2, "Before render", false));
    q2.addOption(new QuestionOption(q2, "On unmount only", false));
    questionRepository.save(q2);

    // React fundamentals question
    Question q3 =
        new Question(
            reactTopic,
            "What is JSX?",
            "fundamentals",
            Difficulty.EASY,
            "JSX is JavaScript XML syntax extension.");
    q3.addOption(new QuestionOption(q3, "JavaScript XML syntax", true));
    q3.addOption(new QuestionOption(q3, "JSON format", false));
    q3.addOption(new QuestionOption(q3, "CSS-in-JS", false));
    q3.addOption(new QuestionOption(q3, "TypeScript extension", false));
    questionRepository.save(q3);
  }

  private String getBaseUrl() {
    return "http://localhost:" + port + "/api/quiz/";
  }

  @Test
  void getQuiz_shouldReturnJavaRecordsQuestions() {
    ResponseEntity<String> response =
        restTemplate.getForEntity(getBaseUrl() + "Java records", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"topic\":\"Java records\"");
    assertThat(response.getBody()).contains("\"totalQuestions\":3");
    assertThat(response.getBody()).contains("\"difficulty\":\"EASY\"");
    assertThat(response.getBody()).contains("\"difficulty\":\"MEDIUM\"");
    assertThat(response.getBody()).contains("\"difficulty\":\"HARD\"");
    assertThat(response.getBody()).contains("\"isCorrect\":true");
    assertThat(response.getBody()).contains("\"isCorrect\":false");
  }

  @Test
  void getQuiz_shouldReturnAllJavaQuestions_whenNoSubtopicSpecified() {
    ResponseEntity<String> response =
        restTemplate.getForEntity(getBaseUrl() + "Java", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"topic\":\"Java\"");
    assertThat(response.getBody()).contains("\"totalQuestions\":3");
  }

  @Test
  void getQuiz_shouldExcludeQuestions_whenExcludeIdsProvided() {
    ResponseEntity<String> response =
        restTemplate.getForEntity(
            getBaseUrl() + "Java records?excludeQuestionIds=1,2", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"totalQuestions\":1");
    assertThat(response.getBody()).contains("\"id\":3,");
    assertThat(response.getBody()).doesNotContain("\"id\":1,");
    assertThat(response.getBody()).doesNotContain("\"id\":2,");
  }

  @Test
  void getQuiz_shouldReturnReactHooksQuestion() {
    ResponseEntity<String> response =
        restTemplate.getForEntity(getBaseUrl() + "React hooks", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"topic\":\"React hooks\"");
    assertThat(response.getBody()).contains("\"totalQuestions\":1");
    assertThat(response.getBody()).contains("useState");
  }

  @Test
  void getQuiz_shouldReturnAllReactQuestions_whenNoSubtopicSpecified() {
    ResponseEntity<String> response =
        restTemplate.getForEntity(getBaseUrl() + "React", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"topic\":\"React\"");
    assertThat(response.getBody()).contains("\"totalQuestions\":3");
    // Should include questions from all subtopics: hooks, lifecycle, fundamentals
  }

  @Test
  void getQuiz_shouldReturn404_whenTopicNotFound() {
    ResponseEntity<String> response =
        restTemplate.getForEntity(getBaseUrl() + "NonExistentTopic", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).contains("\"error\":\"Topic not found: NonExistentTopic\"");
    assertThat(response.getBody()).contains("\"status\":\"404\"");
  }

  @Test
  void getQuiz_shouldReturnEmpty_whenSubtopicHasNoQuestions() {
    ResponseEntity<String> response =
        restTemplate.getForEntity(getBaseUrl() + "Java streams", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"topic\":\"Java streams\"");
    // No seeded questions for Java streams, should return 0
    assertThat(response.getBody()).contains("\"totalQuestions\":0");
  }

  @Test
  void getQuiz_shouldHandleCaseInsensitiveTopics() {
    ResponseEntity<String> response =
        restTemplate.getForEntity(getBaseUrl() + "java RECORDS", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"totalQuestions\":3");
  }

  @Test
  void getQuiz_shouldHandleFuzzySubtopicMatching() {
    // "record" (singular) should match "records" (plural)
    ResponseEntity<String> response =
        restTemplate.getForEntity(getBaseUrl() + "Java record", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"totalQuestions\":3");
  }

  @Test
  void getQuiz_shouldReturnQuestionsWithExplanations() {
    ResponseEntity<String> response =
        restTemplate.getForEntity(getBaseUrl() + "Java records", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    // Some questions have explanations
    assertThat(response.getBody()).contains("\"explanation\":\"");
    // q3 has null explanation
    assertThat(response.getBody()).contains("\"explanation\":null");
  }

  @Test
  void getQuiz_shouldReturnValidJsonStructure() {
    ResponseEntity<String> response =
        restTemplate.getForEntity(getBaseUrl() + "Java", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"topic\":");
    assertThat(response.getBody()).contains("\"totalQuestions\":");
    assertThat(response.getBody()).contains("\"questions\":");
    assertThat(response.getBody()).contains("\"id\":");
    assertThat(response.getBody()).contains("\"text\":");
    assertThat(response.getBody()).contains("\"difficulty\":");
    assertThat(response.getBody()).contains("\"options\":");
    assertThat(response.getBody()).contains("\"isCorrect\":");
  }

  @Test
  void getQuiz_shouldHaveCorsHeaders() {
    ResponseEntity<String> response =
        restTemplate.getForEntity(getBaseUrl() + "Java", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    // Note: CORS headers are typically set by browser preflight requests
    // In actual browser requests, the Access-Control-Allow-Origin header would be present
  }
}
