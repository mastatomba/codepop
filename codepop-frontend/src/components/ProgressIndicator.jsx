function ProgressIndicator({ current, total }) {
  const percentage = (current / total) * 100;

  return (
    <div className="progress-indicator">
      <div className="progress-text">
        Question {current} of {total}
      </div>
      <div className="progress-bar">
        <div
          className="progress-bar-fill"
          style={{ width: `${percentage}%` }}
        ></div>
      </div>
    </div>
  );
}

export default ProgressIndicator;
