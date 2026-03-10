'use client';

import Icon from '@/components/Icon';
import styles from './ChatInput.module.css';

interface ChatInputProps {
  value: string;
  onChange: (value: string) => void;
  onSubmit: () => void;
  placeholder?: string;
  disabled?: boolean;
}

export default function ChatInput({
  value,
  onChange,
  onSubmit,
  placeholder = 'Write a message',
  disabled = false,
}: ChatInputProps) {
  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' && !e.shiftKey && value.trim()) {
      onSubmit();
    }
  };

  return (
    <div className={`${styles.wrapper}${disabled ? ` ${styles.disabled}` : ''}`}>
      <input
        className={styles.input}
        type="text"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        onKeyDown={handleKeyDown}
        placeholder={placeholder}
        disabled={disabled}
      />
      <button
        className={styles.sendButton}
        onClick={onSubmit}
        disabled={disabled || !value.trim()}
        aria-label="Send message"
      >
        <Icon name="send" />
      </button>
    </div>
  );
}
