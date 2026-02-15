import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

function HomePage() {
  const [topic, setTopic] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = (e) => {
    e.preventDefault();
    const trimmedTopic = topic.trim();

    if (!trimmedTopic) {
      return;
    }

    setIsSubmitting(true);
    navigate(`/quiz/${encodeURIComponent(trimmedTopic)}`);
  };

  return (
    <div className="home-page page-with-logo">
      <img src="/codepop_logo.png" alt="CodePop Logo" className="page-logo" />
      <div className="page-content">
        <header>
          <h1>CodePop Quiz</h1>
          <p>Test your coding knowledge with AI-generated quizzes</p>
        </header>

        <main>
          <form onSubmit={handleSubmit} className="topic-form">
            <div className="form-group">
              <label htmlFor="topic-input">
                Enter a coding topic or subtopic:
              </label>
              <input
                id="topic-input"
                type="text"
                value={topic}
                onChange={(e) => setTopic(e.target.value)}
                placeholder="e.g., Java, React hooks, Python decorators"
                disabled={isSubmitting}
                autoFocus
              />
            </div>
            <button type="submit" disabled={!topic.trim() || isSubmitting}>
              {isSubmitting ? 'Loading...' : 'Start Quiz'}
            </button>
          </form>

          <div className="help-text">
            <h3>How it works:</h3>
            <ul>
              <li>Enter any coding topic (e.g., "Java", "React", "Python")</li>
              <li>Add subtopics for focused quizzes (e.g., "Java records")</li>
              <li>Backend uses fuzzy matching to find relevant questions</li>
              <li>Answer 5 questions and see your score!</li>
            </ul>
          </div>
        </main>
      </div>
    </div>
  );
}

export default HomePage;
