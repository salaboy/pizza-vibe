'use client';

import { useState, useEffect } from 'react';

interface ServiceStatus {
  name: string;
  status: 'healthy' | 'unhealthy';
  url: string;
}

interface OrderItem {
  pizzaType: string;
  quantity: number;
}

interface Order {
  orderId: string;
  orderItems: OrderItem[];
  orderData: string;
  orderStatus: string;
}

interface OrderEvent {
  orderId: string;
  status: string;
  source: string;
  message?: string;
  toolName?: string;
  toolInput?: string;
}

export default function ManagementPage() {
  const [services, setServices] = useState<ServiceStatus[]>([
    { name: 'Store Service', status: 'unhealthy', url: '/api/health/store' },
  ]);
  const [loading, setLoading] = useState(true);
  const [orders, setOrders] = useState<Order[]>([]);
  const [selectedOrderId, setSelectedOrderId] = useState<string | null>(null);
  const [events, setEvents] = useState<OrderEvent[]>([]);

  useEffect(() => {
    const checkServices = async () => {
      const updatedServices = await Promise.all(
        services.map(async (service) => {
          try {
            const response = await fetch(service.url);
            return {
              ...service,
              status: response.ok ? ('healthy' as const) : ('unhealthy' as const),
            };
          } catch {
            return {
              ...service,
              status: 'unhealthy' as const,
            };
          }
        })
      );

      setServices(updatedServices);
      setLoading(false);
    };

    const fetchOrders = async () => {
      try {
        const response = await fetch('/api/orders');
        if (response.ok) {
          const data = await response.json();
          setOrders(data);
        }
      } catch {
        // Handle error silently
      }
    };

    checkServices();
    fetchOrders();
    const interval = setInterval(() => {
      checkServices();
      fetchOrders();
    }, 2000);
    return () => clearInterval(interval);
  }, []);

  const handleOrderClick = async (orderId: string) => {
    setSelectedOrderId(orderId);
    try {
      const response = await fetch(`/api/events?orderId=${orderId}`);
      if (response.ok) {
        const data = await response.json();
        setEvents(data);
      }
    } catch {
      setEvents([]);
    }
  };

  if (loading) {
    return (
      <main>
        <h1>Management</h1>
        <p>Checking services...</p>
      </main>
    );
  }

  return (
    <main>
      <h1>Management</h1>
      <h2>Service Status</h2>
      <div>
        {services.map((service) => (
          <div key={service.name}>
            <h3>{service.name}</h3>
            <p>Status: {service.status}</p>
          </div>
        ))}
      </div>

      <h2>Orders</h2>
      {orders.length === 0 ? (
        <p>No orders found</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>Order ID</th>
              <th>Items</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {orders.map((order) => (
              <tr
                key={order.orderId}
                data-testid="order-row"
                onClick={() => handleOrderClick(order.orderId)}
                style={{ cursor: 'pointer' }}
              >
                <td>{order.orderId.substring(0, 8)}...</td>
                <td>
                  {order.orderItems.map((item, idx) => (
                    <span key={idx}>
                      {item.pizzaType} x{item.quantity}
                      {idx < order.orderItems.length - 1 ? ', ' : ''}
                    </span>
                  ))}
                </td>
                <td>{order.orderStatus}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {selectedOrderId && (
        <>
          <h2>Events</h2>
          {events.length === 0 ? (
            <p>No events for this order</p>
          ) : (
            <table data-testid="events-detail-table">
              <thead>
                <tr>
                  <th>Status</th>
                  <th>Source</th>
                  <th>Details</th>
                </tr>
              </thead>
              <tbody>
                {events.map((event, idx) => (
                  <tr key={idx}>
                    <td>{event.status}</td>
                    <td>{event.source}</td>
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
        </>
      )}
    </main>
  );
}
