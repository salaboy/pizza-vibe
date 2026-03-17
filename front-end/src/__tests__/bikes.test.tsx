import { render, screen, waitFor } from '@testing-library/react';
import BikesPage from '@/app/bikes/page';

// Mock fetch
global.fetch = jest.fn();

// Mock data matching bikes service models
const mockBikes = [
  { id: 'bike-1', status: 'AVAILABLE', updatedAt: '2024-01-01T00:00:00Z' },
  { id: 'bike-2', status: 'RESERVED', user: 'john', updatedAt: '2024-01-01T00:00:00Z' },
  { id: 'bike-3', status: 'AVAILABLE', updatedAt: '2024-01-01T00:00:00Z' },
];

describe('Bikes Page', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders the bikes page title', async () => {
    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => mockBikes,
    });

    render(<BikesPage />);

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /bikes/i })).toBeInTheDocument();
    });
  });

  it('displays a list of bikes with their status', async () => {
    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => mockBikes,
    });

    render(<BikesPage />);

    await waitFor(() => {
      expect(screen.getByText('bike-1')).toBeInTheDocument();
    });

    expect(screen.getByText('bike-2')).toBeInTheDocument();
    expect(screen.getByText('bike-3')).toBeInTheDocument();
  });

  it('displays bike status (AVAILABLE or RESERVED)', async () => {
    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => mockBikes,
    });

    render(<BikesPage />);

    await waitFor(() => {
      expect(screen.getAllByText('AVAILABLE').length).toBe(2);
    });

    expect(screen.getByText('RESERVED')).toBeInTheDocument();
  });

  it('displays loading state while fetching bikes', () => {
    (global.fetch as jest.Mock).mockImplementationOnce(
      () => new Promise(() => {}) // Never resolves
    );

    render(<BikesPage />);

    expect(screen.getByText(/loading/i)).toBeInTheDocument();
  });

  it('displays error message when fetch fails', async () => {
    (global.fetch as jest.Mock).mockRejectedValueOnce(new Error('Failed to fetch'));

    render(<BikesPage />);

    await waitFor(() => {
      expect(screen.getByText(/error/i)).toBeInTheDocument();
    });
  });

  it('displays the user who reserved the bike', async () => {
    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => mockBikes,
    });

    render(<BikesPage />);

    await waitFor(() => {
      expect(screen.getByText(/john/i)).toBeInTheDocument();
    });
  });
});
