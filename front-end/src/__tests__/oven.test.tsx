import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import OvenPage from '@/app/oven/page';

// Mock fetch
global.fetch = jest.fn();

// Mock data matching oven service models
const mockOvens = [
  { id: 'oven-1', status: 'AVAILABLE', updatedAt: '2024-01-01T00:00:00Z' },
  { id: 'oven-2', status: 'RESERVED', user: 'user1', updatedAt: '2024-01-01T00:00:00Z' },
  { id: 'oven-3', status: 'AVAILABLE', updatedAt: '2024-01-01T00:00:00Z' },
  { id: 'oven-4', status: 'AVAILABLE', updatedAt: '2024-01-01T00:00:00Z' },
];

describe('Oven Page', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders the oven page title', async () => {
    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => mockOvens,
    });

    render(<OvenPage />);

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /oven/i })).toBeInTheDocument();
    });
  });

  it('displays a list of ovens with their status', async () => {
    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => mockOvens,
    });

    render(<OvenPage />);

    await waitFor(() => {
      expect(screen.getByText('oven-1')).toBeInTheDocument();
    });

    expect(screen.getByText('oven-2')).toBeInTheDocument();
    expect(screen.getByText('oven-3')).toBeInTheDocument();
    expect(screen.getByText('oven-4')).toBeInTheDocument();
  });

  it('displays oven status (AVAILABLE or RESERVED)', async () => {
    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => mockOvens,
    });

    render(<OvenPage />);

    await waitFor(() => {
      expect(screen.getAllByText('AVAILABLE').length).toBe(3);
    });

    expect(screen.getByText('RESERVED')).toBeInTheDocument();
  });

  it('displays loading state while fetching ovens', () => {
    (global.fetch as jest.Mock).mockImplementationOnce(
      () => new Promise(() => {}) // Never resolves
    );

    render(<OvenPage />);

    expect(screen.getByText(/loading/i)).toBeInTheDocument();
  });

  it('displays error message when fetch fails', async () => {
    (global.fetch as jest.Mock).mockRejectedValueOnce(new Error('Failed to fetch'));

    render(<OvenPage />);

    await waitFor(() => {
      expect(screen.getByText(/error/i)).toBeInTheDocument();
    });
  });

  it('shows reserve button for available ovens', async () => {
    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => mockOvens,
    });

    render(<OvenPage />);

    await waitFor(() => {
      const reserveButtons = screen.getAllByRole('button', { name: /reserve/i });
      expect(reserveButtons.length).toBe(3); // 3 available ovens
    });
  });

  it('shows release button for reserved ovens', async () => {
    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => mockOvens,
    });

    render(<OvenPage />);

    await waitFor(() => {
      const releaseButtons = screen.getAllByRole('button', { name: /release/i });
      expect(releaseButtons.length).toBe(1); // 1 reserved oven
    });
  });

  it('reserves an oven when reserve button is clicked', async () => {
    const user = userEvent.setup();

    (global.fetch as jest.Mock)
      .mockResolvedValueOnce({
        ok: true,
        json: async () => mockOvens,
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ id: 'oven-1', status: 'RESERVED', user: 'user', updatedAt: '2024-01-01T00:00:00Z' }),
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => [
          { id: 'oven-1', status: 'RESERVED', user: 'user', updatedAt: '2024-01-01T00:00:00Z' },
          { id: 'oven-2', status: 'RESERVED', user: 'user1', updatedAt: '2024-01-01T00:00:00Z' },
          { id: 'oven-3', status: 'AVAILABLE', updatedAt: '2024-01-01T00:00:00Z' },
          { id: 'oven-4', status: 'AVAILABLE', updatedAt: '2024-01-01T00:00:00Z' },
        ],
      });

    render(<OvenPage />);

    await waitFor(() => {
      expect(screen.getByText('oven-1')).toBeInTheDocument();
    });

    const reserveButtons = screen.getAllByRole('button', { name: /reserve/i });
    await user.click(reserveButtons[0]);

    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/oven/oven-1'),
        expect.objectContaining({ method: 'POST' })
      );
    });
  });

  it('releases an oven when release button is clicked', async () => {
    const user = userEvent.setup();

    (global.fetch as jest.Mock)
      .mockResolvedValueOnce({
        ok: true,
        json: async () => mockOvens,
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ id: 'oven-2', status: 'AVAILABLE', updatedAt: '2024-01-01T00:00:00Z' }),
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => [
          { id: 'oven-1', status: 'AVAILABLE', updatedAt: '2024-01-01T00:00:00Z' },
          { id: 'oven-2', status: 'AVAILABLE', updatedAt: '2024-01-01T00:00:00Z' },
          { id: 'oven-3', status: 'AVAILABLE', updatedAt: '2024-01-01T00:00:00Z' },
          { id: 'oven-4', status: 'AVAILABLE', updatedAt: '2024-01-01T00:00:00Z' },
        ],
      });

    render(<OvenPage />);

    await waitFor(() => {
      expect(screen.getByText('oven-2')).toBeInTheDocument();
    });

    const releaseButton = screen.getByRole('button', { name: /release/i });
    await user.click(releaseButton);

    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/oven/oven-2'),
        expect.objectContaining({ method: 'DELETE' })
      );
    });
  });

  it('displays the user who reserved the oven', async () => {
    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => mockOvens,
    });

    render(<OvenPage />);

    await waitFor(() => {
      expect(screen.getByText(/user1/i)).toBeInTheDocument();
    });
  });
});
