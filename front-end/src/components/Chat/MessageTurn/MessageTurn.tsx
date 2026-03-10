import Message from '../Message';
import styles from './MessageTurn.module.css';

interface MessageTurnProps {
  messages: string[];
  type?: 'bot' | 'user' | 'event';
}

export default function MessageTurn({ messages, type = 'bot' }: MessageTurnProps) {
  const className = type === 'user'
    ? styles.user
    : type === 'event'
      ? styles.event
      : styles.bot;

  return (
    <div className={`${styles.turn} ${className}`}>
      {messages.map((message, index) => (
        <Message key={index} message={message} type={type} />
      ))}
    </div>
  );
}