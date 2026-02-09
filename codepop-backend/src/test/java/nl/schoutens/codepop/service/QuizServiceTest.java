package nl.schoutens.codepop.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import nl.schoutens.codepop.dto.QuizDTO;
import nl.schoutens.codepop.entity.Question;
import nl.schoutens.codepop.entity.QuestionOption;
import nl.schoutens.codepop.entity.Topic;
import nl.schoutens.codepop.repository.QuestionOptionRepository;
import nl.schoutens.codepop.repository.QuestionRepository;
import nl.schoutens.codepop.repository.TopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

  @Mock(lenient = true)
  private TopicRepository topicRepository;

  @Mock(lenient = true)
  private QuestionRepository questionRepository;

  @Mock(lenient = true)
  private QuestionOptionRepository questionOptionRepository;

  @Mock(lenient = true)
  private QuizMaster quizMaster;

  @InjectMocks private QuizService quizService;

  private Topic testTopic;
  private List<Question> testQuestions;
  private List<QuestionOption> testOptions;

  @BeforeEach
  void setUp() {
    testTopic = new Topic("Java", "backend");
    testTopic.setId(1L);

    Question q1 =
        new Question(testTopic, "Question 1", "records", Question.Difficulty.EASY, "Explanation 1");
    q1.setId(1L);
    Question q2 =
        new Question(testTopic, "Question 2", "records", Question.Difficulty.MEDIUM, null);
    q2.setId(2L);
    Question q3 =
        new Question(testTopic, "Question 3", "streams", Question.Difficulty.HARD, "Explanation 3");
    q3.setId(3L);

    testQuestions = Arrays.asList(q1, q2, q3);

    testOptions =
        Arrays.asList(
            new QuestionOption(q1, "Option A", true),
            new QuestionOption(q1, "Option B", false),
            new QuestionOption(q1, "Option C", false),
            new QuestionOption(q1, "Option D", false));
  }

  @Test
  void getQuiz_shouldReturnQuizWithAllQuestions_whenNoExclusions() {
    when(topicRepository.findByNameIgnoreCase("Java")).thenReturn(Optional.of(testTopic));
    when(topicRepository.findAll()).thenReturn(List.of(testTopic));
    when(questionRepository.findByTopicId(1L)).thenReturn(testQuestions);
    when(questionOptionRepository.findByQuestionId(anyLong())).thenReturn(testOptions);

    QuizDTO result = quizService.getQuiz("Java", null);

    assertNotNull(result);
    assertEquals("Java", result.topic());
    assertEquals(3, result.totalQuestions());
    assertEquals(3, result.questions().size());
    verify(topicRepository, atLeastOnce()).findByNameIgnoreCase("Java");
    verify(questionRepository).findByTopicId(1L);
  }

  @Test
  void getQuiz_shouldExcludeQuestions_whenExcludeListProvided() {
    when(topicRepository.findByNameIgnoreCase("Java")).thenReturn(Optional.of(testTopic));
    when(topicRepository.findAll()).thenReturn(List.of(testTopic));
    when(questionRepository.findByTopicId(1L)).thenReturn(testQuestions);
    when(questionOptionRepository.findByQuestionId(anyLong())).thenReturn(testOptions);

    List<Long> excludeIds = Arrays.asList(1L, 2L);

    QuizDTO result = quizService.getQuiz("Java", excludeIds);

    assertNotNull(result);
    assertEquals(1, result.totalQuestions());
    assertEquals(3L, result.questions().get(0).id());
  }

  @Test
  void getQuiz_shouldThrowException_whenTopicNotFound() {
    when(topicRepository.findByNameIgnoreCase("InvalidTopic")).thenReturn(Optional.empty());
    when(topicRepository.findAll()).thenReturn(List.of(testTopic));

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> quizService.getQuiz("InvalidTopic", null));

    assertTrue(exception.getMessage().contains("Topic not found"));
  }

  @Test
  void getQuiz_shouldCallQuizMaster_whenLessThan5QuestionsAvailable() {
    when(topicRepository.findByNameIgnoreCase("Java")).thenReturn(Optional.of(testTopic));
    when(topicRepository.findAll()).thenReturn(List.of(testTopic));
    when(questionRepository.findByTopicId(1L)).thenReturn(testQuestions);
    when(questionOptionRepository.findByQuestionId(anyLong())).thenReturn(testOptions);
    when(quizMaster.generateQuestions(anyString(), anyInt(), anyList()))
        .thenReturn(Collections.emptyList());

    quizService.getQuiz("Java", null);

    verify(quizMaster).generateQuestions(eq("Java"), eq(2), anyList());
  }

  @Test
  void getQuiz_shouldReturnOnlyAvailableQuestions_whenLessThan5() {
    when(topicRepository.findByNameIgnoreCase("Java")).thenReturn(Optional.of(testTopic));
    when(topicRepository.findAll()).thenReturn(List.of(testTopic));
    when(questionRepository.findByTopicId(1L)).thenReturn(testQuestions);
    when(questionOptionRepository.findByQuestionId(anyLong())).thenReturn(testOptions);
    when(quizMaster.generateQuestions(anyString(), anyInt(), anyList()))
        .thenReturn(Collections.emptyList());

    QuizDTO result = quizService.getQuiz("Java", null);

    assertEquals(3, result.totalQuestions());
    assertTrue(result.totalQuestions() < 5);
  }

  @Test
  void getQuiz_shouldRandomizeQuestions_whenMoreThan5Available() {
    List<Question> manyQuestions =
        Arrays.asList(
            new Question(testTopic, "Q1", Question.Difficulty.EASY, null),
            new Question(testTopic, "Q2", Question.Difficulty.EASY, null),
            new Question(testTopic, "Q3", Question.Difficulty.EASY, null),
            new Question(testTopic, "Q4", Question.Difficulty.EASY, null),
            new Question(testTopic, "Q5", Question.Difficulty.EASY, null),
            new Question(testTopic, "Q6", Question.Difficulty.EASY, null),
            new Question(testTopic, "Q7", Question.Difficulty.EASY, null),
            new Question(testTopic, "Q8", Question.Difficulty.EASY, null),
            new Question(testTopic, "Q9", Question.Difficulty.EASY, null),
            new Question(testTopic, "Q10", Question.Difficulty.EASY, null));

    for (int i = 0; i < manyQuestions.size(); i++) {
      manyQuestions.get(i).setId((long) (i + 1));
    }

    when(topicRepository.findByNameIgnoreCase("Java")).thenReturn(Optional.of(testTopic));
    when(topicRepository.findAll()).thenReturn(List.of(testTopic));
    when(questionRepository.findByTopicId(1L)).thenReturn(manyQuestions);
    when(questionOptionRepository.findByQuestionId(anyLong())).thenReturn(testOptions);

    QuizDTO result = quizService.getQuiz("Java", null);

    assertEquals(5, result.totalQuestions());
    verify(quizMaster, never()).generateQuestions(anyString(), anyInt(), anyList());
  }

  @Test
  void getQuiz_shouldBeCaseInsensitive() {
    when(topicRepository.findByNameIgnoreCase("java")).thenReturn(Optional.of(testTopic));
    when(topicRepository.findAll()).thenReturn(List.of(testTopic));
    when(questionRepository.findByTopicId(1L)).thenReturn(testQuestions);
    when(questionOptionRepository.findByQuestionId(anyLong())).thenReturn(testOptions);

    QuizDTO result = quizService.getQuiz("java", null);

    assertNotNull(result);
    verify(topicRepository, atLeastOnce()).findByNameIgnoreCase("java");
  }

  @Test
  void getQuiz_shouldMatchSubtopic_whenUserInputIncludesSubtopic() {
    List<Question> recordsQuestions =
        Arrays.asList(
            testQuestions.get(0), // q1 with subtopic "records"
            testQuestions.get(1) // q2 with subtopic "records"
            );

    when(topicRepository.findByNameIgnoreCase("Java")).thenReturn(Optional.of(testTopic));
    when(topicRepository.findAll()).thenReturn(List.of(testTopic));
    when(questionRepository.findByTopicIdAndSubtopicContainingIgnoreCase(1L, "records"))
        .thenReturn(recordsQuestions);
    when(questionOptionRepository.findByQuestionId(anyLong())).thenReturn(testOptions);
    when(quizMaster.generateQuestions(anyString(), anyInt(), anyList()))
        .thenReturn(Collections.emptyList());

    QuizDTO result = quizService.getQuiz("Java records", null);

    assertNotNull(result);
    assertEquals("Java records", result.topic());
    assertEquals(2, result.totalQuestions());
    verify(questionRepository).findByTopicIdAndSubtopicContainingIgnoreCase(1L, "records");
  }

  @Test
  void getQuiz_shouldReturnEmptyAndCallLLM_whenSubtopicNotFound() {
    when(topicRepository.findByNameIgnoreCase("Java")).thenReturn(Optional.of(testTopic));
    when(topicRepository.findAll()).thenReturn(List.of(testTopic));
    when(questionRepository.findByTopicIdAndSubtopicContainingIgnoreCase(1L, "nonexistent"))
        .thenReturn(Collections.emptyList());
    when(questionOptionRepository.findByQuestionId(anyLong())).thenReturn(testOptions);
    when(quizMaster.generateQuestions(anyString(), eq(5), anyList()))
        .thenReturn(Collections.emptyList());

    QuizDTO result = quizService.getQuiz("Java nonexistent", null);

    assertNotNull(result);
    assertEquals(0, result.totalQuestions());
    verify(questionRepository).findByTopicIdAndSubtopicContainingIgnoreCase(1L, "nonexistent");
    verify(questionRepository, never()).findByTopicId(1L); // Should NOT fallback to all topic
    verify(quizMaster)
        .generateQuestions(eq("Java nonexistent"), eq(5), anyList()); // Should call LLM
  }

  @Test
  void getQuiz_shouldReturnAllTopicQuestions_whenNoSubtopicSpecified() {
    when(topicRepository.findByNameIgnoreCase("Java")).thenReturn(Optional.of(testTopic));
    when(topicRepository.findAll()).thenReturn(List.of(testTopic));
    when(questionRepository.findByTopicId(1L)).thenReturn(testQuestions);
    when(questionOptionRepository.findByQuestionId(anyLong())).thenReturn(testOptions);

    QuizDTO result = quizService.getQuiz("Java", null);

    assertNotNull(result);
    assertEquals(3, result.totalQuestions());
    verify(questionRepository).findByTopicId(1L);
    verify(questionRepository, never())
        .findByTopicIdAndSubtopicContainingIgnoreCase(anyLong(), anyString());
  }
}
