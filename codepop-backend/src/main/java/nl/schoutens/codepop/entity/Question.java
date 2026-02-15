package nl.schoutens.codepop.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
public class Question {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "topic_id", nullable = false)
  private Topic topic;

  @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
  private String questionText;

  @Column(length = 100)
  private String subtopic; // Optional subtopic for filtering (e.g., "records", "hooks")

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Difficulty difficulty;

  @Column(columnDefinition = "TEXT")
  private String explanation;

  @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<QuestionOption> options = new ArrayList<>();

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public Question(Topic topic, String questionText, Difficulty difficulty, String explanation) {
    this.topic = topic;
    this.questionText = questionText;
    this.difficulty = difficulty;
    this.explanation = explanation;
    this.options = new ArrayList<>();
  }

  public Question(
      Topic topic,
      String questionText,
      String subtopic,
      Difficulty difficulty,
      String explanation) {
    this.topic = topic;
    this.questionText = questionText;
    this.subtopic = subtopic;
    this.difficulty = difficulty;
    this.explanation = explanation;
    this.options = new ArrayList<>();
  }

  public void addOption(QuestionOption option) {
    options.add(option);
    option.setQuestion(this);
  }

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }

  public enum Difficulty {
    EASY,
    MEDIUM,
    HARD
  }
}
