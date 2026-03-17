import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import ManagementPage from '@/app/management/page';

// Mock fetch
global.fetch = jest.fn();

// Mock data matching store/models.go types
const mockOrders = [
  {
    orderId: '123e4567-e89b-12d3-a456-426614174000',
    orderItems: [{ pizzaType: 'Margherita', quantity: 2 }],
    orderData: 'Test order 1',
    orderStatus: 'pending',
  },
  {
    orderId: '223e4567-e89b-12d3-a456-426614174001',
    orderItems: [{ pizzaType: 'Pepperoni', quantity: 1 }],
    orderData: 'Test order 2',
    orderStatus: 'COOKED',
  },
];

const mockEvents = [
  { orderId: '123e4567-e89b-12d3-a456-426614174000', status: 'cooking', source: 'kitchen' },
  { orderId: '123e4567-e89b-12d3-a456-426614174000', status: 'in oven', source: 'kitchen' },
  { orderId: '123e4567-e89b-12d3-a456-426614174000', status: 'DONE', source: 'kitchen' },
];

const mockEventsWithDelivery = [
  { orderId: '123e4567-e89b-12d3-a456-426614174000', status: 'checking_inventory', source: 'kitchen', message: 'Checking available ingredients', toolName: 'getInventory', toolInput: '{}' },
  { orderId: '123e4567-e89b-12d3-a456-426614174000', status: 'COOKED', source: 'kitchen' },
  { orderId: '123e4567-e89b-12d3-a456-426614174000', status: 'checking_bikes', source: 'delivery', message: 'Checking available bikes for delivery', toolName: 'getBikes', toolInput: '{}' },
  { orderId: '123e4567-e89b-12d3-a456-426614174000', status: 'reserving_bike', source: 'delivery', message: 'Reserving bike for delivery: bike-1', toolName: 'reserveBike', toolInput: '{"bikeId":"bike-1"}' },
  { orderId: '123e4567-e89b-12d3-a456-426614174000', status: 'checking_bike_status', source: 'delivery', message: 'Checking bike status: bike-1', toolName: 'getBike', toolInput: '{"bikeId":"bike-1"}' },
  { orderId: '123e4567-e89b-12d3-a456-426614174000', status: 'DELIVERED', source: 'delivery' },
];

describe('Management Page', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders the management page title', () => {
    (global.fetch as jest.Mock).mockImplementation((url: string) => {
      if (url.includes('/health')) {
        return Promise.resolve({
          ok: true,
          json: async () => ({ status: 'healthy' }),
        });
      }
      if (url.includes('/api/orders')) {
        return Promise.resolve({
          ok: true,
          json: async () => [],
        });
      }
      return Promise.reject(new Error('Unknown endpoint'));
    });

    render(<ManagementPage />);
    expect(screen.getByRole('heading', { name: /management/i })).toBeInTheDocument();
  });

  it('displays status for all three services', async () => {
    (global.fetch as jest.Mock).mockImplementation((url: string) => {
      if (url.includes('/health')) {
        return Promise.resolve({
          ok: true,
          json: async () => ({ status: 'healthy' }),
        });
      }
      if (url.includes('/api/orders')) {
        return Promise.resolve({
          ok: true,
          json: async () => [],
        });
      }
      return Promise.reject(new Error('Unknown endpoint'));
    });

    render(<ManagementPage />);

    await waitFor(() => {
      expect(screen.getByText(/store service/i)).toBeInTheDocument();
    });

  });

  it('shows healthy status when services are up', async () => {
    (global.fetch as jest.Mock).mockImplementation((url: string) => {
      if (url.includes('/health')) {
        return Promise.resolve({
          ok: true,
          json: async () => ({ status: 'healthy' }),
        });
      }
      if (url.includes('/api/orders')) {
        return Promise.resolve({
          ok: true,
          json: async () => [],
        });
      }
      return Promise.reject(new Error('Unknown endpoint'));
    });

    render(<ManagementPage />);

    await waitFor(() => {
      const healthyStatuses = screen.getAllByText(/healthy/i);
      expect(healthyStatuses.length).toBeGreaterThan(0);
    });
  });

  it('shows unhealthy status when services are down', async () => {
    (global.fetch as jest.Mock).mockImplementation((url: string) => {
      if (url.includes('/health')) {
        return Promise.resolve({
          ok: false,
          status: 500,
        });
      }
      if (url.includes('/api/orders')) {
        return Promise.resolve({
          ok: true,
          json: async () => [],
        });
      }
      return Promise.reject(new Error('Unknown endpoint'));
    });

    render(<ManagementPage />);

    await waitFor(() => {
      const unhealthyStatuses = screen.getAllByText(/unhealthy/i);
      expect(unhealthyStatuses.length).toBeGreaterThan(0);
    });
  });

  it('displays loading state while checking services', () => {
    (global.fetch as jest.Mock).mockImplementation(
      () => new Promise(() => {}) // Never resolves
    );

    render(<ManagementPage />);

    expect(screen.getByText(/checking services/i)).toBeInTheDocument();
  });

  it('handles fetch errors gracefully', async () => {
    (global.fetch as jest.Mock).mockImplementation((url: string) => {
      if (url.includes('/api/orders')) {
        return Promise.resolve({
          ok: true,
          json: async () => [],
        });
      }
      return Promise.reject(new Error('Network error'));
    });

    render(<ManagementPage />);

    await waitFor(() => {
      const unhealthyStatuses = screen.getAllByText(/unhealthy/i);
      expect(unhealthyStatuses.length).toBeGreaterThan(0);
    });
  });

  it('displays orders list section', async () => {
    (global.fetch as jest.Mock).mockImplementation((url: string) => {
      if (url.includes('/health')) {
        return Promise.resolve({
          ok: true,
          json: async () => ({ status: 'healthy' }),
        });
      }
      if (url.includes('/api/orders')) {
        return Promise.resolve({
          ok: true,
          json: async () => mockOrders,
        });
      }
      return Promise.reject(new Error('Unknown endpoint'));
    });

    render(<ManagementPage />);

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /orders/i })).toBeInTheDocument();
    });
  });

  it('displays all orders with their status', async () => {
    (global.fetch as jest.Mock).mockImplementation((url: string) => {
      if (url.includes('/health')) {
        return Promise.resolve({
          ok: true,
          json: async () => ({ status: 'healthy' }),
        });
      }
      if (url.includes('/api/orders')) {
        return Promise.resolve({
          ok: true,
          json: async () => mockOrders,
        });
      }
      return Promise.reject(new Error('Unknown endpoint'));
    });

    render(<ManagementPage />);

    await waitFor(() => {
      expect(screen.getByText(/pending/i)).toBeInTheDocument();
    });

    expect(screen.getByText(/COOKED/i)).toBeInTheDocument();
    expect(screen.getByText(/Margherita/i)).toBeInTheDocument();
    expect(screen.getByText(/Pepperoni/i)).toBeInTheDocument();
  });

  it('shows events when an order is selected', async () => {
    (global.fetch as jest.Mock).mockImplementation((url: string) => {
      if (url.includes('/health')) {
        return Promise.resolve({
          ok: true,
          json: async () => ({ status: 'healthy' }),
        });
      }
      if (url.includes('/api/orders')) {
        return Promise.resolve({
          ok: true,
          json: async () => mockOrders,
        });
      }
      if (url.includes('/api/events')) {
        return Promise.resolve({
          ok: true,
          json: async () => mockEvents,
        });
      }
      return Promise.reject(new Error('Unknown endpoint'));
    });

    render(<ManagementPage />);

    // Wait for orders to load
    await waitFor(() => {
      expect(screen.getByText(/pending/i)).toBeInTheDocument();
    });

    // Click on the first order
    const orderRows = screen.getAllByTestId('order-row');
    fireEvent.click(orderRows[0]);

    // Wait for events to load
    await waitFor(() => {
      expect(screen.getByText(/cooking/i)).toBeInTheDocument();
    });

    expect(screen.getByText(/in oven/i)).toBeInTheDocument();
    expect(screen.getByText(/DONE/i)).toBeInTheDocument();
  });

  it('displays empty message when no orders exist', async () => {
    (global.fetch as jest.Mock).mockImplementation((url: string) => {
      if (url.includes('/health')) {
        return Promise.resolve({
          ok: true,
          json: async () => ({ status: 'healthy' }),
        });
      }
      if (url.includes('/api/orders')) {
        return Promise.resolve({
          ok: true,
          json: async () => [],
        });
      }
      return Promise.reject(new Error('Unknown endpoint'));
    });

    render(<ManagementPage />);

    await waitFor(() => {
      expect(screen.getByText(/no orders/i)).toBeInTheDocument();
    });
  });

  it('displays events section heading when order is selected', async () => {
    (global.fetch as jest.Mock).mockImplementation((url: string) => {
      if (url.includes('/health')) {
        return Promise.resolve({
          ok: true,
          json: async () => ({ status: 'healthy' }),
        });
      }
      if (url.includes('/api/orders')) {
        return Promise.resolve({
          ok: true,
          json: async () => mockOrders,
        });
      }
      if (url.includes('/api/events')) {
        return Promise.resolve({
          ok: true,
          json: async () => mockEvents,
        });
      }
      return Promise.reject(new Error('Unknown endpoint'));
    });

    render(<ManagementPage />);

    await waitFor(() => {
      expect(screen.getByText(/pending/i)).toBeInTheDocument();
    });

    const orderRows = screen.getAllByTestId('order-row');
    fireEvent.click(orderRows[0]);

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /events/i })).toBeInTheDocument();
    });
  });

  it('displays delivery event details with message and tool info', async () => {
    (global.fetch as jest.Mock).mockImplementation((url: string) => {
      if (url.includes('/health')) {
        return Promise.resolve({
          ok: true,
          json: async () => ({ status: 'healthy' }),
        });
      }
      if (url.includes('/api/orders')) {
        return Promise.resolve({
          ok: true,
          json: async () => mockOrders,
        });
      }
      if (url.includes('/api/events')) {
        return Promise.resolve({
          ok: true,
          json: async () => mockEventsWithDelivery,
        });
      }
      return Promise.reject(new Error('Unknown endpoint'));
    });

    render(<ManagementPage />);

    await waitFor(() => {
      expect(screen.getByText(/pending/i)).toBeInTheDocument();
    });

    // Click on the first order
    const orderRows = screen.getAllByTestId('order-row');
    fireEvent.click(orderRows[0]);

    // Wait for events to load and verify delivery events are shown
    await waitFor(() => {
      expect(screen.getByText('Checking available bikes for delivery')).toBeInTheDocument();
    });

    // Verify delivery event details
    expect(screen.getByText('Reserving bike for delivery: bike-1')).toBeInTheDocument();
    expect(screen.getByText('Checking bike status: bike-1')).toBeInTheDocument();

  });

  it('shows Details column header in events table', async () => {
    (global.fetch as jest.Mock).mockImplementation((url: string) => {
      if (url.includes('/health')) {
        return Promise.resolve({
          ok: true,
          json: async () => ({ status: 'healthy' }),
        });
      }
      if (url.includes('/api/orders')) {
        return Promise.resolve({
          ok: true,
          json: async () => mockOrders,
        });
      }
      if (url.includes('/api/events')) {
        return Promise.resolve({
          ok: true,
          json: async () => mockEventsWithDelivery,
        });
      }
      return Promise.reject(new Error('Unknown endpoint'));
    });

    render(<ManagementPage />);

    await waitFor(() => {
      expect(screen.getByText(/pending/i)).toBeInTheDocument();
    });

    const orderRows = screen.getAllByTestId('order-row');
    fireEvent.click(orderRows[0]);

    await waitFor(() => {
      expect(screen.getByTestId('events-detail-table')).toBeInTheDocument();
    });

    // Verify the events detail table has the expected column headers
    const eventsTable = screen.getByTestId('events-detail-table');
    const headers = eventsTable.querySelectorAll('th');
    const headerTexts = Array.from(headers).map((h) => h.textContent);
    expect(headerTexts).toContain('Status');
    expect(headerTexts).toContain('Source');
    expect(headerTexts).toContain('Details');
  });

  it('displays tool name and input in events detail table', async () => {
    (global.fetch as jest.Mock).mockImplementation((url: string) => {
      if (url.includes('/health')) {
        return Promise.resolve({
          ok: true,
          json: async () => ({ status: 'healthy' }),
        });
      }
      if (url.includes('/api/orders')) {
        return Promise.resolve({
          ok: true,
          json: async () => mockOrders,
        });
      }
      if (url.includes('/api/events')) {
        return Promise.resolve({
          ok: true,
          json: async () => mockEventsWithDelivery,
        });
      }
      return Promise.reject(new Error('Unknown endpoint'));
    });

    render(<ManagementPage />);

    await waitFor(() => {
      expect(screen.getByText(/pending/i)).toBeInTheDocument();
    });

    const orderRows = screen.getAllByTestId('order-row');
    fireEvent.click(orderRows[0]);

    // Verify tool names are displayed in brackets
    await waitFor(() => {
      expect(screen.getByText(/\[getBikes\]/)).toBeInTheDocument();
    });

    expect(screen.getByText(/\[reserveBike\]/)).toBeInTheDocument();
    expect(screen.getByText(/\[getBike\]/)).toBeInTheDocument();
  });
});
