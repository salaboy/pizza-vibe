'use client';

import { useState, useEffect } from 'react';

interface OrderItem {
  pizzaType: string;
  quantity: number;
}

interface KitchenOrder {
  orderId: string;
  orderItems: OrderItem[];
  status: string;
}

export default function KitchenPage() {
  const [orders, setOrders] = useState<KitchenOrder[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchOrders = async () => {
    try {
      const response = await fetch('/api/kitchen/orders');
      if (!response.ok) {
        throw new Error('Failed to fetch orders');
      }
      const data = await response.json();
      setOrders(data.orders || []);
      setLoading(false);
    } catch (err) {
      setError('Error loading kitchen orders');
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOrders();
    const interval = setInterval(fetchOrders, 2000);
    return () => clearInterval(interval);
  }, []);

  if (loading) {
    return (
      <main>
        <h1>Kitchen</h1>
        <p>Loading...</p>
      </main>
    );
  }

  if (error) {
    return (
      <main>
        <h1>Kitchen</h1>
        <p>Error: {error}</p>
      </main>
    );
  }

  return (
    <main>
      <h1>Kitchen</h1>
      {orders.length === 0 ? (
        <p>No orders in the kitchen</p>
      ) : (
        <div>
          {orders.map((order) => (
            <div key={order.orderId}>
              <h3>{order.orderId}</h3>
              <p>Status: {order.status}</p>
              <ul>
                {order.orderItems.map((item, index) => (
                  <li key={index}>
                    {item.quantity}x {item.pizzaType}
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      )}
    </main>
  );
}
