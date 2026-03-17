import { render, screen, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import Home from '@/app/page';
import { OrderProvider } from '@/context/OrderContext';

// Mock fetch
global.fetch = jest.fn();

// Helper: create a mock WebSocket that auto-fires onopen after construction
function createMockWebSocket() {
  const mockWs = {
    close: jest.fn(),
    addEventListener: jest.fn(),
    removeEventListener: jest.fn(),
    readyState: 1,
    onopen: null as ((ev: Event) => void) | null,
    onmessage: null as ((ev: MessageEvent) => void) | null,
    onclose: null as ((ev: CloseEvent) => void) | null,
    onerror: null as ((ev: Event) => void) | null,
  };
  const MockWebSocket = jest.fn(() => {
    // Simulate async connection: fire onopen on next microtask
    Promise.resolve().then(() => {
      if (mockWs.onopen) {
        mockWs.onopen(new Event('open'));
      }
    });
    return mockWs;
  });
  (global as unknown as Record<string, unknown>).WebSocket = MockWebSocket;
  return { mockWs, MockWebSocket };
}

// Helper: add an item to the cart
async function addItemToCart(
  user: ReturnType<typeof userEvent.setup>,
  pizzaType: string,
  quantity: number
) {
  const pizzaSelect = screen.getByLabelText(/pizza type/i);
  const quantityInput = screen.getByLabelText(/quantity/i);
  const addToCartButton = screen.getByRole('button', { name: /add to cart/i });

  await user.selectOptions(pizzaSelect, pizzaType);
  await user.tripleClick(quantityInput);
  await user.keyboard(String(quantity));
  await user.click(addToCartButton);
}

function renderHome() {
  return render(
    <OrderProvider>
      <Home />
    </OrderProvider>
  );
}

describe('Home Page', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    sessionStorage.clear();
    // Mock drinks-stock fetch that fires on component mount
    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => [],
    });
  });

  it('renders the page title', () => {
    renderHome();
    expect(screen.getByRole('heading', { name: /pizza vibe/i })).toBeInTheDocument();
  });

  it('displays an order form with pizza type selection', () => {
    renderHome();
    expect(screen.getByLabelText(/pizza type/i)).toBeInTheDocument();
  });

  it('displays an order form with quantity input', () => {
    renderHome();
    expect(screen.getByLabelText(/quantity/i)).toBeInTheDocument();
  });

  it('displays a submit button to place the order', () => {
    renderHome();
    expect(screen.getByRole('button', { name: /place order/i })).toBeInTheDocument();
  });

  it('submits the order to the store service when form is submitted', async () => {
    const user = userEvent.setup();
    createMockWebSocket();

    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => ({ orderId: 'test-order-id', orderStatus: 'pending' }),
    });

    renderHome();

    // Add item to cart first
    await addItemToCart(user, 'Margherita', 2);

    const submitButton = screen.getByRole('button', { name: /place order/i });
    await user.click(submitButton);

    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith('/api/order', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          orderItems: [{ pizzaType: 'Margherita', quantity: 2 }],
        }),
      });
    });
  });

  it('displays success message and order ID after successful order', async () => {
    const user = userEvent.setup();
    createMockWebSocket();

    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => ({ orderId: 'test-order-id', orderStatus: 'pending' }),
    });

    renderHome();

    // Add item to cart first
    await addItemToCart(user, 'Margherita', 2);

    const submitButton = screen.getByRole('button', { name: /place order/i });
    await user.click(submitButton);

    await waitFor(() => {
      expect(screen.getByText(/order placed successfully/i)).toBeInTheDocument();
    });

    // Verify order ID is displayed
    expect(screen.getByText(/test-order-id/i)).toBeInTheDocument();
  });

  it('connects to WebSocket with unique client ID before placing order', async () => {
    const user = userEvent.setup();
    const { MockWebSocket } = createMockWebSocket();

    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => ({ orderId: 'ws-test-order-id', orderStatus: 'pending' }),
    });

    renderHome();

    // Add item to cart first
    await addItemToCart(user, 'Margherita', 1);

    const submitButton = screen.getByRole('button', { name: /place order/i });
    await user.click(submitButton);

    await waitFor(() => {
      expect(MockWebSocket).toHaveBeenCalled();
    });

    // Verify WebSocket URL connects directly to the store service with clientId
    const wsUrl = MockWebSocket.mock.calls[0][0] as string;
    expect(wsUrl).toContain('/ws?clientId=');
    expect(wsUrl).toMatch(/^wss?:\/\//);

    // Verify fetch was called after WebSocket connected
    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalled();
    });
  });

  it('displays WebSocket connection indicator', async () => {
    const user = userEvent.setup();
    createMockWebSocket();

    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => ({ orderId: 'indicator-test-id', orderStatus: 'pending' }),
    });

    renderHome();

    // Add item to cart first
    await addItemToCart(user, 'Margherita', 1);

    const submitButton = screen.getByRole('button', { name: /place order/i });
    await user.click(submitButton);

    // Wait for order to be placed (WebSocket connected first, then order placed)
    await waitFor(() => {
      expect(screen.getByText(/indicator-test-id/i)).toBeInTheDocument();
    });

    expect(screen.getByTestId('ws-status')).toBeInTheDocument();
    expect(screen.getByText(/connected/i)).toBeInTheDocument();
  });

  it('displays incoming WebSocket events in a table', async () => {
    const user = userEvent.setup();
    const { mockWs } = createMockWebSocket();

    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => ({ orderId: 'table-test-order-id', orderStatus: 'pending' }),
    });

    renderHome();

    // Add item to cart first
    await addItemToCart(user, 'Margherita', 1);

    const submitButton = screen.getByRole('button', { name: /place order/i });
    await user.click(submitButton);

    await waitFor(() => {
      expect(screen.getByText(/table-test-order-id/i)).toBeInTheDocument();
    });

    // Simulate receiving WebSocket events
    await act(async () => {
      if (mockWs.onmessage) {
        mockWs.onmessage(new MessageEvent('message', {
          data: JSON.stringify({
            orderId: 'table-test-order-id',
            status: 'cooking',
            source: 'kitchen',
            timestamp: '2026-01-26T10:00:00Z',
          }),
        }));
      }
    });

    // Verify events table is displayed
    expect(screen.getByTestId('events-table')).toBeInTheDocument();

    // Verify table headers
    expect(screen.getByText('Order ID')).toBeInTheDocument();
    expect(screen.getByText('Status')).toBeInTheDocument();
    expect(screen.getByText('Source')).toBeInTheDocument();
    expect(screen.getByText('Timestamp')).toBeInTheDocument();

    // Verify event data in table
    expect(screen.getByText('cooking')).toBeInTheDocument();
    expect(screen.getByText('kitchen')).toBeInTheDocument();
    expect(screen.getByText('2026-01-26T10:00:00Z')).toBeInTheDocument();

    // Simulate a second event
    await act(async () => {
      if (mockWs.onmessage) {
        mockWs.onmessage(new MessageEvent('message', {
          data: JSON.stringify({
            orderId: 'table-test-order-id',
            status: 'COOKED',
            source: 'kitchen',
            timestamp: '2026-01-26T10:05:00Z',
          }),
        }));
      }
    });

    // Verify both events are in the table
    expect(screen.getByText('cooking')).toBeInTheDocument();
    expect(screen.getByText('COOKED')).toBeInTheDocument();

    // Verify table rows (header + 2 data rows)
    const rows = screen.getByTestId('events-table').querySelectorAll('tr');
    expect(rows.length).toBe(3); // 1 header + 2 data rows
  });

  it('displays cooking update message and tool parameters in events table', async () => {
    const user = userEvent.setup();
    const { mockWs } = createMockWebSocket();

    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => ({ orderId: 'rich-event-order-id', orderStatus: 'pending' }),
    });

    renderHome();
    await addItemToCart(user, 'Margherita', 1);
    await user.click(screen.getByRole('button', { name: /place order/i }));

    await waitFor(() => {
      expect(screen.getByText(/rich-event-order-id/i)).toBeInTheDocument();
    });

    // Simulate a rich cooking update event
    await act(async () => {
      if (mockWs.onmessage) {
        mockWs.onmessage(new MessageEvent('message', {
          data: JSON.stringify({
            orderId: 'rich-event-order-id',
            status: 'checking_inventory',
            source: 'kitchen',
            timestamp: '2026-01-26T10:00:00Z',
            message: 'Checking available ingredients in inventory',
            toolName: 'getInventory',
            toolInput: '{}',
          }),
        }));
      }
    });

    // Verify message is displayed
    expect(screen.getByText('Checking available ingredients in inventory')).toBeInTheDocument();
  });

  it('does not display tool_completed events without context', async () => {
    const user = userEvent.setup();
    const { mockWs } = createMockWebSocket();

    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => ({ orderId: 'filter-test-id', orderStatus: 'pending' }),
    });

    renderHome();
    await addItemToCart(user, 'Margherita', 1);
    await user.click(screen.getByRole('button', { name: /place order/i }));

    await waitFor(() => {
      expect(screen.getByText(/filter-test-id/i)).toBeInTheDocument();
    });

    // Send a tool_completed event (should be filtered)
    await act(async () => {
      if (mockWs.onmessage) {
        mockWs.onmessage(new MessageEvent('message', {
          data: JSON.stringify({
            orderId: 'filter-test-id',
            status: 'tool_completed',
            source: 'kitchen',
            timestamp: '2026-01-26T10:00:00Z',
            message: 'Tool execution completed: getInventory',
          }),
        }));
      }
    });

    // Send a normal action event
    await act(async () => {
      if (mockWs.onmessage) {
        mockWs.onmessage(new MessageEvent('message', {
          data: JSON.stringify({
            orderId: 'filter-test-id',
            status: 'reserving_oven',
            source: 'kitchen',
            timestamp: '2026-01-26T10:01:00Z',
            message: 'Reserving oven for cooking',
          }),
        }));
      }
    });

    // tool_completed should not appear as a status in the table
    expect(screen.queryByText('tool_completed')).not.toBeInTheDocument();
    // But the normal event should appear
    expect(screen.getByText('Reserving oven for cooking')).toBeInTheDocument();
  });

  it('displays cooking progress percentage', async () => {
    const user = userEvent.setup();
    const { mockWs } = createMockWebSocket();

    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => ({ orderId: 'progress-test-id', orderStatus: 'pending' }),
    });

    renderHome();
    await addItemToCart(user, 'Margherita', 1);
    await user.click(screen.getByRole('button', { name: /place order/i }));

    await waitFor(() => {
      expect(screen.getByText(/progress-test-id/i)).toBeInTheDocument();
    });

    // Send cooking action events representing progress
    const events = [
      { status: 'checking_inventory', message: 'Checking inventory' },
      { status: 'acquiring_ingredients', message: 'Acquiring mozzarella' },
      { status: 'reserving_oven', message: 'Reserving oven' },
    ];

    for (const evt of events) {
      await act(async () => {
        if (mockWs.onmessage) {
          mockWs.onmessage(new MessageEvent('message', {
            data: JSON.stringify({
              orderId: 'progress-test-id',
              ...evt,
              source: 'kitchen',
              timestamp: '2026-01-26T10:00:00Z',
            }),
          }));
        }
      });
    }

    // Should show a progress indicator (data-testid="cooking-progress")
    expect(screen.getByTestId('cooking-progress')).toBeInTheDocument();
  });

  it('displays delivery progress after cooking is complete', async () => {
    const user = userEvent.setup();
    const { mockWs } = createMockWebSocket();

    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => ({ orderId: 'delivery-progress-test-id', orderStatus: 'pending' }),
    });

    renderHome();
    await addItemToCart(user, 'Margherita', 1);
    await user.click(screen.getByRole('button', { name: /place order/i }));

    await waitFor(() => {
      expect(screen.getByText(/delivery-progress-test-id/i)).toBeInTheDocument();
    });

    // Send COOKED event from kitchen
    await act(async () => {
      if (mockWs.onmessage) {
        mockWs.onmessage(new MessageEvent('message', {
          data: JSON.stringify({
            orderId: 'delivery-progress-test-id',
            status: 'COOKED',
            source: 'kitchen',
            timestamp: '2026-01-26T10:00:00Z',
          }),
        }));
      }
    });

    // Should show delivery progress indicator
    expect(screen.getByTestId('delivery-progress')).toBeInTheDocument();
    expect(screen.getByTestId('delivery-progress')).toHaveTextContent('In progress (0 updates)');

    // Send delivery agent events
    await act(async () => {
      if (mockWs.onmessage) {
        mockWs.onmessage(new MessageEvent('message', {
          data: JSON.stringify({
            orderId: 'delivery-progress-test-id',
            status: 'checking_bikes',
            source: 'delivery',
            timestamp: '2026-01-26T10:01:00Z',
            message: 'Checking available bikes for delivery',
            toolName: 'getBikes',
          }),
        }));
      }
    });

    expect(screen.getByTestId('delivery-progress')).toHaveTextContent('In progress (1 updates)');
    // Verify the delivery event details are shown in the events table
    expect(screen.getByText('Checking available bikes for delivery')).toBeInTheDocument();

    // Send reserving_bike event
    await act(async () => {
      if (mockWs.onmessage) {
        mockWs.onmessage(new MessageEvent('message', {
          data: JSON.stringify({
            orderId: 'delivery-progress-test-id',
            status: 'reserving_bike',
            source: 'delivery',
            timestamp: '2026-01-26T10:02:00Z',
            message: 'Reserving bike for delivery: bike-1',
            toolName: 'reserveBike',
            toolInput: '{"bikeId":"bike-1"}',
          }),
        }));
      }
    });

    expect(screen.getByTestId('delivery-progress')).toHaveTextContent('In progress (2 updates)');
    expect(screen.getByText('Reserving bike for delivery: bike-1')).toBeInTheDocument();
  });

  it('shows delivered status when delivery is complete', async () => {
    const user = userEvent.setup();
    const { mockWs } = createMockWebSocket();

    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => ({ orderId: 'delivered-test-id', orderStatus: 'pending' }),
    });

    renderHome();
    await addItemToCart(user, 'Margherita', 1);
    await user.click(screen.getByRole('button', { name: /place order/i }));

    await waitFor(() => {
      expect(screen.getByText(/delivered-test-id/i)).toBeInTheDocument();
    });

    // Send COOKED event
    await act(async () => {
      if (mockWs.onmessage) {
        mockWs.onmessage(new MessageEvent('message', {
          data: JSON.stringify({
            orderId: 'delivered-test-id',
            status: 'COOKED',
            source: 'kitchen',
            timestamp: '2026-01-26T10:00:00Z',
          }),
        }));
      }
    });

    // Send delivery events
    await act(async () => {
      if (mockWs.onmessage) {
        mockWs.onmessage(new MessageEvent('message', {
          data: JSON.stringify({
            orderId: 'delivered-test-id',
            status: 'checking_bikes',
            source: 'delivery',
            timestamp: '2026-01-26T10:01:00Z',
            message: 'Checking available bikes',
          }),
        }));
      }
    });

    // Send DELIVERED event
    await act(async () => {
      if (mockWs.onmessage) {
        mockWs.onmessage(new MessageEvent('message', {
          data: JSON.stringify({
            orderId: 'delivered-test-id',
            status: 'DELIVERED',
            source: 'delivery',
            timestamp: '2026-01-26T10:05:00Z',
          }),
        }));
      }
    });

    expect(screen.getByTestId('delivery-progress')).toHaveTextContent('Delivered');
  });

  it('saves order to sessionStorage when order is created', async () => {
    const user = userEvent.setup();
    createMockWebSocket();

    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => ({ orderId: 'session-test-id', orderStatus: 'pending' }),
    });

    // Clear sessionStorage before test
    sessionStorage.clear();

    renderHome();
    await addItemToCart(user, 'Margherita', 1);
    await user.click(screen.getByRole('button', { name: /place order/i }));

    await waitFor(() => {
      expect(screen.getByText(/session-test-id/i)).toBeInTheDocument();
    });

    // Verify order was saved to sessionStorage
    const savedOrder = sessionStorage.getItem('currentOrder');
    expect(savedOrder).not.toBeNull();
    const parsed = JSON.parse(savedOrder!);
    expect(parsed.orderId).toBe('session-test-id');
  });

  it('restores order from sessionStorage on mount', async () => {
    createMockWebSocket();

    // Pre-populate sessionStorage with an existing order
    sessionStorage.setItem('currentOrder', JSON.stringify({
      orderId: 'restored-order-id',
      events: [
        {
          orderId: 'restored-order-id',
          status: 'checking_inventory',
          source: 'kitchen',
          timestamp: '2026-01-26T10:00:00Z',
          message: 'Checking inventory',
        },
      ],
    }));

    renderHome();

    // The restored order ID should be displayed
    await waitFor(() => {
      expect(screen.getByTestId('order-id')).toHaveTextContent('restored-order-id');
    });

    // The restored events should be displayed
    expect(screen.getByText('Checking inventory')).toBeInTheDocument();
  });

  it('displays error message when order fails', async () => {
    const user = userEvent.setup();
    createMockWebSocket();

    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: false,
      status: 500,
    });

    renderHome();

    // Add item to cart first
    await addItemToCart(user, 'Margherita', 1);

    const submitButton = screen.getByRole('button', { name: /place order/i });
    await user.click(submitButton);

    await waitFor(() => {
      expect(screen.getByText(/failed to place order/i)).toBeInTheDocument();
    });
  });
});
