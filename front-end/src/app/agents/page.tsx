'use client';

import { useState, useEffect, useCallback } from 'react';
import styles from './page.module.css';

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

const AGENTS: AgentConfig[] = [
  { agentIds: ['store-mgmt-agent', 'chat-agent', 'pizza-order-workflow'], label: 'Store Manager', emoji: '🏪' },
  { agentIds: ['drinks-agent'], label: 'Drinks Agent', emoji: '🍹' },
  { agentIds: ['delivery-agent'], label: 'Delivery Agent', emoji: '🚴' },
  { agentIds: ['cooking-agent'], label: 'Cooking Agent', emoji: '👨‍🍳' },
];

const KIND_EMOJI: Record<string, string> = {
  request: '📤',
  response: '📥',
  error: '❌',
};

const STORE_SERVICE_URL = process.env.NEXT_PUBLIC_STORE_SERVICE_URL || '';

export default function AgentsPage() {
  const [eventsByAgent, setEventsByAgent] = useState<Record<string, AgentEvent[]>>({});

  const fetchEvents = useCallback(async () => {
    const base = STORE_SERVICE_URL;
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
      // silently ignore fetch errors
    }
  }, []);

  useEffect(() => {
    fetchEvents();
    const interval = setInterval(fetchEvents, 2000);
    return () => clearInterval(interval);
  }, [fetchEvents]);

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
      <div className={styles.titleRow}>
        <h2 className={styles.title}>🤖 Agents</h2>
        <button className={styles.clearButton} onClick={handleClearAll}>
          🗑️ Clear All
        </button>
      </div>

      <div className={styles.grid}>
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
