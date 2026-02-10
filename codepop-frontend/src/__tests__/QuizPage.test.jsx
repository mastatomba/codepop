import { describe, it, expect } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import userEvent from '@testing-library/user-event';
import QuizPage from '../pages/QuizPage';

const renderQuizPage = (topic = 'Java') => {
  return render(
    <MemoryRouter initialEntries={[`/quiz/${topic}`]}>
      <Routes>
        <Route path="/quiz/:topic" element={<QuizPage />} />
      </Routes>
    </MemoryRouter>
  );
};

describe('QuizPage', () => {
  it('shows loading state initially', () => {
    renderQuizPage();
    expect(screen.getByText(/loading quiz/i)).toBeInTheDocument();
  });

  it('loads and displays quiz questions', async () => {
    renderQuizPage('Java');

    await waitFor(() => {
      expect(screen.getByText('Quiz: Java')).toBeInTheDocument();
    });

    expect(screen.getByText('What is a test question?')).toBeInTheDocument();
    expect(screen.getByText('Question 1 of 3')).toBeInTheDocument();
  });

  it('displays error message for invalid topic', async () => {
    renderQuizPage('InvalidTopic123');

    await waitFor(() => {
      expect(screen.getByText('Error')).toBeInTheDocument();
    });

    expect(
      screen.getByText(/Topic not found:.*InvalidTopic123/i)
    ).toBeInTheDocument();
  });

  it('allows user to select an option', async () => {
    const user = userEvent.setup();
    renderQuizPage('Java');

    await waitFor(() => {
      expect(screen.getByText('What is a test question?')).toBeInTheDocument();
    });

    const correctOption = screen.getByText('Correct Answer');
    await user.click(correctOption);

    expect(correctOption.closest('button')).toHaveClass('selected');
  });

  it('submit button is disabled until option is selected', async () => {
    const user = userEvent.setup();
    renderQuizPage('Java');

    await waitFor(() => {
      expect(screen.getByText('What is a test question?')).toBeInTheDocument();
    });

    const submitButton = screen.getByRole('button', {
      name: /submit answer/i,
    });
    expect(submitButton).toBeDisabled();

    const option = screen.getByText('Correct Answer');
    await user.click(option);

    expect(submitButton).not.toBeDisabled();
  });

  it('shows feedback after submitting correct answer', async () => {
    const user = userEvent.setup();
    renderQuizPage('Java');

    await waitFor(() => {
      expect(screen.getByText('What is a test question?')).toBeInTheDocument();
    });

    const correctOption = screen.getByText('Correct Answer');
    await user.click(correctOption);

    const submitButton = screen.getByRole('button', {
      name: /submit answer/i,
    });
    await user.click(submitButton);

    expect(screen.getByText(/✓ Correct!/i)).toBeInTheDocument();
    expect(screen.getByText('This is a test explanation.')).toBeInTheDocument();
  });

  it('shows feedback after submitting incorrect answer', async () => {
    const user = userEvent.setup();
    renderQuizPage('Java');

    await waitFor(() => {
      expect(screen.getByText('What is a test question?')).toBeInTheDocument();
    });

    const wrongOption = screen.getByText('Wrong Answer 1');
    await user.click(wrongOption);

    const submitButton = screen.getByRole('button', {
      name: /submit answer/i,
    });
    await user.click(submitButton);

    expect(screen.getByText(/✗ Incorrect/i)).toBeInTheDocument();
    expect(screen.getByText(/You selected/i)).toBeInTheDocument();
    expect(screen.getByText(/Correct answer:/i)).toBeInTheDocument();
  });

  it('progresses to next question after clicking next', async () => {
    const user = userEvent.setup();
    renderQuizPage('Java');

    await waitFor(() => {
      expect(screen.getByText('What is a test question?')).toBeInTheDocument();
    });

    const option = screen.getByText('Correct Answer');
    await user.click(option);

    const submitButton = screen.getByRole('button', {
      name: /submit answer/i,
    });
    await user.click(submitButton);

    const nextButton = screen.getByRole('button', { name: /next question/i });
    await user.click(nextButton);

    expect(
      screen.getByText('What is another test question?')
    ).toBeInTheDocument();
    expect(screen.getByText('Question 2 of 3')).toBeInTheDocument();
  });

  it('shows results after completing all questions', async () => {
    const user = userEvent.setup();
    renderQuizPage('Java');

    await waitFor(() => {
      expect(screen.getByText('What is a test question?')).toBeInTheDocument();
    });

    // Answer question 1
    await user.click(screen.getByText('Correct Answer'));
    await user.click(screen.getByRole('button', { name: /submit answer/i }));
    await user.click(screen.getByRole('button', { name: /next question/i }));

    // Answer question 2
    await user.click(screen.getByText('Correct Answer'));
    await user.click(screen.getByRole('button', { name: /submit answer/i }));
    await user.click(screen.getByRole('button', { name: /next question/i }));

    // Answer question 3
    await user.click(screen.getByText('Correct Answer'));
    await user.click(screen.getByRole('button', { name: /submit answer/i }));
    await user.click(screen.getByRole('button', { name: /view results/i }));

    expect(screen.getByText('Quiz Complete!')).toBeInTheDocument();
    expect(screen.getByText(/Review Your Answers/i)).toBeInTheDocument();
    expect(screen.getByText('100%')).toBeInTheDocument();
  });
});
