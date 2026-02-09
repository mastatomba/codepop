package nl.schoutens.codepop.controller;

import nl.schoutens.codepop.dto.OptionDTO;
import nl.schoutens.codepop.dto.QuestionDTO;
import nl.schoutens.codepop.dto.QuizDTO;
import nl.schoutens.codepop.service.QuizService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for QuizController.
 * Uses @WebMvcTest to test only the web layer with mocked service.
 */
@WebMvcTest(QuizController.class)
class QuizControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QuizService quizService;

    @Test
    void getQuiz_shouldReturn200WithQuizData_whenTopicExists() throws Exception {
        // Arrange
        List<OptionDTO> options = Arrays.asList(
                new OptionDTO(1L, "Option A", true),
                new OptionDTO(2L, "Option B", false)
        );
        
        List<QuestionDTO> questions = Arrays.asList(
                new QuestionDTO(1L, "Question 1?", "EASY", "Explanation 1", options),
                new QuestionDTO(2L, "Question 2?", "MEDIUM", null, options)
        );
        
        QuizDTO quizDTO = new QuizDTO("Java", 2, questions);
        
        when(quizService.getQuiz(eq("Java"), isNull())).thenReturn(quizDTO);

        // Act & Assert
        mockMvc.perform(get("/api/quiz/Java")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.topic").value("Java"))
                .andExpect(jsonPath("$.totalQuestions").value(2))
                .andExpect(jsonPath("$.questions.length()").value(2))
                .andExpect(jsonPath("$.questions[0].id").value(1))
                .andExpect(jsonPath("$.questions[0].text").value("Question 1?"))
                .andExpect(jsonPath("$.questions[0].difficulty").value("EASY"))
                .andExpect(jsonPath("$.questions[0].explanation").value("Explanation 1"))
                .andExpect(jsonPath("$.questions[0].options.length()").value(2))
                .andExpect(jsonPath("$.questions[0].options[0].isCorrect").value(true))
                .andExpect(jsonPath("$.questions[1].explanation").doesNotExist());

        verify(quizService).getQuiz("Java", null);
    }

    @Test
    void getQuiz_shouldReturn200WithExcludedQuestions_whenExcludeIdsProvided() throws Exception {
        // Arrange
        List<OptionDTO> options = List.of(new OptionDTO(1L, "Option A", true));
        List<QuestionDTO> questions = List.of(
                new QuestionDTO(3L, "Question 3?", "HARD", null, options)
        );
        QuizDTO quizDTO = new QuizDTO("React", 1, questions);
        
        when(quizService.getQuiz(eq("React"), eq(Arrays.asList(1L, 2L)))).thenReturn(quizDTO);

        // Act & Assert
        mockMvc.perform(get("/api/quiz/React")
                        .param("excludeQuestionIds", "1", "2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topic").value("React"))
                .andExpect(jsonPath("$.totalQuestions").value(1))
                .andExpect(jsonPath("$.questions[0].id").value(3));

        verify(quizService).getQuiz("React", Arrays.asList(1L, 2L));
    }

    @Test
    void getQuiz_shouldReturn200WithEmptyQuestions_whenNoQuestionsAvailable() throws Exception {
        // Arrange
        QuizDTO quizDTO = new QuizDTO("Python", 0, List.of());
        when(quizService.getQuiz(eq("Python"), isNull())).thenReturn(quizDTO);

        // Act & Assert
        mockMvc.perform(get("/api/quiz/Python")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topic").value("Python"))
                .andExpect(jsonPath("$.totalQuestions").value(0))
                .andExpect(jsonPath("$.questions.length()").value(0));
    }

    @Test
    void getQuiz_shouldReturn404_whenTopicNotFound() throws Exception {
        // Arrange
        when(quizService.getQuiz(eq("NonExistent"), isNull()))
                .thenThrow(new IllegalArgumentException("Topic not found: NonExistent"));

        // Act & Assert
        mockMvc.perform(get("/api/quiz/NonExistent")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Topic not found: NonExistent"))
                .andExpect(jsonPath("$.status").value("404"));
    }

    @Test
    void getQuiz_shouldHandleTopicWithSpaces() throws Exception {
        // Arrange
        List<OptionDTO> options = List.of(new OptionDTO(1L, "record", true));
        List<QuestionDTO> questions = List.of(
                new QuestionDTO(1L, "What keyword?", "EASY", null, options)
        );
        QuizDTO quizDTO = new QuizDTO("Java records", 1, questions);
        
        when(quizService.getQuiz(eq("Java records"), isNull())).thenReturn(quizDTO);

        // Act & Assert
        mockMvc.perform(get("/api/quiz/Java records")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topic").value("Java records"));
    }

    @Test
    void getQuiz_shouldHandleUrlEncodedTopic() throws Exception {
        // Arrange
        List<OptionDTO> options = List.of(new OptionDTO(1L, "useState", true));
        List<QuestionDTO> questions = List.of(
                new QuestionDTO(1L, "What is useState?", "EASY", null, options)
        );
        QuizDTO quizDTO = new QuizDTO("React hooks", 1, questions);
        
        when(quizService.getQuiz(eq("React hooks"), isNull())).thenReturn(quizDTO);

        // Act & Assert
        mockMvc.perform(get("/api/quiz/React%20hooks")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topic").value("React hooks"));
    }

    @Test
    void getQuiz_shouldReturnCorrectCorsHeaders() throws Exception {
        // Arrange
        QuizDTO quizDTO = new QuizDTO("Java", 0, List.of());
        when(quizService.getQuiz(any(), any())).thenReturn(quizDTO);

        // Act & Assert
        mockMvc.perform(get("/api/quiz/Java")
                        .header("Origin", "http://localhost:5173")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
