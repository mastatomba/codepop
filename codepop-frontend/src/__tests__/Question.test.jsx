import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import Question from '../components/Question';

const mockQuestion = {
  id: 1,
  text: 'What is a test question?',
  difficulty: 'EASY',
  explanation: 'This is an explanation.',
  options: [
    { id: 1, text: 'Correct Answer', isCorrect: true },
    { id: 2, text: 'Wrong Answer 1', isCorrect: false },
    { id: 3, text: 'Wrong Answer 2', isCorrect: false },
  ],
};

describe('Question', () => {
  it('renders question text and difficulty', () => {
    render(
      <Question
        question={mockQuestion}
        selectedOptionId={null}
        showFeedback={false}
        onOptionSelect={vi.fn()}
      />
    );

    expect(screen.getByText('What is a test question?')).toBeInTheDocument();
    expect(screen.getByText('EASY')).toBeInTheDocument();
  });

  it('renders all options', () => {
    render(
      <Question
        question={mockQuestion}
        selectedOptionId={null}
        showFeedback={false}
        onOptionSelect={vi.fn()}
      />
    );

    expect(screen.getByText('Correct Answer')).toBeInTheDocument();
    expect(screen.getByText('Wrong Answer 1')).toBeInTheDocument();
    expect(screen.getByText('Wrong Answer 2')).toBeInTheDocument();
  });

  it('does not show feedback when showFeedback is false', () => {
    render(
      <Question
        question={mockQuestion}
        selectedOptionId={1}
        showFeedback={false}
        onOptionSelect={vi.fn()}
      />
    );

    expect(screen.queryByText(/correct!/i)).not.toBeInTheDocument();
  });

  it('shows correct feedback when answer is correct', () => {
    render(
      <Question
        question={mockQuestion}
        selectedOptionId={1}
        showFeedback={true}
        onOptionSelect={vi.fn()}
      />
    );

    expect(screen.getByText(/✓ Correct!/i)).toBeInTheDocument();
    expect(screen.getByText('This is an explanation.')).toBeInTheDocument();
  });

  it('shows incorrect feedback when answer is wrong', () => {
    render(
      <Question
        question={mockQuestion}
        selectedOptionId={2}
        showFeedback={true}
        onOptionSelect={vi.fn()}
      />
    );

    expect(screen.getByText(/✗ Incorrect/i)).toBeInTheDocument();
    expect(screen.getByText(/You selected/i)).toBeInTheDocument();
    expect(screen.getByText(/Correct answer:/i)).toBeInTheDocument();
  });

  it('handles question without explanation', () => {
    const questionNoExplanation = { ...mockQuestion, explanation: null };
    render(
      <Question
        question={questionNoExplanation}
        selectedOptionId={1}
        showFeedback={true}
        onOptionSelect={vi.fn()}
      />
    );

    expect(screen.getByText(/✓ Correct!/i)).toBeInTheDocument();
    expect(screen.queryByText('Explanation:')).not.toBeInTheDocument();
  });

  it('applies correct difficulty class', () => {
    const { container } = render(
      <Question
        question={mockQuestion}
        selectedOptionId={null}
        showFeedback={false}
        onOptionSelect={vi.fn()}
      />
    );

    const difficultyBadge = container.querySelector('.difficulty-easy');
    expect(difficultyBadge).toBeInTheDocument();
  });
});
