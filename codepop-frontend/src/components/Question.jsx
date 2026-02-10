import QuestionOption from './QuestionOption';

function Question({
  question,
  selectedOptionId,
  showFeedback,
  onOptionSelect,
}) {
  const selectedOption = question.options.find(
    (opt) => opt.id === selectedOptionId
  );
  const isCorrect = selectedOption?.isCorrect;

  return (
    <div className="question">
      <div className="question-header">
        <h2>{question.text}</h2>
        <span
          className={`difficulty difficulty-${question.difficulty.toLowerCase()}`}
        >
          {question.difficulty}
        </span>
      </div>

      <div className="question-options">
        {question.options.map((option) => (
          <QuestionOption
            key={option.id}
            option={option}
            isSelected={option.id === selectedOptionId}
            showFeedback={showFeedback}
            onSelect={() => onOptionSelect(option.id)}
          />
        ))}
      </div>

      {showFeedback && (
        <div className={`feedback ${isCorrect ? 'correct' : 'incorrect'}`}>
          <h3>{isCorrect ? '✓ Correct!' : '✗ Incorrect'}</h3>
          {!isCorrect && selectedOption && (
            <p>
              You selected: <strong>{selectedOption.text}</strong>
            </p>
          )}
          {!isCorrect && (
            <p>
              Correct answer:{' '}
              <strong>
                {question.options.find((opt) => opt.isCorrect).text}
              </strong>
            </p>
          )}
          {question.explanation && (
            <div className="explanation">
              <h4>Explanation:</h4>
              <p>{question.explanation}</p>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export default Question;
