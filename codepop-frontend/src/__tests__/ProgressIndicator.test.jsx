import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import ProgressIndicator from '../components/ProgressIndicator';

describe('ProgressIndicator', () => {
  it('displays current and total questions', () => {
    render(<ProgressIndicator current={2} total={5} />);

    expect(screen.getByText('Question 2 of 5')).toBeInTheDocument();
  });

  it('shows progress bar with correct width', () => {
    const { container } = render(<ProgressIndicator current={3} total={5} />);

    const progressFill = container.querySelector('.progress-bar-fill');
    expect(progressFill).toHaveStyle({ width: '60%' });
  });

  it('shows 100% progress when on last question', () => {
    const { container } = render(<ProgressIndicator current={5} total={5} />);

    const progressFill = container.querySelector('.progress-bar-fill');
    expect(progressFill).toHaveStyle({ width: '100%' });
  });

  it('shows 20% progress on first question of five', () => {
    const { container } = render(<ProgressIndicator current={1} total={5} />);

    const progressFill = container.querySelector('.progress-bar-fill');
    expect(progressFill).toHaveStyle({ width: '20%' });
  });
});
