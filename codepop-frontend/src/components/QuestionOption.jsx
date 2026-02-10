function QuestionOption({ option, isSelected, showFeedback, onSelect }) {
  const getClassName = () => {
    let className = 'question-option';
    if (isSelected) className += ' selected';
    if (showFeedback) {
      if (option.isCorrect) className += ' correct';
      else if (isSelected && !option.isCorrect) className += ' incorrect';
    }
    return className;
  };

  return (
    <button
      className={getClassName()}
      onClick={onSelect}
      disabled={showFeedback}
      type="button"
    >
      <span className="option-text">{option.text}</span>
      {showFeedback && option.isCorrect && <span className="icon">✓</span>}
      {showFeedback && isSelected && !option.isCorrect && (
        <span className="icon">✗</span>
      )}
    </button>
  );
}

export default QuestionOption;
