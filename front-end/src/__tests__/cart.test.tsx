import { render, screen, within } from '@testing-library/react';
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

function renderHome() {
  return render(
    <OrderProvider>
      <Home />
    </OrderProvider>
  );
}

describe('Cart Functionality', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    sessionStorage.clear();
    // Mock drinks-stock fetch that fires on component mount
    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => [],
    });
  });

  it('displays an Add to Cart button', () => {
    renderHome();
    expect(screen.getByRole('button', { name: /add to cart/i })).toBeInTheDocument();
  });

  it('adds an item to the cart when Add to Cart is clicked', async () => {
    const user = userEvent.setup();
    renderHome();

    const pizzaSelect = screen.getByLabelText(/pizza type/i);
    const quantityInput = screen.getByLabelText(/quantity/i);
    const addToCartButton = screen.getByRole('button', { name: /add to cart/i });

    await user.selectOptions(pizzaSelect, 'Pepperoni');
    await user.tripleClick(quantityInput);
    await user.keyboard('3');
    await user.click(addToCartButton);

    // Cart should show the added item
    const cart = screen.getByTestId('cart');
    expect(within(cart).getByText('Pepperoni')).toBeInTheDocument();
    expect(within(cart).getByText('3')).toBeInTheDocument();
  });

  it('adds multiple different pizza types to the cart', async () => {
    const user = userEvent.setup();
    renderHome();

    const pizzaSelect = screen.getByLabelText(/pizza type/i);
    const quantityInput = screen.getByLabelText(/quantity/i);
    const addToCartButton = screen.getByRole('button', { name: /add to cart/i });

    // Add Margherita
    await user.selectOptions(pizzaSelect, 'Margherita');
    await user.tripleClick(quantityInput);
    await user.keyboard('2');
    await user.click(addToCartButton);

    // Add Hawaiian
    await user.selectOptions(pizzaSelect, 'Hawaiian');
    await user.tripleClick(quantityInput);
    await user.keyboard('1');
    await user.click(addToCartButton);

    const cart = screen.getByTestId('cart');
    expect(within(cart).getByText('Margherita')).toBeInTheDocument();
    expect(within(cart).getByText('Hawaiian')).toBeInTheDocument();
  });

  it('removes an item from the cart', async () => {
    const user = userEvent.setup();
    renderHome();

    const pizzaSelect = screen.getByLabelText(/pizza type/i);
    const quantityInput = screen.getByLabelText(/quantity/i);
    const addToCartButton = screen.getByRole('button', { name: /add to cart/i });

    // Add Margherita
    await user.selectOptions(pizzaSelect, 'Margherita');
    await user.tripleClick(quantityInput);
    await user.keyboard('2');
    await user.click(addToCartButton);

    // Add Pepperoni
    await user.selectOptions(pizzaSelect, 'Pepperoni');
    await user.tripleClick(quantityInput);
    await user.keyboard('1');
    await user.click(addToCartButton);

    // Remove Margherita
    const cart = screen.getByTestId('cart');
    const margheritaRow = within(cart).getByText('Margherita').closest('tr')!;
    const removeButton = within(margheritaRow).getByRole('button', { name: /remove/i });
    await user.click(removeButton);

    // Margherita should be gone, Pepperoni should remain
    expect(within(cart).queryByText('Margherita')).not.toBeInTheDocument();
    expect(within(cart).getByText('Pepperoni')).toBeInTheDocument();
  });

  it('updates the quantity of a cart item when same pizza type is added again', async () => {
    const user = userEvent.setup();
    renderHome();

    const pizzaSelect = screen.getByLabelText(/pizza type/i);
    const quantityInput = screen.getByLabelText(/quantity/i);
    const addToCartButton = screen.getByRole('button', { name: /add to cart/i });

    // Add Margherita with quantity 2
    await user.selectOptions(pizzaSelect, 'Margherita');
    await user.tripleClick(quantityInput);
    await user.keyboard('2');
    await user.click(addToCartButton);

    // Add Margherita again with quantity 3
    await user.selectOptions(pizzaSelect, 'Margherita');
    await user.tripleClick(quantityInput);
    await user.keyboard('3');
    await user.click(addToCartButton);

    // Quantity should be updated to 5 (2 + 3)
    const cart = screen.getByTestId('cart');
    const margheritaRow = within(cart).getByText('Margherita').closest('tr')!;
    expect(within(margheritaRow).getByText('5')).toBeInTheDocument();
  });

  it('places order with all cart items', async () => {
    const user = userEvent.setup();
    createMockWebSocket();

    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => ({ orderId: 'cart-order-id', orderStatus: 'pending' }),
    });

    renderHome();

    const pizzaSelect = screen.getByLabelText(/pizza type/i);
    const quantityInput = screen.getByLabelText(/quantity/i);
    const addToCartButton = screen.getByRole('button', { name: /add to cart/i });

    // Add Margherita x2
    await user.selectOptions(pizzaSelect, 'Margherita');
    await user.tripleClick(quantityInput);
    await user.keyboard('2');
    await user.click(addToCartButton);

    // Add Pepperoni x1
    await user.selectOptions(pizzaSelect, 'Pepperoni');
    await user.tripleClick(quantityInput);
    await user.keyboard('1');
    await user.click(addToCartButton);

    // Place order
    const placeOrderButton = screen.getByRole('button', { name: /place order/i });
    await user.click(placeOrderButton);

    expect(global.fetch).toHaveBeenCalledWith('/api/order', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        orderItems: [
          { pizzaType: 'Margherita', quantity: 2 },
          { pizzaType: 'Pepperoni', quantity: 1 },
        ],
      }),
    });
  });

  it('does not show cart when no items are added', () => {
    renderHome();
    expect(screen.queryByTestId('cart')).not.toBeInTheDocument();
  });

  it('clears the cart after placing order', async () => {
    const user = userEvent.setup();
    createMockWebSocket();

    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => ({ orderId: 'clear-cart-id', orderStatus: 'pending' }),
    });

    renderHome();

    const pizzaSelect = screen.getByLabelText(/pizza type/i);
    const quantityInput = screen.getByLabelText(/quantity/i);
    const addToCartButton = screen.getByRole('button', { name: /add to cart/i });

    // Add item to cart
    await user.selectOptions(pizzaSelect, 'Margherita');
    await user.tripleClick(quantityInput);
    await user.keyboard('1');
    await user.click(addToCartButton);

    // Place order
    const placeOrderButton = screen.getByRole('button', { name: /place order/i });
    await user.click(placeOrderButton);

    // Wait for order to be placed
    const orderIdEl = await screen.findByTestId('order-id');
    expect(orderIdEl).toBeInTheDocument();

    // Cart should be cleared
    expect(screen.queryByTestId('cart')).not.toBeInTheDocument();
  });

  it('disables Place Order when cart is empty', () => {
    renderHome();
    const placeOrderButton = screen.getByRole('button', { name: /place order/i });
    expect(placeOrderButton).toBeDisabled();
  });
});
