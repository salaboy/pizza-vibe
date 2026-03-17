'use client';

import { createContext, useContext, useState, useRef, useCallback, useEffect, ReactNode } from 'react';

export interface WebSocketEvent {
  orderId: string;
  status: string;
  source: string;
  timestamp: string;
  message?: string;
  toolName?: string;
  toolInput?: string;
}

interface OrderContextValue {
  orderId: string | null;
  setOrderId: (id: string | null) => void;
  events: WebSocketEvent[];
  setEvents: React.Dispatch<React.SetStateAction<WebSocketEvent[]>>;
  wsConnected: boolean;
  connectWebSocket: (orderId: string) => Promise<void>;
}

const OrderContext = createContext<OrderContextValue | null>(null);

export function useOrder() {
  const ctx = useContext(OrderContext);
  if (!ctx) throw new Error('useOrder must be used within OrderProvider');
  return ctx;
}

export function OrderProvider({ children }: { children: ReactNode }) {
  const [orderId, setOrderIdState] = useState<string | null>(null);
  const [events, setEvents] = useState<WebSocketEvent[]>([]);
  const [wsConnected, setWsConnected] = useState(false);
  const wsRef = useRef<WebSocket | null>(null);

  const setOrderId = useCallback((id: string | null) => {
    setOrderIdState(id);
    if (id) {
      sessionStorage.setItem('currentOrder', JSON.stringify({ orderId: id, events: [] }));
    }
  }, []);

  // Restore order from sessionStorage on mount
  useEffect(() => {
    const saved = sessionStorage.getItem('currentOrder');
    if (saved) {
      try {
        const parsed = JSON.parse(saved);
        if (parsed.orderId) {
          setOrderIdState(parsed.orderId);
          if (parsed.events) {
            setEvents(parsed.events);
          }
          connectWebSocket(parsed.orderId).catch(() => {});
        }
      } catch {
        // ignore invalid data
      }
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Save events to sessionStorage when they change
  useEffect(() => {
    if (orderId) {
      sessionStorage.setItem('currentOrder', JSON.stringify({ orderId, events }));
    }
  }, [orderId, events]);

  const connectWebSocket = useCallback((orderId: string): Promise<void> => {
    return new Promise((resolve, reject) => {
      const storeWsUrl = process.env.NEXT_PUBLIC_STORE_WS_URL || 'ws://localhost:8080';
      const wsUrl = `${storeWsUrl}/ws?orderId=${orderId}`;
      const ws = new WebSocket(wsUrl);

      ws.onopen = () => {
        setWsConnected(true);
        resolve();
      };

      ws.onmessage = (event: MessageEvent) => {
        const data: WebSocketEvent = JSON.parse(event.data);
        if (data.status === 'tool_completed') return;
        setEvents((prev) => [...prev, data]);
      };

      ws.onclose = () => {
        setWsConnected(false);
      };

      ws.onerror = () => {
        setWsConnected(false);
        reject(new Error('WebSocket connection failed'));
      };

      wsRef.current = ws;
    });
  }, []);

  return (
    <OrderContext.Provider value={{ orderId, setOrderId, events, setEvents, wsConnected, connectWebSocket }}>
      {children}
    </OrderContext.Provider>
  );
}
