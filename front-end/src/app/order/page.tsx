'use client';

import { useState } from 'react';
import styles from './page.module.css';
import Tabs from '@/components/Tabs';
import PizzaItem from '@/components/PizzaItem';
import Button from '@/components/Button';
import CartItem from '@/components/CartItem';
import EmptyBlock from '@/components/EmptyBlock';
import { useOrder } from '@/context/OrderContext';

interface OrderItem {
  pizzaType: string;
  quantity: number;
}

const PIZZAS = [
  {
    name: 'Margherita',
    price: 10,
    description: 'San Marzano tomatoes, mozzarella cheese, fresh basil, salt, and extra-virgin olive oil',
    image: '/images/pizza-margherita.svg',
  },
  {
    name: 'Pepperoni',
    price: 15,
    description: 'Mozzarella cheese, pepperoni slices, olive oil, salt, and pepper',
    image: '/images/pizza-pepperoni.svg',
  },
  {
    name: 'Hawaiian',
    price: 15,
    description: 'Tomato sauce, mozzarella cheese, cooked ham, pineapple',
    image: '/images/pizza-hawaiian.svg',
  },
  {
    name: 'Vegan',
    price: 12,
    description: 'Vegan cheese, tomato sauce, mushrooms, onions, green peppers, and black olives',
    image: '/images/pizza-vegan.svg',
  },
];

function getPizzaPrice(pizzaType: string): number {
  return PIZZAS.find((p) => p.name === pizzaType)?.price ?? 0;
}

export default function OrderPage() {
  const [activeTab, setActiveTab] = useState('new');
  const [cart, setCart] = useState<OrderItem[]>([]);
  const [message, setMessage] = useState<string | null>(null);
  const [isError, setIsError] = useState(false);
  const [orderDelivered, setOrderDelivered] = useState(true);

  const { orderId, setOrderId, events, setEvents, wsConnected, connectWebSocket } = useOrder();

  const totalQuantity = cart.reduce((sum, item) => sum + item.quantity, 0);
  const totalPrice = cart.reduce(
    (sum, item) => sum + item.quantity * getPizzaPrice(item.pizzaType),
    0
  );

  const kitchenEvents = events.filter((e) => e.source === 'kitchen');
  const deliveryEvents = events.filter((e) => e.source === 'delivery');
  const isCooked = events.some((e) => e.status === 'COOKED');
  const isDelivered = events.some((e) => e.status === 'DELIVERED');

  const handleAddPizza = (pizzaName: string) => {
    setCart((prevCart) => {
      const existingIndex = prevCart.findIndex(
        (item) => item.pizzaType === pizzaName
      );
      if (existingIndex >= 0) {
        const updated = [...prevCart];
        updated[existingIndex] = {
          ...updated[existingIndex],
          quantity: updated[existingIndex].quantity + 1,
        };
        return updated;
      }
      return [...prevCart, { pizzaType: pizzaName, quantity: 1 }];
    });
  };

  const handleQuantityChange = (pizzaType: string, newQuantity: number) => {
    setCart((prevCart) =>
      prevCart.map((item) =>
        item.pizzaType === pizzaType
          ? { ...item, quantity: newQuantity }
          : item
      )
    );
  };

  const handleDeleteItem = (pizzaType: string) => {
    setCart((prevCart) =>
      prevCart.filter((item) => item.pizzaType !== pizzaType)
    );
  };

  const handlePlaceOrder = async () => {
    if (cart.length === 0) return;

    setMessage(null);
    setIsError(false);
    setOrderId(null);
    setEvents([]);
    setOrderDelivered(false);

    try {
      const response = await fetch('/api/order', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          orderItems: cart,
        }),
      });

      if (response.ok) {
        const data = await response.json();
        setOrderId(data.orderId);
        await connectWebSocket(data.orderId);
        setMessage('Order placed successfully!');
        setIsError(false);
        setCart([]);
        setActiveTab('orders');
      } else {
        setMessage('Failed to place order');
        setIsError(true);
      }
    } catch {
      setMessage('Failed to place order');
      setIsError(true);
    }
  };

  return (
    <main>
      <Tabs
        key={activeTab}
        tabs={[
          { label: 'New Order', value: 'new', disabled: !orderDelivered },
          { label: 'Your Orders', value: 'orders' },
        ]}
        defaultValue={activeTab}
        onTabChange={setActiveTab}
      />

      {activeTab === 'new' && (
        <div className={styles.mainContent}>
          <div className={styles.contentWrapper}>
            <section className={styles.orderSection}>
              <div className={styles.orderInfo}>
                <h2>Order Here</h2>
                <p className={styles.orderInfoDescription}>
                  Click in a Pizza to add it to your cart.
                  <br />
                  Press as many times as you want.
                </p>
              </div>
              <div className={styles.pizzaGrid}>
                {PIZZAS.map((pizza) => (
                  <PizzaItem
                    key={pizza.name}
                    name={pizza.name}
                    price={pizza.price}
                    description={pizza.description}
                    image={pizza.image}
                    onAdd={() => handleAddPizza(pizza.name)}
                    className={styles.pizzaGridItem}
                  />
                ))}
              </div>
            </section>

            <div className={styles.divider} />

            <section className={styles.cartSection}>
              <div className={styles.cartWrapper}>
                <div className={styles.cartTextContainer}>
                  <h2>Cart</h2>
                  <p className={styles.cartDescription}>
                    {cart.length === 0
                      ? 'Your cart is empty'
                      : `${totalQuantity} pizzas in the cart`}
                  </p>
                </div>
                <Button
                  disabled={cart.length === 0}
                  onClick={handlePlaceOrder}
                >
                  {cart.length === 0
                    ? 'Place Order'
                    : `Place Order - $${totalPrice}`}
                </Button>
              </div>

              {message && isError && (
                <p role="status">
                  {message}
                </p>
              )}

              {cart.length === 0 ? (
                <EmptyBlock className={styles.emptyBlockFill} />
              ) : (
                <div className={styles.cartItems}>
                  {cart.map((item) => (
                    <CartItem
                      key={`${item.pizzaType}-${item.quantity}`}
                      name={item.pizzaType}
                      unitPrice={getPizzaPrice(item.pizzaType)}
                      quantity={item.quantity}
                      onQuantityChange={(newQty) =>
                        handleQuantityChange(item.pizzaType, newQty)
                      }
                      onDelete={() => handleDeleteItem(item.pizzaType)}
                    />
                  ))}
                </div>
              )}
            </section>
          </div>
        </div>
      )}

      {activeTab === 'orders' && (
        <div className={styles.mainContent}>
          {message && (
            <p role="status" className={styles.statusMessage}>
              {message}
            </p>
          )}
          {orderId && (
            <div className={styles.orderInfo}>
              <p data-testid="order-id">Order ID: {orderId}</p>
              <p data-testid="ws-status">
                WebSocket: {wsConnected ? 'Connected' : 'Disconnected'}
              </p>
              <p data-testid="order-status">
                Order Status: {isDelivered ? 'DELIVERED' : isCooked ? 'COOKED' : 'PENDING'}
              </p>
            </div>
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
              <p data-testid="cooking-progress" className={styles.statusMessage}>
                Cooking progress: {progressPercent}%
              </p>
            );
          })()}
          {isCooked && (
            <p data-testid="delivery-progress" className={styles.statusMessage}>
              Delivery: {isDelivered ? 'Delivered' : `In progress (${deliveryEvents.filter((e) => e.status !== 'DELIVERED').length} updates)`}
            </p>
          )}
          {events.length > 0 && (
            <div className={styles.eventsSection}>
              <table className={styles.eventsTable} data-testid="events-table">
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
            </div>
          )}
        </div>
      )}
    </main>
  );
}
