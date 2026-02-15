import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ScoreBreakdown from '../components/ScoreBreakdown';

const mockUserAnswers = [
  {
    questionId: 1,
    questionText: 'What is question 1?',
    selectedOptionId: 1,
    selectedOptionText: 'Correct Answer',
    isCorrect: true,
    correctOption: { text: 'Correct Answer' },
    explanation: 'This is correct.',
    difficulty: 'EASY',
  },
  {
    questionId: 2,
    questionText: 'What is question 2?',
    selectedOptionId: 3,
    selectedOptionText: 'Wrong Answer',
    isCorrect: false,
    correctOption: { text: 'Right Answer' },
    explanation: 'This is wrong.',
    difficulty: 'MEDIUM',
  },
  {
    questionId: 3,
    questionText: 'What is question 3?',
    selectedOptionId: 5,
    selectedOptionText: 'Correct Answer',
    isCorrect: true,
    correctOption: { text: 'Correct Answer' },
    explanation: null,
    difficulty: 'HARD',
  },
];

describe('ScoreBreakdown', () => {
  it('displays quiz completion title', () => {
    render(
      <ScoreBreakdown
        userAnswers={mockUserAnswers}
        totalQuestions={3}
        topic="Java"
        onRetakeQuiz={vi.fn()}
        onContinueSameTopic={vi.fn()}
      />
    );

    expect(screen.getByText('Quiz Complete!')).toBeInTheDocument();
    expect(screen.getByText('Java')).toBeInTheDocument();
  });

  it('calculates and displays correct percentage', () => {
    render(
      <ScoreBreakdown
        userAnswers={mockUserAnswers}
        totalQuestions={3}
        topic="Java"
        onRetakeQuiz={vi.fn()}
        onContinueSameTopic={vi.fn()}
      />
    );

    expect(screen.getByText('67%')).toBeInTheDocument();
    expect(screen.getByText('2 / 3 correct')).toBeInTheDocument();
  });

  it('displays perfect score message for 100%', () => {
    const perfectAnswers = mockUserAnswers.map((a) => ({
      ...a,
      isCorrect: true,
    }));

    render(
      <ScoreBreakdown
        userAnswers={perfectAnswers}
        totalQuestions={3}
        topic="Java"
        onRetakeQuiz={vi.fn()}
        onContinueSameTopic={vi.fn()}
      />
    );

    expect(screen.getByText('100%')).toBeInTheDocument();
    expect(screen.getByText(/Perfect score! 🎉/i)).toBeInTheDocument();
  });

  it('displays excellent message for 80%+', () => {
    const excellentAnswers = [
      { ...mockUserAnswers[0], isCorrect: true },
      { ...mockUserAnswers[1], isCorrect: true },
      { ...mockUserAnswers[2], isCorrect: true },
      { ...mockUserAnswers[0], isCorrect: true },
      { ...mockUserAnswers[1], isCorrect: false },
    ];

    render(
      <ScoreBreakdown
        userAnswers={excellentAnswers}
        totalQuestions={5}
        topic="Java"
        onRetakeQuiz={vi.fn()}
        onContinueSameTopic={vi.fn()}
      />
    );

    expect(screen.getByText(/Excellent work! 🌟/i)).toBeInTheDocument();
  });

  it('shows all questions in review section', () => {
    render(
      <ScoreBreakdown
        userAnswers={mockUserAnswers}
        totalQuestions={3}
        topic="Java"
        onRetakeQuiz={vi.fn()}
        onContinueSameTopic={vi.fn()}
      />
    );

    expect(screen.getByText('What is question 1?')).toBeInTheDocument();
    expect(screen.getByText('What is question 2?')).toBeInTheDocument();
    expect(screen.getByText('What is question 3?')).toBeInTheDocument();
  });

  it('shows correct answer for incorrect questions', () => {
    render(
      <ScoreBreakdown
        userAnswers={mockUserAnswers}
        totalQuestions={3}
        topic="Java"
        onRetakeQuiz={vi.fn()}
        onContinueSameTopic={vi.fn()}
      />
    );

    expect(screen.getByText('Wrong Answer')).toBeInTheDocument();
    expect(screen.getByText('Right Answer')).toBeInTheDocument();
  });

  it('calls onRetakeQuiz when button is clicked', async () => {
    const user = userEvent.setup();
    const handleRetake = vi.fn();

    render(
      <ScoreBreakdown
        userAnswers={mockUserAnswers}
        totalQuestions={3}
        topic="Java"
        onRetakeQuiz={handleRetake}
        onContinueSameTopic={vi.fn()}
      />
    );

    const retakeButton = screen.getByRole('button', {
      name: /take another quiz/i,
    });
    await user.click(retakeButton);

    expect(handleRetake).toHaveBeenCalledTimes(1);
  });

  it('calls onContinueSameTopic when continue button is clicked', async () => {
    const user = userEvent.setup();
    const handleContinue = vi.fn();

    render(
      <ScoreBreakdown
        userAnswers={mockUserAnswers}
        totalQuestions={3}
        topic="Java"
        onRetakeQuiz={vi.fn()}
        onContinueSameTopic={handleContinue}
      />
    );

    const continueButton = screen.getByRole('button', {
      name: /continue with java/i,
    });
    await user.click(continueButton);

    expect(handleContinue).toHaveBeenCalledTimes(1);
  });

  it('displays explanations when available', () => {
    render(
      <ScoreBreakdown
        userAnswers={mockUserAnswers}
        totalQuestions={3}
        topic="Java"
        onRetakeQuiz={vi.fn()}
        onContinueSameTopic={vi.fn()}
      />
    );

    expect(screen.getByText('This is correct.')).toBeInTheDocument();
    expect(screen.getByText('This is wrong.')).toBeInTheDocument();
  });

  it('shows difficulty badges for each question', () => {
    render(
      <ScoreBreakdown
        userAnswers={mockUserAnswers}
        totalQuestions={3}
        topic="Java"
        onRetakeQuiz={vi.fn()}
        onContinueSameTopic={vi.fn()}
      />
    );

    expect(screen.getByText('EASY')).toBeInTheDocument();
    expect(screen.getByText('MEDIUM')).toBeInTheDocument();
    expect(screen.getByText('HARD')).toBeInTheDocument();
  });
});
