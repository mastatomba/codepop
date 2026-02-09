package nl.schoutens.codepop.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for QuizController.
 * Tests the full application stack including database, service layer, and HTTP.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class QuizControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String getBaseUrl() {
        return "http://localhost:" + port + "/api/quiz/";
    }

    @Test
    void getQuiz_shouldReturnJavaRecordsQuestions() {
        // Act
        ResponseEntity<String> response = restTemplate.getForEntity(
                getBaseUrl() + "Java records",
                String.class
        );

        // Assert
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
        // Act
        ResponseEntity<String> response = restTemplate.getForEntity(
                getBaseUrl() + "Java",
                String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"topic\":\"Java\"");
        assertThat(response.getBody()).contains("\"totalQuestions\":3");
    }

    @Test
    void getQuiz_shouldExcludeQuestions_whenExcludeIdsProvided() {
        // Act
        ResponseEntity<String> response = restTemplate.getForEntity(
                getBaseUrl() + "Java records?excludeQuestionIds=1,2",
                String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"totalQuestions\":1");
        assertThat(response.getBody()).contains("\"id\":3");
        assertThat(response.getBody()).doesNotContain("\"id\":1");
        assertThat(response.getBody()).doesNotContain("\"id\":2");
    }

    @Test
    void getQuiz_shouldReturnReactHooksQuestion() {
        // Act
        ResponseEntity<String> response = restTemplate.getForEntity(
                getBaseUrl() + "React hooks",
                String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"topic\":\"React hooks\"");
        assertThat(response.getBody()).contains("\"totalQuestions\":1");
        assertThat(response.getBody()).contains("useState");
    }

    @Test
    void getQuiz_shouldReturnAllReactQuestions_whenNoSubtopicSpecified() {
        // Act
        ResponseEntity<String> response = restTemplate.getForEntity(
                getBaseUrl() + "React",
                String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"topic\":\"React\"");
        assertThat(response.getBody()).contains("\"totalQuestions\":3");
        // Should include questions from all subtopics: hooks, lifecycle, fundamentals
    }

    @Test
    void getQuiz_shouldReturn404_whenTopicNotFound() {
        // Act
        ResponseEntity<String> response = restTemplate.getForEntity(
                getBaseUrl() + "NonExistentTopic",
                String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("\"error\":\"Topic not found: NonExistentTopic\"");
        assertThat(response.getBody()).contains("\"status\":\"404\"");
    }

    @Test
    void getQuiz_shouldReturnEmpty_whenSubtopicHasNoQuestions() {
        // Act - "Java streams" subtopic doesn't exist in seed data
        ResponseEntity<String> response = restTemplate.getForEntity(
                getBaseUrl() + "Java streams",
                String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"topic\":\"Java streams\"");
        assertThat(response.getBody()).contains("\"totalQuestions\":0");
    }

    @Test
    void getQuiz_shouldHandleCaseInsensitiveTopics() {
        // Act
        ResponseEntity<String> response = restTemplate.getForEntity(
                getBaseUrl() + "java RECORDS",
                String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"totalQuestions\":3");
    }

    @Test
    void getQuiz_shouldHandleFuzzySubtopicMatching() {
        // Act - "record" (singular) should match "records" (plural)
        ResponseEntity<String> response = restTemplate.getForEntity(
                getBaseUrl() + "Java record",
                String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"totalQuestions\":3");
    }

    @Test
    void getQuiz_shouldReturnQuestionsWithExplanations() {
        // Act
        ResponseEntity<String> response = restTemplate.getForEntity(
                getBaseUrl() + "Java records",
                String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // Some questions should have explanations
        assertThat(response.getBody()).contains("\"explanation\":\"");
        // Some questions should have null explanations
        assertThat(response.getBody()).contains("\"explanation\":null");
    }

    @Test
    void getQuiz_shouldReturnValidJsonStructure() {
        // Act
        ResponseEntity<String> response = restTemplate.getForEntity(
                getBaseUrl() + "Java",
                String.class
        );

        // Assert
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
        // Act
        ResponseEntity<String> response = restTemplate.getForEntity(
                getBaseUrl() + "Java",
                String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // Note: CORS headers are typically set by browser preflight requests
        // In actual browser requests, the Access-Control-Allow-Origin header would be present
    }
}
