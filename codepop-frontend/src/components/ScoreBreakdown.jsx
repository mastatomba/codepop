function ScoreBreakdown({
  userAnswers,
  totalQuestions,
  topic,
  onRetakeQuiz,
  onContinueSameTopic,
}) {
  const correctCount = userAnswers.filter((answer) => answer.isCorrect).length;
  const percentage = Math.round((correctCount / totalQuestions) * 100);

  const getScoreMessage = () => {
    if (percentage === 100) return 'Perfect score! 🎉';
    if (percentage >= 80) return 'Excellent work! 🌟';
    if (percentage >= 60) return 'Good job! 👍';
    if (percentage >= 40) return 'Keep practicing! 📚';
    return 'Keep learning! 💪';
  };

  return (
    <div className="score-breakdown">
      <header className="score-header">
        <h1>Quiz Complete!</h1>
        <h2>{topic}</h2>
      </header>

      <div className="score-summary">
        <div className="score-circle">
          <div className="percentage">{percentage}%</div>
          <div className="score-text">
            {correctCount} / {totalQuestions} correct
          </div>
        </div>
        <p className="score-message">{getScoreMessage()}</p>
      </div>

      <div className="answers-review">
        <h3>Review Your Answers:</h3>
        {userAnswers.map((answer, index) => (
          <div
            key={answer.questionId}
            className={`answer-item ${answer.isCorrect ? 'correct' : 'incorrect'}`}
          >
            <div className="answer-header">
              <span className="question-number">Question {index + 1}</span>
              <span
                className={`difficulty difficulty-${answer.difficulty.toLowerCase()}`}
              >
                {answer.difficulty}
              </span>
              <span className="result-icon">
                {answer.isCorrect ? '✓' : '✗'}
              </span>
            </div>
            <p className="question-text">{answer.questionText}</p>
            <div className="answer-details">
              <p>
                <strong>Your answer:</strong>{' '}
                <span
                  className={
                    answer.isCorrect ? 'correct-text' : 'incorrect-text'
                  }
                >
                  {answer.selectedOptionText}
                </span>
              </p>
              {!answer.isCorrect && (
                <p>
                  <strong>Correct answer:</strong>{' '}
                  <span className="correct-text">
                    {answer.correctOption.text}
                  </span>
                </p>
              )}
              {answer.explanation && (
                <div className="explanation">
                  <strong>Explanation:</strong>
                  <p>{answer.explanation}</p>
                </div>
              )}
            </div>
          </div>
        ))}
      </div>

      <div className="score-actions">
        <button onClick={onContinueSameTopic} className="btn-primary">
          Continue with {topic}
        </button>
        <button onClick={onRetakeQuiz} className="btn-secondary">
          Take Another Quiz
        </button>
      </div>
    </div>
  );
}

export default ScoreBreakdown;
