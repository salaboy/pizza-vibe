import ReactMarkdown from 'react-markdown';
import styles from './Message.module.css';

interface MessageProps {
  message: string;
  type?: 'bot' | 'user' | 'event';
}

// Check if a message is a progress update (Kitchen cooking or Delivery in transit)
function isProgressMessage(text: string): boolean {
  if (text.includes('✅') || text.includes('❌')) return false;
  if (text.includes('Cleaning up the oven')) return false;
  return (text.startsWith('🍕 Kitchen:') || text.startsWith('🚲 Delivery:')) &&
    (text.includes('...') || text.includes('%'));
}

export default function Message({ message, type = 'bot' }: MessageProps) {
  const className = type === 'user'
    ? styles.user
    : type === 'event'
      ? styles.event
      : styles.bot;

  const progress = isProgressMessage(message);

  if (type === 'bot' || type === 'event') {
    return (
      <div className={`${styles.message} ${className} ${progress ? styles.processing : ''}`}>
        <div className={styles.text}>
          <ReactMarkdown
            components={{
              table: ({ children }) => <table className={styles.table}>{children}</table>,
              th: ({ children }) => <th className={styles.th}>{children}</th>,
              td: ({ children }) => <td className={styles.td}>{children}</td>,
              p: ({ children }) => <p className={styles.paragraph}>{children}</p>,
              ul: ({ children }) => <ul className={styles.list}>{children}</ul>,
              ol: ({ children }) => <ol className={styles.list}>{children}</ol>,
              li: ({ children }) => <li className={styles.listItem}>{children}</li>,
              strong: ({ children }) => <strong className={styles.bold}>{children}</strong>,
            }}
          >
            {message}
          </ReactMarkdown>
        </div>
      </div>
    );
  }

  return (
    <div className={`${styles.message} ${className} ${progress ? styles.processing : ''}`}>
      <p className={styles.text}>{message}</p>
    </div>
  );
}
