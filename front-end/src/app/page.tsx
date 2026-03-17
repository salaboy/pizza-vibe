'use client';

import { useState, useEffect, FormEvent } from 'react';
import { useOrder } from '@/context/OrderContext';

interface OrderItem {
  pizzaType: string;
  quantity: number;
}

interface DrinkItem {
  drinkType: string;
  quantity: number;
}

interface DrinksStockItem {
  item: string;
  quantity: number;
}

export default function Home() {
  const [pizzaType, setPizzaType] = useState('Margherita');
  const [quantity, setQuantity] = useState(1);
  const [cart, setCart] = useState<OrderItem[]>([]);
  const [drinkType, setDrinkType] = useState('');
  const [drinkQuantity, setDrinkQuantity] = useState(1);
  const [drinkCart, setDrinkCart] = useState<DrinkItem[]>([]);
  const [availableDrinks, setAvailableDrinks] = useState<DrinksStockItem[]>([]);
  const [message, setMessage] = useState<string | null>(null);
  const [isError, setIsError] = useState(false);

  const { orderId, setOrderId, events, setEvents, wsConnected, connectWebSocket } = useOrder();

  useEffect(() => {
    const loadDrinks = async () => {
      try {
        const res = await fetch('/api/drinks-stock');
        if (!res || !res.ok) return;
        const data = await res.json();
        if (!Array.isArray(data)) return;
        setAvailableDrinks(data);
        if (data.length > 0) {
          setDrinkType(data[0].item);
        }
      } catch {
        // Drinks stock service unavailable
      }
    };
    loadDrinks();
  }, []);

  const handleAddToCart = () => {
    setCart((prevCart) => {
      const existingIndex = prevCart.findIndex(
        (item) => item.pizzaType === pizzaType
      );
      if (existingIndex >= 0) {
        const updated = [...prevCart];
        updated[existingIndex] = {
          ...updated[existingIndex],
          quantity: updated[existingIndex].quantity + quantity,
        };
        return updated;
      }
      return [...prevCart, { pizzaType, quantity }];
    });
  };

  const handleRemoveFromCart = (pizzaTypeToRemove: string) => {
    setCart((prevCart) =>
      prevCart.filter((item) => item.pizzaType !== pizzaTypeToRemove)
    );
  };

  const handleAddDrinkToCart = () => {
    if (!drinkType) return;
    setDrinkCart((prev) => {
      const existingIndex = prev.findIndex((item) => item.drinkType === drinkType);
      if (existingIndex >= 0) {
        const updated = [...prev];
        updated[existingIndex] = {
          ...updated[existingIndex],
          quantity: updated[existingIndex].quantity + drinkQuantity,
        };
        return updated;
      }
      return [...prev, { drinkType, quantity: drinkQuantity }];
    });
  };

  const handleRemoveFromDrinkCart = (drinkTypeToRemove: string) => {
    setDrinkCart((prev) =>
      prev.filter((item) => item.drinkType !== drinkTypeToRemove)
    );
  };

  const hasItems = cart.length > 0 || drinkCart.length > 0;

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!hasItems) return;

    setMessage(null);
    setIsError(false);
    setOrderId(null);
    setEvents([]);

    const orderBody: { orderItems: OrderItem[]; drinkItems?: DrinkItem[] } = {
      orderItems: cart,
    };
    if (drinkCart.length > 0) {
      orderBody.drinkItems = drinkCart;
    }

    try {
      const response = await fetch('/api/order', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(orderBody),
      });

      if (response.ok) {
        const data = await response.json();
        setOrderId(data.orderId);
        // Connect WebSocket keyed to this specific orderId so only events for
        // this order are delivered to this frontend instance.
        await connectWebSocket(data.orderId);
        setMessage('Order placed successfully!');
        setIsError(false);
        setCart([]);
        setDrinkCart([]);
      } else {
        setMessage('Failed to place order');
        setIsError(true);
      }
    } catch {
      setMessage('Failed to place order');
      setIsError(true);
    }
  };

  const kitchenEvents = events.filter((e) => e.source === 'kitchen');
  const deliveryEvents = events.filter((e) => e.source === 'delivery');
  const isCooked = events.some((e) => e.status === 'COOKED');
  const isDelivered = events.some((e) => e.status === 'DELIVERED');
  const orderStatus = orderId
    ? isDelivered ? 'DELIVERED' : isCooked ? 'COOKED' : 'PENDING'
    : null;

  return (
    <main>
      <h1>Pizza Vibe</h1>
      <form onSubmit={handleSubmit}>
        <div>
          <label htmlFor="pizzaType">Pizza Type</label>
          <select
            id="pizzaType"
            value={pizzaType}
            onChange={(e) => setPizzaType(e.target.value)}
          >
            <option value="Margherita">Margherita</option>
            <option value="Pepperoni">Pepperoni</option>
            <option value="Hawaiian">Hawaiian</option>
            <option value="Veggie">Veggie</option>
          </select>
        </div>
        <div>
          <label htmlFor="quantity">Quantity</label>
          <input
            id="quantity"
            type="number"
            min="1"
            value={quantity}
            onChange={(e) => setQuantity(parseInt(e.target.value, 10) || 1)}
          />
        </div>
        <button type="button" onClick={handleAddToCart}>Add to Cart</button>
        {availableDrinks.length > 0 && (
          <>
            <div>
              <label htmlFor="drinkType">Drink</label>
              <select
                id="drinkType"
                value={drinkType}
                onChange={(e) => setDrinkType(e.target.value)}
              >
                {availableDrinks.map((drink) => (
                  <option key={drink.item} value={drink.item}>
                    {drink.item}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label htmlFor="drinkQuantity">Drink Qty</label>
              <input
                id="drinkQuantity"
                type="number"
                min="1"
                value={drinkQuantity}
                onChange={(e) => setDrinkQuantity(parseInt(e.target.value, 10) || 1)}
              />
            </div>
            <button type="button" onClick={handleAddDrinkToCart}>Add Drink</button>
          </>
        )}
        {hasItems && (
          <table data-testid="cart">
            <thead>
              <tr>
                <th>Item</th>
                <th>Quantity</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {cart.map((item) => (
                <tr key={`pizza-${item.pizzaType}`}>
                  <td>{item.pizzaType}</td>
                  <td>{item.quantity}</td>
                  <td>
                    <button
                      type="button"
                      onClick={() => handleRemoveFromCart(item.pizzaType)}
                    >
                      Remove
                    </button>
                  </td>
                </tr>
              ))}
              {drinkCart.map((item) => (
                <tr key={`drink-${item.drinkType}`}>
                  <td>{item.drinkType}</td>
                  <td>{item.quantity}</td>
                  <td>
                    <button
                      type="button"
                      onClick={() => handleRemoveFromDrinkCart(item.drinkType)}
                    >
                      Remove
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        <button type="submit" disabled={!hasItems}>Place Order</button>
      </form>
      {message && (
        <p role="status" style={{ color: isError ? 'red' : 'green' }}>
          {message}
        </p>
      )}
      {orderId && (
        <p data-testid="order-id">Order ID: {orderId}</p>
      )}
      {orderStatus && (
        <p data-testid="order-status">Order Status: {orderStatus}</p>
      )}
      {orderId && (
        <p data-testid="ws-status">
          WebSocket: {wsConnected ? 'Connected' : 'Disconnected'}
        </p>
      )}
      {events.length > 0 && (() => {
        const latestOvenProgress = kitchenEvents
          .filter((e) => e.status === 'oven_progress')
          .slice(-1)[0];
        const ovenPercent = latestOvenProgress?.message
          ? parseInt(latestOvenProgress.message.match(/(\d+)% complete/)?.[1] || '0', 10)
          : null;
        const progressPercent = isCooked ? 100 : (ovenPercent !== null ? Math.min(99, ovenPercent) : Math.min(99, kitchenEvents.length * 20));
        return (
          <p data-testid="cooking-progress">
            Cooking progress: {progressPercent}%
          </p>
        );
      })()}
      {isCooked && (
        <p data-testid="delivery-progress">
          Delivery: {isDelivered ? 'Delivered' : `In progress (${deliveryEvents.filter((e) => e.status !== 'DELIVERED').length} updates)`}
        </p>
      )}
      {events.length > 0 && (
        <table data-testid="events-table">
          <thead>
            <tr>
              <th>Order ID</th>
              <th>Status</th>
              <th>Source</th>
              <th>Timestamp</th>
              <th>Details</th>
            </tr>
          </thead>
          <tbody>
            {events.map((event, index) => (
              <tr key={index}>
                <td>{event.orderId}</td>
                <td>{event.status}</td>
                <td>{event.source}</td>
                <td>{event.timestamp}</td>
                <td>
                  {event.message && <span>{event.message}</span>}
                  {event.toolName && <span> [{event.toolName}]</span>}
                  {event.toolInput && <span> {event.toolInput}</span>}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </main>
  );
}
