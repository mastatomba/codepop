package nl.schoutens.codepop.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public Question(Topic topic, String questionText, Difficulty difficulty, String explanation) {
    this.topic = topic;
    this.questionText = questionText;
    this.difficulty = difficulty;
    this.explanation = explanation;
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
