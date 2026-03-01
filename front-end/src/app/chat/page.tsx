'use client';

import { useState } from 'react';
import Chat, { ChatMessage } from '@/components/Chat';
import styles from './page.module.css';

export default function ChatPage() {
  const [messages, setMessages] = useState<ChatMessage[]>([
    { id: '1', content: 'Welcome to Pizza Vibe! What kind of pizza are you in the mood for today?', type: 'bot' },
  ]);
  const [inputValue, setInputValue] = useState('');

  const handleSubmit = () => {
    if (!inputValue.trim()) return;
    setMessages(prev => [...prev, { id: String(Date.now()), content: inputValue, type: 'user' }]);
    setInputValue('');
  };

  return (
    <main className={styles.page}>
      <div className={styles.chatWrapper}>
        <Chat
          messages={messages}
          inputValue={inputValue}
          onInputChange={setInputValue}
          onSubmit={handleSubmit}
        />
      </div>
      <div className={styles.rightPanel}>
        <div className={styles.rightHeader}>
          <h2>Pizza Vibe Status</h2>
        </div>
        <div className={styles.statusSection}>
          <div className={styles.ordersColumn}>
            <h3>Orders</h3>
          </div>
          <div className={styles.rightColumn}>
            <div className={styles.kitchenBlock}>
              <h3>Kitchen</h3>
            </div>
            <div className={styles.deliveryBlock}>
              <h3>Delivery</h3>
            </div>
          </div>
        </div>
      </div>
    </main>
  );
}
