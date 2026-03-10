'use client';

import { useRef, useEffect } from 'react';
import Logo from '@/components/Logo';
import ChatInput from './ChatInput';
import Message from './Message';
import styles from './Chat.module.css';

export interface ChatMessage {
  id: string;
  content: string;
  type: 'bot' | 'user' | 'event';
}

interface ChatProps {
  messages?: ChatMessage[];
  inputValue?: string;
  onInputChange?: (value: string) => void;
  onSubmit?: () => void;
  inputDisabled?: boolean;
}

export default function Chat({
  messages = [],
  inputValue = '',
  onInputChange,
  onSubmit,
  inputDisabled = false,
}: ChatProps) {
  const contentRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (contentRef.current) {
      contentRef.current.scrollTop = contentRef.current.scrollHeight;
    }
  }, [messages]);

  return (
    <div className={styles.chat}>
      <div className={styles.header}>
        <Logo type="isotype" />
        <div className={styles.headerInfo}>
          <p className={styles.title}>Pizza Vibe Assistant</p>
          <p className={styles.subtitle}>Lets get you a pizza</p>
        </div>
      </div>
      <div className={styles.content} ref={contentRef}>
        <div className={styles.spacer} />
        {messages.map((msg) => (
          <Message key={msg.id} message={msg.content} type={msg.type} />
        ))}
      </div>
      <div className={styles.inputArea}>
        <ChatInput
          value={inputValue}
          onChange={onInputChange ?? (() => {})}
          onSubmit={onSubmit ?? (() => {})}
          disabled={inputDisabled}
        />
      </div>
    </div>
  );
}
