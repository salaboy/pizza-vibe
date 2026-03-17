import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import InventoryPage from '@/app/inventory/page';

// Mock fetch
global.fetch = jest.fn();

// Mock data matching inventory service models
const mockInventory = [
  { item: 'Pepperoni', quantity: 10 },
  { item: 'Pineapple', quantity: 10 },
  { item: 'PizzaDough', quantity: 10 },
  { item: 'Mozzarella', quantity: 10 },
  { item: 'Sauce', quantity: 10 },
];

describe('Inventory Page', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders the inventory page title', async () => {
    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => mockInventory,
    });

    render(<InventoryPage />);

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /inventory/i })).toBeInTheDocument();
    });
  });

  it('displays a list of inventory items with their quantities', async () => {
    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => mockInventory,
    });

    render(<InventoryPage />);

    await waitFor(() => {
      expect(screen.getByText('Pepperoni')).toBeInTheDocument();
    });

    expect(screen.getByText('Pineapple')).toBeInTheDocument();
    expect(screen.getByText('PizzaDough')).toBeInTheDocument();
    expect(screen.getByText('Mozzarella')).toBeInTheDocument();
    expect(screen.getByText('Sauce')).toBeInTheDocument();
  });

  it('displays item quantities', async () => {
    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => mockInventory,
    });

    render(<InventoryPage />);

    await waitFor(() => {
      const quantities = screen.getAllByText('10');
      expect(quantities.length).toBe(5);
    });
  });

  it('displays loading state while fetching inventory', () => {
    (global.fetch as jest.Mock).mockImplementationOnce(
      () => new Promise(() => {}) // Never resolves
    );

    render(<InventoryPage />);

    expect(screen.getByText(/loading/i)).toBeInTheDocument();
  });

  it('displays error message when fetch fails', async () => {
    (global.fetch as jest.Mock).mockRejectedValueOnce(new Error('Failed to fetch'));

    render(<InventoryPage />);

    await waitFor(() => {
      expect(screen.getByText(/error/i)).toBeInTheDocument();
    });
  });

  it('shows acquire button for each inventory item', async () => {
    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => mockInventory,
    });

    render(<InventoryPage />);

    await waitFor(() => {
      const acquireButtons = screen.getAllByRole('button', { name: /acquire/i });
      expect(acquireButtons.length).toBe(5);
    });
  });

  it('acquires an item when acquire button is clicked', async () => {
    const user = userEvent.setup();

    (global.fetch as jest.Mock)
      .mockResolvedValueOnce({
        ok: true,
        json: async () => mockInventory,
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ item: 'Pepperoni', status: 'ACQUIRED', remainingQuantity: 9 }),
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => [
          { item: 'Pepperoni', quantity: 9 },
          { item: 'Pineapple', quantity: 10 },
          { item: 'PizzaDough', quantity: 10 },
          { item: 'Mozzarella', quantity: 10 },
          { item: 'Sauce', quantity: 10 },
        ],
      });

    render(<InventoryPage />);

    await waitFor(() => {
      expect(screen.getByText('Pepperoni')).toBeInTheDocument();
    });

    const acquireButtons = screen.getAllByRole('button', { name: /acquire/i });
    await user.click(acquireButtons[0]);

    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/inventory/Pepperoni'),
        expect.objectContaining({ method: 'POST' })
      );
    });
  });

  it('displays empty status when item quantity is 0', async () => {
    const inventoryWithEmpty = [
      { item: 'Pepperoni', quantity: 0 },
      { item: 'Pineapple', quantity: 10 },
      { item: 'PizzaDough', quantity: 10 },
      { item: 'Mozzarella', quantity: 10 },
      { item: 'Sauce', quantity: 10 },
    ];

    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => inventoryWithEmpty,
    });

    render(<InventoryPage />);

    await waitFor(() => {
      expect(screen.getByText('0')).toBeInTheDocument();
    });
  });

  it('shows add quantity section for each inventory item', async () => {
    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => mockInventory,
    });

    render(<InventoryPage />);

    await waitFor(() => {
      const addButtons = screen.getAllByRole('button', { name: /add/i });
      expect(addButtons.length).toBe(5);
    });
  });

  it('has input field for adding quantity to items', async () => {
    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => mockInventory,
    });

    render(<InventoryPage />);

    await waitFor(() => {
      const quantityInputs = screen.getAllByRole('spinbutton');
      expect(quantityInputs.length).toBe(5);
    });
  });

  it('adds quantity to an item when add button is clicked', async () => {
    const user = userEvent.setup();

    (global.fetch as jest.Mock)
      .mockResolvedValueOnce({
        ok: true,
        json: async () => mockInventory,
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ item: 'Pepperoni', quantity: 15 }),
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => [
          { item: 'Pepperoni', quantity: 15 },
          { item: 'Pineapple', quantity: 10 },
          { item: 'PizzaDough', quantity: 10 },
          { item: 'Mozzarella', quantity: 10 },
          { item: 'Sauce', quantity: 10 },
        ],
      });

    render(<InventoryPage />);

    await waitFor(() => {
      expect(screen.getByText('Pepperoni')).toBeInTheDocument();
    });

    const quantityInputs = screen.getAllByRole('spinbutton');
    await user.clear(quantityInputs[0]);
    await user.type(quantityInputs[0], '5');

    const addButtons = screen.getAllByRole('button', { name: /add/i });
    await user.click(addButtons[0]);

    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/inventory/Pepperoni/add'),
        expect.objectContaining({
          method: 'POST',
          body: JSON.stringify({ quantity: 5 }),
        })
      );
    });
  });
});
