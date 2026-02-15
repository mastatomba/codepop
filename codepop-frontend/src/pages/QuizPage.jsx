import { useState, useEffect } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import { quizApi } from '../services/api';
import Question from '../components/Question';
import ProgressIndicator from '../components/ProgressIndicator';
import ScoreBreakdown from '../components/ScoreBreakdown';
import { getAskedQuestions, addAskedQuestions } from '../utils/sessionStorage';

function QuizPage() {
  const { topic } = useParams();
  const navigate = useNavigate();
  const location = useLocation();

  const [quizData, setQuizData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [currentQuestionIndex, setCurrentQuestionIndex] = useState(0);
  const [selectedOptionId, setSelectedOptionId] = useState(null);
  const [showFeedback, setShowFeedback] = useState(false);
  const [userAnswers, setUserAnswers] = useState([]);
  const [isQuizComplete, setIsQuizComplete] = useState(false);

  useEffect(() => {
    const fetchQuiz = async () => {
      try {
        setLoading(true);
        setError(null);

        // Reset quiz state when starting a new quiz
        setCurrentQuestionIndex(0);
        setSelectedOptionId(null);
        setShowFeedback(false);
        setUserAnswers([]);
        setIsQuizComplete(false);

        // Get previously asked questions for this topic from session storage
        const excludeQuestionIds = getAskedQuestions(topic);
        console.log(
          `[QuizPage] Topic: "${topic}", Excluded IDs:`,
          excludeQuestionIds
        );

        // Fetch quiz, excluding already-asked questions
        const data = await quizApi.getQuiz(topic, excludeQuestionIds);
        console.log('[QuizPage] Quiz data received:', data);
        setQuizData(data);

        // Save the question IDs to session storage
        if (data.questions && data.questions.length > 0) {
          const questionIds = data.questions.map((q) => q.id);
          console.log('[QuizPage] Saving question IDs:', questionIds);
          addAskedQuestions(topic, questionIds);

          // Verify it was saved
          const saved = getAskedQuestions(topic);
          console.log('[QuizPage] Verified saved IDs:', saved);
        }
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };

    fetchQuiz();
  }, [topic, location.state]);

  const handleOptionSelect = (optionId) => {
    if (!showFeedback) {
      setSelectedOptionId(optionId);
    }
  };

  const handleSubmitAnswer = () => {
    if (selectedOptionId === null) return;

    setShowFeedback(true);

    const currentQuestion = quizData.questions[currentQuestionIndex];
    const selectedOption = currentQuestion.options.find(
      (opt) => opt.id === selectedOptionId
    );

    setUserAnswers([
      ...userAnswers,
      {
        questionId: currentQuestion.id,
        questionText: currentQuestion.text,
        selectedOptionId,
        selectedOptionText: selectedOption.text,
        isCorrect: selectedOption.isCorrect,
        correctOption: currentQuestion.options.find((opt) => opt.isCorrect),
        explanation: currentQuestion.explanation,
        difficulty: currentQuestion.difficulty,
      },
    ]);
  };

  const handleNext = () => {
    if (currentQuestionIndex < quizData.questions.length - 1) {
      setCurrentQuestionIndex(currentQuestionIndex + 1);
      setSelectedOptionId(null);
      setShowFeedback(false);
    } else {
      setIsQuizComplete(true);
    }
  };

  const handleRetakeQuiz = () => {
    navigate('/');
  };

  const handleContinueSameTopic = () => {
    navigate(`/quiz/${encodeURIComponent(topic)}`, {
      state: { refresh: Date.now() },
    });
  };

  if (loading) {
    return (
      <div className="quiz-page loading page-with-logo">
        <img src="/codepop_logo.png" alt="CodePop Logo" className="page-logo" />
        <div className="page-content">
          <div className="spinner"></div>
          <p>Loading quiz for "{topic}"...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="quiz-page error page-with-logo">
        <img src="/codepop_logo.png" alt="CodePop Logo" className="page-logo" />
        <div className="page-content">
          <h2>Error</h2>
          <p>{error}</p>
          <button onClick={() => navigate('/')}>Back to Home</button>
        </div>
      </div>
    );
  }

  if (!quizData || !quizData.questions || quizData.questions.length === 0) {
    return (
      <div className="quiz-page error page-with-logo">
        <img src="/codepop_logo.png" alt="CodePop Logo" className="page-logo" />
        <div className="page-content">
          <h2>No Questions Available</h2>
          <p>
            No questions found for "{topic}". Try a different topic or subtopic.
          </p>
          <button onClick={() => navigate('/')}>Back to Home</button>
        </div>
      </div>
    );
  }

  if (isQuizComplete) {
    return (
      <div className="quiz-page complete page-with-logo">
        <img src="/codepop_logo.png" alt="CodePop Logo" className="page-logo" />
        <div className="page-content">
          <ScoreBreakdown
            userAnswers={userAnswers}
            totalQuestions={quizData.totalQuestions}
            topic={quizData.topic}
            onRetakeQuiz={handleRetakeQuiz}
            onContinueSameTopic={handleContinueSameTopic}
          />
        </div>
      </div>
    );
  }

  const currentQuestion = quizData.questions[currentQuestionIndex];

  return (
    <div className="quiz-page page-with-logo">
      <img src="/codepop_logo.png" alt="CodePop Logo" className="page-logo" />
      <div className="page-content">
        <header>
          <h1>Quiz: {quizData.topic}</h1>
          <ProgressIndicator
            current={currentQuestionIndex + 1}
            total={quizData.totalQuestions}
          />
        </header>

        <main>
          <Question
            question={currentQuestion}
            selectedOptionId={selectedOptionId}
            showFeedback={showFeedback}
            onOptionSelect={handleOptionSelect}
          />

          <div className="quiz-actions">
            {!showFeedback ? (
              <button
                onClick={handleSubmitAnswer}
                disabled={selectedOptionId === null}
                className="btn-submit"
              >
                Submit Answer
              </button>
            ) : (
              <button onClick={handleNext} className="btn-next">
                {currentQuestionIndex < quizData.questions.length - 1
                  ? 'Next Question'
                  : 'View Results'}
              </button>
            )}
          </div>
        </main>
      </div>
    </div>
  );
}

export default QuizPage;
