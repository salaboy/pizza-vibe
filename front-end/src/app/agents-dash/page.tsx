'use client';

import { useState, useEffect, useCallback } from 'react';
import styles from './page.module.css';

interface InventoryItem {
  item: string;
  quantity: number;
}

interface DrinkItem {
  item: string;
  quantity: number;
}

interface Oven {
  id: string;
  status: string;
  user?: string;
  progress: number;
  updatedAt: string;
}

interface Bike {
  id: string;
  status: string;
  user?: string;
  orderId?: string;
  updatedAt: string;
}

interface AgentEvent {
  agentId: string;
  kind: 'request' | 'response' | 'error';
  text: string;
  timestamp: string;
}

interface AgentConfig {
  agentIds: string[];
  label: string;
  emoji: string;
}

const INVENTORY_EMOJI: Record<string, string> = {
  PizzaDough: '🫓',
  Mozzarella: '🧀',
  Sauce: '🍅',
  Pepperoni: '🥓',
  Pineapple: '🍍',
};

const DRINK_EMOJI: Record<string, string> = {
  Beer: '🍺',
  Coke: '🥤',
  Wine: '🍷',
  Water: '💧',
  Juice: '🧃',
  Coffee: '☕',
  Lemonade: '🍋',
};

const KIND_EMOJI: Record<string, string> = {
  request: '📤',
  response: '📥',
  error: '❌',
};

const AGENTS: AgentConfig[] = [
  { agentIds: ['store-mgmt-agent', 'chat-agent', 'pizza-order-workflow'], label: 'Store Manager', emoji: '🏪' },
  { agentIds: ['drinks-agent'], label: 'Drinks Agent', emoji: '🍹' },
  { agentIds: ['cooking-agent'], label: 'Cooking Agent', emoji: '👨‍🍳' },
  { agentIds: ['delivery-agent'], label: 'Delivery Agent', emoji: '🚴' },
];

function getInventoryEmoji(item: string): string {
  return INVENTORY_EMOJI[item] || '📦';
}

function getDrinkEmoji(item: string): string {
  return DRINK_EMOJI[item] || '🥤';
}

const STORE_SERVICE_URL = process.env.NEXT_PUBLIC_STORE_SERVICE_URL || '';

export default function AgentsDashPage() {
  const [inventory, setInventory] = useState<InventoryItem[]>([]);
  const [drinks, setDrinks] = useState<DrinkItem[]>([]);
  const [ovens, setOvens] = useState<Oven[]>([]);
  const [bikes, setBikes] = useState<Bike[]>([]);
  const [inventoryOk, setInventoryOk] = useState(true);
  const [drinksOk, setDrinksOk] = useState(true);
  const [ovensOk, setOvensOk] = useState(true);
  const [bikesOk, setBikesOk] = useState(true);
  const [eventsByAgent, setEventsByAgent] = useState<Record<string, AgentEvent[]>>({});

  const fetchAll = useCallback(async () => {
    const base = STORE_SERVICE_URL;

    try {
      const res = await fetch(`${base}/api/inventory`);
      if (res.ok) { setInventory(await res.json()); setInventoryOk(true); }
      else setInventoryOk(false);
    } catch { setInventoryOk(false); }

    try {
      const res = await fetch(`${base}/api/drinks-stock`);
      if (res.ok) { setDrinks(await res.json()); setDrinksOk(true); }
      else setDrinksOk(false);
    } catch { setDrinksOk(false); }

    try {
      const res = await fetch(`${base}/api/oven`);
      if (res.ok) { setOvens(await res.json()); setOvensOk(true); }
      else setOvensOk(false);
    } catch { setOvensOk(false); }

    try {
      const res = await fetch(`${base}/api/bikes`);
      if (res.ok) { setBikes(await res.json()); setBikesOk(true); }
      else setBikesOk(false);
    } catch { setBikesOk(false); }

    try {
      const res = await fetch(`${base}/api/agents-events`);
      if (!res.ok) return;
      const allEvents: AgentEvent[] = await res.json();
      const grouped: Record<string, AgentEvent[]> = {};
      for (const agent of AGENTS) {
        grouped[agent.label] = [];
      }
      for (const event of allEvents) {
        const agent = AGENTS.find((a) => a.agentIds.includes(event.agentId));
        if (agent) {
          grouped[agent.label].push(event);
        }
      }
      setEventsByAgent(grouped);
    } catch {
      // silently ignore
    }
  }, []);

  useEffect(() => {
    fetchAll();
    const interval = setInterval(fetchAll, 1000);
    return () => clearInterval(interval);
  }, [fetchAll]);

  const handleClearAll = async () => {
    const base = STORE_SERVICE_URL;
    try {
      await fetch(`${base}/api/agents-events`, { method: 'DELETE' });
      setEventsByAgent({});
    } catch {
      // silently ignore
    }
  };

  return (
    <main className={styles.page}>
      {/* Dashboard Section */}
      <h2 className={styles.sectionTitle}>Management Dashboard</h2>

      <div className={styles.panelRow}>
        {/* Drinks Stock */}
        <div className={styles.box}>
          <div className={styles.boxHeader}>
            <h3 className={styles.boxTitle}>🍹 Drinks Stock</h3>
            <div className={`${styles.statusDot} ${!drinksOk ? styles.statusDotError : ''}`} />
          </div>
          {!drinksOk && <p className={styles.errorText}>Service unavailable</p>}
          <div className={styles.chipWrap}>
            {drinks.map((item) => (
              <div key={item.item} className={styles.chip}>
                <span className={styles.chipEmoji}>{getDrinkEmoji(item.item)}</span>
                <div className={styles.chipInfo}>
                  <span className={styles.chipLabel}>{item.item}</span>
                  <span className={styles.chipQty}>{item.quantity}</span>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Inventory */}
        <div className={styles.box}>
          <div className={styles.boxHeader}>
            <h3 className={styles.boxTitle}>📦 Inventory</h3>
            <div className={`${styles.statusDot} ${!inventoryOk ? styles.statusDotError : ''}`} />
          </div>
          {!inventoryOk && <p className={styles.errorText}>Service unavailable</p>}
          <div className={styles.chipWrap}>
            {inventory.map((item) => (
              <div key={item.item} className={styles.chip}>
                <span className={styles.chipEmoji}>{getInventoryEmoji(item.item)}</span>
                <div className={styles.chipInfo}>
                  <span className={styles.chipLabel}>{item.item}</span>
                  <span className={styles.chipQty}>{item.quantity}</span>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Ovens */}
        <div className={styles.box}>
          <div className={styles.boxHeader}>
            <h3 className={styles.boxTitle}>🔥 Ovens</h3>
            <div className={`${styles.statusDot} ${!ovensOk ? styles.statusDotError : ''}`} />
          </div>
          {!ovensOk && <p className={styles.errorText}>Service unavailable</p>}
          {ovens.map((oven) => (
            <div key={oven.id} className={styles.unitRow}>
              <span className={styles.unitEmoji}>
                {oven.status === 'AVAILABLE' ? '🟢' : '🍕'}
              </span>
              <div className={styles.unitInfo}>
                <span className={styles.unitId}>{oven.id}</span>
                <span className={`${styles.unitStatus} ${oven.status === 'AVAILABLE' ? styles.available : styles.reserved}`}>
                  {oven.status === 'AVAILABLE' ? 'Available' : `Cooking${oven.user ? ` (${oven.user})` : ''}`}
                </span>
                {oven.status === 'RESERVED' && (
                  <div className={styles.progressBar}>
                    <div className={styles.progressFill} style={{ width: `${oven.progress}%` }} />
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>

        {/* Bikes */}
        <div className={styles.box}>
          <div className={styles.boxHeader}>
            <h3 className={styles.boxTitle}>🚲 Bikes</h3>
            <div className={`${styles.statusDot} ${!bikesOk ? styles.statusDotError : ''}`} />
          </div>
          {!bikesOk && <p className={styles.errorText}>Service unavailable</p>}
          {bikes.map((bike) => (
            <div key={bike.id} className={styles.unitRow}>
              <span className={styles.unitEmoji}>
                {bike.status === 'AVAILABLE' ? '🟢' : '🚴'}
              </span>
              <div className={styles.unitInfo}>
                <span className={styles.unitId}>{bike.id}</span>
                <span className={`${styles.unitStatus} ${bike.status === 'AVAILABLE' ? styles.available : styles.reserved}`}>
                  {bike.status === 'AVAILABLE' ? 'Available' : `Delivering${bike.user ? ` (${bike.user})` : ''}`}
                </span>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Agents Section */}
      <div className={styles.titleRow}>
        <h2 className={styles.sectionTitle}>🤖 Agents</h2>
        <button className={styles.clearButton} onClick={handleClearAll}>
          🗑️ Clear All
        </button>
      </div>

      <div className={styles.panelRow}>
        {AGENTS.map((agent) => {
          const events = eventsByAgent[agent.label] || [];
          return (
            <div key={agent.label} className={styles.box}>
              <div className={styles.boxHeader}>
                <h3 className={styles.boxTitle}>
                  {agent.emoji} {agent.label}
                </h3>
              </div>

              {events.length === 0 ? (
                <p className={styles.emptyText}>No events yet... 🦗</p>
              ) : (
                <div className={styles.eventList}>
                  {[...events].reverse().map((event, idx) => (
                    <div key={idx} className={styles.eventRow}>
                      <span className={styles.eventKind}>
                        {KIND_EMOJI[event.kind] || '📋'}
                      </span>
                      <div className={styles.eventBody}>
                        <span className={styles.eventText}>{event.text}</span>
                        <span className={styles.eventMeta}>
                          {event.kind} · {new Date(event.timestamp).toLocaleTimeString()}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          );
        })}
      </div>
    </main>
  );
}
