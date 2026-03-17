import Message from '../Message';
import styles from './MessageTurn.module.css';

interface MessageTurnProps {
  messages: string[];
  type?: 'bot' | 'user' | 'event';
  size?: 'default' | 'small';
}

export default function MessageTurn({ messages, type = 'bot', size = 'default' }: MessageTurnProps) {
  const className = type === 'user'
    ? styles.user
    : type === 'event'
      ? styles.event
      : styles.bot;

  const sizeClass = size === 'small' ? styles.small : '';

  return (
    <div className={`${styles.turn} ${className} ${sizeClass}`}>
      {messages.map((message, index) => (
        <Message key={index} message={message} type={type} size={size} />
      ))}
    </div>
  );
}
