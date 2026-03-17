'use client';

import { useState, useEffect } from 'react';

interface DrinksStockItem {
  item: string;
  quantity: number;
}

export default function DrinksStockPage() {
  const [stock, setStock] = useState<DrinksStockItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [addQuantities, setAddQuantities] = useState<{ [key: string]: number }>({});

  const fetchStock = async () => {
    try {
      const response = await fetch('/api/drinks-stock');
      if (!response.ok) {
        throw new Error('Failed to fetch drinks stock');
      }
      const data = await response.json();
      setStock(data);
      setLoading(false);
    } catch (err) {
      setError('Error loading drinks stock');
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchStock();
    const interval = setInterval(fetchStock, 2000);
    return () => clearInterval(interval);
  }, []);

  const handleAcquire = async (itemName: string) => {
    try {
      const response = await fetch(`/api/drinks-stock/${itemName}`, {
        method: 'POST',
      });
      if (response.ok) {
        fetchStock();
      }
    } catch (err) {
      setError('Error acquiring item');
    }
  };

  const handleAddQuantity = async (itemName: string) => {
    const quantity = addQuantities[itemName] || 0;
    if (quantity <= 0) return;

    try {
      const response = await fetch(`/api/drinks-stock/${itemName}/add`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ quantity }),
      });
      if (response.ok) {
        setAddQuantities({ ...addQuantities, [itemName]: 0 });
        fetchStock();
      }
    } catch (err) {
      setError('Error adding quantity');
    }
  };

  const handleQuantityChange = (itemName: string, value: number) => {
    setAddQuantities({ ...addQuantities, [itemName]: value });
  };

  if (loading) {
    return <div>Loading...</div>;
  }

  if (error) {
    return <div>Error: {error}</div>;
  }

  return (
    <div>
      <h1>Drinks Stock</h1>
      <table>
        <thead>
          <tr>
            <th>Item</th>
            <th>Quantity</th>
            <th>Actions</th>
            <th>Add Quantity</th>
          </tr>
        </thead>
        <tbody>
          {stock.map((item) => (
            <tr key={item.item}>
              <td>{item.item}</td>
              <td>{item.quantity}</td>
              <td>
                <button onClick={() => handleAcquire(item.item)}>Acquire</button>
              </td>
              <td>
                <input
                  type="number"
                  min="0"
                  value={addQuantities[item.item] || 0}
                  onChange={(e) => handleQuantityChange(item.item, parseInt(e.target.value) || 0)}
                />
                <button onClick={() => handleAddQuantity(item.item)}>Add</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
