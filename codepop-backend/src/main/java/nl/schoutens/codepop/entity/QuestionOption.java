package nl.schoutens.codepop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "question_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuestionOption {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "question_id", nullable = false)
  private Question question;

  @Column(name = "option_text", nullable = false, columnDefinition = "TEXT")
  private String optionText;

  @Column(name = "is_correct", nullable = false)
  private Boolean isCorrect;

  public QuestionOption(Question question, String optionText, Boolean isCorrect) {
    this.question = question;
    this.optionText = optionText;
    this.isCorrect = isCorrect;
  }
}
