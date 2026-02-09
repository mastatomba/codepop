package nl.schoutens.codepop.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import nl.schoutens.codepop.dto.QuizDTO;
import nl.schoutens.codepop.service.QuizService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
public class QuizController {

  private final QuizService quizService;

  @GetMapping("/{topic}")
  public ResponseEntity<QuizDTO> getQuiz(
      @PathVariable String topic, @RequestParam(required = false) List<Long> excludeQuestionIds) {

    QuizDTO quiz = quizService.getQuiz(topic, excludeQuestionIds);
    return ResponseEntity.ok(quiz);
  }
}
