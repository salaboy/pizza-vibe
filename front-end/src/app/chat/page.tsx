'use client';

import { useState, useRef, useEffect, useCallback } from 'react';
import Chat, { ChatMessage } from '@/components/Chat';
import { useOrder } from '@/context/OrderContext';
import styles from './page.module.css';

const STORE_SERVICE_URL = process.env.NEXT_PUBLIC_STORE_SERVICE_URL || 'http://localhost:8080';

// Extract a UUID from text (used to detect orderId in bot responses)
function extractOrderId(text: string): string | null {
  const match = text.match(/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/i);
  return match ? match[0] : null;
}

export default function ChatPage() {
  const [messages, setMessages] = useState<ChatMessage[]>([
    { id: '1', content: 'Welcome to Pizza Vibe! Tell me what you would like to order and I will help you put together the perfect pizza and drinks order.', type: 'bot' },
  ]);
  const [inputValue, setInputValue] = useState('');
  const [inputDisabled, setInputDisabled] = useState(false);
  const sessionIdRef = useRef<string>(crypto.randomUUID());
  const trackedOrderIdRef = useRef<string | null>(null);

  const { setOrderId, events, connectWebSocket } = useOrder();

  // Group events by category into a single updating message per category
  const EVENT_GROUP_KITCHEN = 'event-group-kitchen';
  const EVENT_GROUP_INVENTORY = 'event-group-inventory';
  const EVENT_GROUP_DELIVERY = 'event-group-delivery';
  const DELIVERED_MSG_ID = 'event-delivered';

  type EventGroup = 'kitchen' | 'inventory' | 'delivery';

  function getEventGroup(source: string): EventGroup | null {
    const s = source.toLowerCase();
    if (s === 'kitchen') return 'kitchen';
    if (s === 'drinks' || s === 'drinks-stock' || s === 'inventory') return 'inventory';
    if (s === 'bikes' || s === 'delivery') return 'delivery';
    return null;
  }

  function extractPercent(msg: string | undefined): number | null {
    if (!msg) return null;
    const match = msg.match(/(\d+)%/);
    return match ? parseInt(match[1], 10) : null;
  }

  function buildKitchenContent(groupEvents: typeof events): string {
    const latest = groupEvents[groupEvents.length - 1];
    if (latest.status === 'COOKED') return '🍕 Kitchen: Done! Pizza is ready! ✅';
    if (latest.status === 'COOKING_ERROR') return '🍕 Kitchen: Something went wrong ❌';
    const pct = extractPercent(latest.message);
    if (pct !== null) return `🍕 Kitchen: Cooking... ${pct}%`;
    if (latest.status === 'RESERVING_OVEN') return '🍕 Kitchen: Warming up the oven...';
    if (latest.status === 'RELEASING_OVEN') return '🍕 Kitchen: Cleaning up the oven...';
    if (latest.status === 'COOKING') return '🍕 Kitchen: Cooking...';
    return `🍕 Kitchen: ${latest.status}`;
  }

  function buildInventoryContent(groupEvents: typeof events): string {
    const acquired = groupEvents.filter(e => e.status === 'ACQUIRED');
    const empty = groupEvents.filter(e => e.status === 'EMPTY');
    const items = acquired.map(e => e.message || 'item').join(', ');
    let content = `📦 Inventory: ${acquired.length} item${acquired.length !== 1 ? 's' : ''} ready`;
    if (items) content += ` (${items})`;
    if (empty.length > 0) content += ` ⚠️ ${empty.length} out of stock`;
    content += acquired.length > 0 && empty.length === 0 ? ' ✅' : '';
    return content;
  }

  function buildDeliveryContent(groupEvents: typeof events): string {
    const latest = groupEvents[groupEvents.length - 1];
    if (latest.status === 'DELIVERED') return '🚲 Delivery: Arrived! ✅';
    if (latest.status === 'DELIVERY_ERROR') return '🚲 Delivery: Something went wrong ❌';
    const pct = extractPercent(latest.message);
    if (pct !== null) return `🚲 Delivery: On the way... ${pct}%`;
    if (latest.status === 'ON_ROUTE' || latest.status === 'DELIVERING') return '🚲 Delivery: On the way...';
    return `🚲 Delivery: ${latest.status}`;
  }

  const lastEventCountRef = useRef(0);
  const deliveredAddedRef = useRef(false);
  useEffect(() => {
    if (events.length <= lastEventCountRef.current) return;
    lastEventCountRef.current = events.length;

    // Collect events per group
    const kitchenEvents: typeof events = [];
    const inventoryEvents: typeof events = [];
    const deliveryEvents: typeof events = [];

    for (const event of events) {
      const group = getEventGroup(event.source);
      if (group === 'kitchen') kitchenEvents.push(event);
      else if (group === 'inventory') inventoryEvents.push(event);
      else if (group === 'delivery') deliveryEvents.push(event);
    }

    // Check for final DELIVERED from store-mgmt-agent
    const isDelivered = events.some(
      e => e.status === 'DELIVERED' && (e.source === 'store-mgmt-agent' || e.source === 'delivery' || e.source === 'bikes')
    );

    setMessages(prev => {
      let updated = [...prev];

      // Helper to upsert a group message
      const upsert = (id: string, content: string) => {
        const idx = updated.findIndex(m => m.id === id);
        if (idx >= 0) {
          updated[idx] = { ...updated[idx], content };
        } else {
          updated = [...updated, { id, content, type: 'bot' as const }];
        }
      };

      if (kitchenEvents.length > 0) upsert(EVENT_GROUP_KITCHEN, buildKitchenContent(kitchenEvents));
      if (inventoryEvents.length > 0) upsert(EVENT_GROUP_INVENTORY, buildInventoryContent(inventoryEvents));
      if (deliveryEvents.length > 0) upsert(EVENT_GROUP_DELIVERY, buildDeliveryContent(deliveryEvents));

      // Add a final bot message when the order is delivered
      if (isDelivered && !deliveredAddedRef.current) {
        deliveredAddedRef.current = true;
        updated = [...updated, {
          id: DELIVERED_MSG_ID,
          content: '🎉 Your order has been delivered! Enjoy your pizza! 🍕',
          type: 'bot',
        }];
      }

      return updated;
    });
  }, [events]);

  const connectForOrder = useCallback(async (detectedOrderId: string) => {
    if (trackedOrderIdRef.current === detectedOrderId) return;
    trackedOrderIdRef.current = detectedOrderId;
    setOrderId(detectedOrderId);
    try {
      await connectWebSocket(detectedOrderId);
    } catch {
      // WebSocket connection failure is non-fatal
    }
  }, [setOrderId, connectWebSocket]);

  const handleSubmit = async () => {
    if (!inputValue.trim()) return;

    const userText = inputValue;
    const ts = Date.now();
    setMessages(prev => [...prev, { id: `user-${ts}`, content: userText, type: 'user' }]);
    setInputValue('');
    setInputDisabled(true);

    // Create a placeholder bot message that we will stream into
    const botMessageId = `bot-${ts}`;
    setMessages(prev => [...prev, { id: botMessageId, content: '', type: 'bot' }]);

    try {
      const response = await fetch(`${STORE_SERVICE_URL}/api/chat`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'text/event-stream',
        },
        body: JSON.stringify({
          sessionId: sessionIdRef.current,
          message: userText,
        }),
      });

      if (!response.ok) {
        setMessages(prev =>
          prev.map(m => m.id === botMessageId
            ? { ...m, content: 'Sorry, there was a problem. Please try again.' }
            : m
          )
        );
        return;
      }

      const reader = response.body?.getReader();
      if (!reader) {
        setMessages(prev =>
          prev.map(m => m.id === botMessageId
            ? { ...m, content: 'Sorry, streaming is not supported in your browser.' }
            : m
          )
        );
        return;
      }

      const decoder = new TextDecoder();
      let accumulated = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        const chunk = decoder.decode(value, { stream: true });
        // Parse SSE data lines
        const lines = chunk.split('\n');
        for (const line of lines) {
          if (line.startsWith('data:')) {
            const data = line.slice(5);
            accumulated += data;
            const currentText = accumulated;
            setMessages(prev =>
              prev.map(m => m.id === botMessageId
                ? { ...m, content: currentText }
                : m
              )
            );
          }
        }
      }

      // If we got no content, show a fallback
      if (!accumulated) {
        setMessages(prev =>
          prev.map(m => m.id === botMessageId
            ? { ...m, content: 'I didn\'t get a response. Please try again.' }
            : m
          )
        );
      } else {
        // Check if the response contains an orderId - if so, connect WebSocket to track events
        const detectedOrderId = extractOrderId(accumulated);
        if (detectedOrderId) {
          connectForOrder(detectedOrderId);
        }
      }
    } catch {
      setMessages(prev =>
        prev.map(m => m.id === botMessageId
          ? { ...m, content: 'Sorry, I couldn\'t reach the assistant. Please try again later.' }
          : m
        )
      );
    } finally {
      setInputDisabled(false);
    }
  };

  return (
    <main className={styles.page}>
      <Chat
        messages={messages}
        inputValue={inputValue}
        onInputChange={setInputValue}
        onSubmit={handleSubmit}
        inputDisabled={inputDisabled}
      />
    </main>
  );
}