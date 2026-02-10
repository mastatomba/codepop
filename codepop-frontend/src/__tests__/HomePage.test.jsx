import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import userEvent from '@testing-library/user-event';
import HomePage from '../pages/HomePage';

const renderWithRouter = (component) => {
  return render(<BrowserRouter>{component}</BrowserRouter>);
};

describe('HomePage', () => {
  it('renders the homepage with title and form', () => {
    renderWithRouter(<HomePage />);

    expect(screen.getByText('CodePop Quiz')).toBeInTheDocument();
    expect(
      screen.getByText('Test your coding knowledge with AI-generated quizzes')
    ).toBeInTheDocument();
    expect(
      screen.getByLabelText(/enter a coding topic or subtopic/i)
    ).toBeInTheDocument();
  });

  it('displays help text with instructions', () => {
    renderWithRouter(<HomePage />);

    expect(screen.getByText('How it works:')).toBeInTheDocument();
    expect(screen.getByText(/Enter any coding topic/i)).toBeInTheDocument();
  });

  it('has submit button disabled when input is empty', () => {
    renderWithRouter(<HomePage />);

    const submitButton = screen.getByRole('button', { name: /start quiz/i });
    expect(submitButton).toBeDisabled();
  });

  it('enables submit button when input has text', async () => {
    const user = userEvent.setup();
    renderWithRouter(<HomePage />);

    const input = screen.getByLabelText(/enter a coding topic or subtopic/i);
    const submitButton = screen.getByRole('button', { name: /start quiz/i });

    await user.type(input, 'Java');

    expect(submitButton).not.toBeDisabled();
  });

  it('does not submit with only whitespace', async () => {
    const user = userEvent.setup();
    renderWithRouter(<HomePage />);

    const input = screen.getByLabelText(/enter a coding topic or subtopic/i);
    const submitButton = screen.getByRole('button', { name: /start quiz/i });

    await user.type(input, '   ');

    expect(submitButton).toBeDisabled();
  });

  it('updates input value as user types', async () => {
    const user = userEvent.setup();
    renderWithRouter(<HomePage />);

    const input = screen.getByLabelText(/enter a coding topic or subtopic/i);

    await user.type(input, 'React hooks');

    expect(input).toHaveValue('React hooks');
  });
});
