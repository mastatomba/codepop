package nl.schoutens.codepop.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.List;
import nl.schoutens.codepop.entity.Question;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.Builder;

/**
 * Unit tests for OllamaQuizMaster's JSON extraction logic. Tests the robustness of parsing LLM
 * responses that may contain code snippets with special characters.
 */
class OllamaQuizMasterTest {

  private OllamaQuizMaster quizMaster;

  @BeforeEach
  void setUp() {
    Builder chatClientBuilder = mock(ChatClient.Builder.class, RETURNS_DEEP_STUBS);

    when(chatClientBuilder.defaultAdvisors(any(List.class))).thenReturn(chatClientBuilder);
    when(chatClientBuilder.defaultOptions(any())).thenReturn(chatClientBuilder);
    when(chatClientBuilder.build()).thenReturn(mock(ChatClient.class));

    quizMaster = new OllamaQuizMaster(chatClientBuilder);
  }

  @Test
  void testExtractJson_simpleJson() throws Exception {
    String response = "{\"questions\": []}";
    String result = invokeExtractJson(response);
    assertEquals("{\"questions\": []}", result);
  }

  @Test
  void testExtractJson_withMarkdownCodeBlock() throws Exception {
    String response = "```json\n{\"questions\": []}\n```";
    String result = invokeExtractJson(response);
    assertEquals("{\"questions\": []}", result);
  }

  @Test
  void testExtractJson_withExplanatoryText() throws Exception {
    String response = "Here's the JSON:\n{\"questions\": []}\nHope this helps!";
    String result = invokeExtractJson(response);
    assertEquals("{\"questions\": []}", result);
  }

  @Test
  void testExtractJson_withNestedBraces() throws Exception {
    String response = "{\"outer\": {\"inner\": \"value\"}}";
    String result = invokeExtractJson(response);
    assertEquals("{\"outer\": {\"inner\": \"value\"}}", result);
  }

  @Test
  void testExtractJson_withCodeSnippetInString() throws Exception {
    String response =
        "{\"question\": \"What does `if (x > 0) { return true; }` do?\", \"answer\": \"checks x\"}";
    String result = invokeExtractJson(response);
    assertNotNull(result);
    assertTrue(result.contains("if (x > 0) { return true; }"));
  }

  @Test
  void testExtractJson_withEscapedQuotes() throws Exception {
    String response = "{\"question\": \"What is \\\"escaping\\\"?\"}";
    String result = invokeExtractJson(response);
    assertNotNull(result);
    assertTrue(result.contains("\\\"escaping\\\""));
  }

  @Test
  void testExtractJson_withMarkdownCodeBlockInQuestion() throws Exception {
    String response =
        "{\"question\": \"Code:\\n```java\\nif (x > 0) { return true; }\\n```\", \"answer\": \"condition\"}";
    String result = invokeExtractJson(response);
    assertNotNull(result);
    assertTrue(result.contains("```java"));
  }

  @Test
  void testExtractJson_emptyResponse() throws Exception {
    String response = "";
    String result = invokeExtractJson(response);
    assertNull(result);
  }

  @Test
  void testExtractJson_noOpeningBrace() throws Exception {
    String response = "No JSON here!";
    String result = invokeExtractJson(response);
    assertNull(result);
  }

  @Test
  void testExtractJson_unmatchedBraces() throws Exception {
    String response = "{\"questions\": [";
    String result = invokeExtractJson(response);
    assertNull(result);
  }

  @Test
  void testParseResponse_validJson() throws Exception {
    String validJson =
        """
        {
          "questions": [
            {
              "question": "What is Java?",
              "options": ["A language", "A drink", "An island", "A framework"],
              "correct_index": 0,
              "difficulty": "easy",
              "explanation": "Java is a programming language"
            }
          ]
        }
        """;

    List<Question> questions = invokeParseResponse(validJson);
    assertEquals(1, questions.size());
    assertEquals("What is Java?", questions.get(0).getQuestionText());
    assertEquals(4, questions.get(0).getOptions().size());
  }

  @Test
  void testParseResponse_withCodeSnippet() throws Exception {
    String jsonWithCode =
        """
        {
          "questions": [
            {
              "question": "What does `System.out.println(\\"Hello\\");` do?",
              "options": ["Prints Hello", "Throws error", "Does nothing", "Compiles error"],
              "correct_index": 0,
              "difficulty": "easy",
              "explanation": "It prints Hello to console"
            }
          ]
        }
        """;

    List<Question> questions = invokeParseResponse(jsonWithCode);
    assertEquals(1, questions.size());
    assertTrue(questions.get(0).getQuestionText().contains("System.out.println"));
  }

  @Test
  void testParseResponse_invalidJson() throws Exception {
    String invalidJson = "This is not JSON";
    List<Question> questions = invokeParseResponse(invalidJson);
    assertTrue(questions.isEmpty());
  }

  // ==================== Delimiter Format Tests ====================

  @Test
  void testParseDelimitedFormat_singleQuestion() throws Exception {
    String delimitedResponse =
        """
        ### QUESTION 1 ###
        DIFFICULTY: easy
        QUESTION: What is Java?
        OPTION: A programming language [CORRECT]
        OPTION: A drink
        OPTION: An island
        OPTION: A framework
        EXPLANATION: Java is a high-level programming language
        """;

    List<Question> questions = invokeParseDelimitedFormat(delimitedResponse);
    assertEquals(1, questions.size());
    assertEquals("What is Java?", questions.get(0).getQuestionText());
    assertEquals(4, questions.get(0).getOptions().size());
    assertTrue(questions.get(0).getOptions().get(0).getIsCorrect());
  }

  @Test
  void testParseDelimitedFormat_multipleQuestions() throws Exception {
    String delimitedResponse =
        """
        ### QUESTION 1 ###
        DIFFICULTY: easy
        QUESTION: What is 2+2?
        OPTION: 4 [CORRECT]
        OPTION: 3
        OPTION: 5
        OPTION: 22
        EXPLANATION: Basic arithmetic

        ### QUESTION 2 ###
        DIFFICULTY: medium
        QUESTION: What is recursion?
        OPTION: A function calling itself [CORRECT]
        OPTION: A loop
        OPTION: An error
        OPTION: A data structure
        EXPLANATION: Recursion is when a function calls itself
        """;

    List<Question> questions = invokeParseDelimitedFormat(delimitedResponse);
    assertEquals(2, questions.size());
  }

  @Test
  void testParseDelimitedFormat_withCodeSnippet() throws Exception {
    String delimitedResponse =
        """
        ### QUESTION 1 ###
        DIFFICULTY: medium
        QUESTION: What does this code print?
        ```java
        public class Test {
          public static void main(String[] args) {
            System.out.println("Hello, World!");
          }
        }
        ```
        OPTION: Hello, World! [CORRECT]
        OPTION: Error
        OPTION: Nothing
        OPTION: Test
        EXPLANATION: The code prints "Hello, World!" to the console
        """;

    List<Question> questions = invokeParseDelimitedFormat(delimitedResponse);
    assertEquals(1, questions.size());
    assertTrue(questions.get(0).getQuestionText().contains("```java"));
    assertTrue(questions.get(0).getQuestionText().contains("System.out.println"));
  }

  @Test
  void testParseDelimitedFormat_withSpecialCharacters() throws Exception {
    String delimitedResponse =
        """
        ### QUESTION 1 ###
        DIFFICULTY: easy
        QUESTION: What does `System.out.println("Hello");` do?
        OPTION: Prints "Hello" [CORRECT]
        OPTION: Throws an error
        OPTION: Does nothing
        OPTION: Compiles error
        EXPLANATION: It prints the string "Hello" to standard output
        """;

    List<Question> questions = invokeParseDelimitedFormat(delimitedResponse);
    assertEquals(1, questions.size());
    assertTrue(questions.get(0).getQuestionText().contains("\"Hello\""));
  }

  @Test
  void testParseDelimitedFormat_emptyResponse() throws Exception {
    String delimitedResponse = "";
    List<Question> questions = invokeParseDelimitedFormat(delimitedResponse);
    assertTrue(questions.isEmpty());
  }

  @Test
  void testParseDelimitedFormat_missingFields() throws Exception {
    String delimitedResponse =
        """
        ### QUESTION 1 ###
        DIFFICULTY: easy
        OPTION: Answer 1 [CORRECT]
        OPTION: Answer 2
        """;

    List<Question> questions = invokeParseDelimitedFormat(delimitedResponse);
    assertTrue(questions.isEmpty(), "Should skip question with missing QUESTION field");
  }

  @Test
  void testParseResponse_prefersDelimiterFormat() throws Exception {
    String delimitedResponse =
        """
        ### QUESTION 1 ###
        DIFFICULTY: easy
        QUESTION: Test question
        OPTION: Answer [CORRECT]
        OPTION: Wrong
        OPTION: Wrong2
        OPTION: Wrong3
        EXPLANATION: Test explanation
        """;

    List<Question> questions = invokeParseResponse(delimitedResponse);
    assertEquals(1, questions.size());
    assertEquals("Test question", questions.get(0).getQuestionText());
  }

  @Test
  void testParseResponse_fallsBackToJson() throws Exception {
    String jsonResponse =
        """
        {
          "questions": [{
            "question": "JSON format test",
            "options": ["A", "B", "C", "D"],
            "correct_index": 0,
            "difficulty": "easy",
            "explanation": "Testing JSON fallback"
          }]
        }
        """;

    List<Question> questions = invokeParseResponse(jsonResponse);
    assertEquals(1, questions.size());
    assertEquals("JSON format test", questions.get(0).getQuestionText());
  }

  // Helper methods to invoke private methods using reflection
  private String invokeExtractJson(String response) throws Exception {
    Method method = OllamaQuizMaster.class.getDeclaredMethod("extractJson", String.class);
    method.setAccessible(true);
    return (String) method.invoke(quizMaster, response);
  }

  @SuppressWarnings("unchecked")
  private List<Question> invokeParseResponse(String response) throws Exception {
    Method method = OllamaQuizMaster.class.getDeclaredMethod("parseResponse", String.class);
    method.setAccessible(true);
    return (List<Question>) method.invoke(quizMaster, response);
  }

  @SuppressWarnings("unchecked")
  private List<Question> invokeParseDelimitedFormat(String response) throws Exception {
    Method method = OllamaQuizMaster.class.getDeclaredMethod("parseDelimitedFormat", String.class);
    method.setAccessible(true);
    return (List<Question>) method.invoke(quizMaster, response);
  }
}
