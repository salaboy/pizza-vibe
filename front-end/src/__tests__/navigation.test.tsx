import { render, screen } from '@testing-library/react';
import Navigation from '@/components/Navigation';

// Mock next/navigation
jest.mock('next/navigation', () => ({
  usePathname: () => '/',
}));

describe('Navigation', () => {
  it('renders the navigation with brand name', () => {
    render(<Navigation />);
    expect(screen.getByText('Pizza Vibe')).toBeInTheDocument();
  });

  it('renders all navigation links', () => {
    render(<Navigation />);
    expect(screen.getByRole('link', { name: /order/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /inventory/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /drinks stock/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /ovens/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /bikes/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /management/i })).toBeInTheDocument();
  });

  it('has correct href attributes for links', () => {
    render(<Navigation />);
    expect(screen.getByRole('link', { name: /order/i })).toHaveAttribute('href', '/');
    expect(screen.getByRole('link', { name: /inventory/i })).toHaveAttribute('href', '/inventory');
    expect(screen.getByRole('link', { name: /drinks stock/i })).toHaveAttribute('href', '/drinks-stock');
    expect(screen.getByRole('link', { name: /ovens/i })).toHaveAttribute('href', '/oven');
    expect(screen.getByRole('link', { name: /bikes/i })).toHaveAttribute('href', '/bikes');
    expect(screen.getByRole('link', { name: /management/i })).toHaveAttribute('href', '/management');
  });

});
