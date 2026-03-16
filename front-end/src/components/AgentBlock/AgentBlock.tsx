import styles from './AgentBlock.module.css';

interface AgentBlockProps {
  emoji: string;
  title: string;
  status: 'default' | 'talking' | 'changed';
  children?: React.ReactNode;
  className?: string;
}

export default function AgentBlock({ emoji, title, status, children, className }: AgentBlockProps) {
  return (
    <div className={`${styles.block} ${styles[status]}${className ? ` ${className}` : ''}`}>
      <div className={styles.header}>
        <span className={styles.avatar}>{emoji}</span>
        <span className={styles.title}>{title}</span>
      </div>
      <div className={styles.chat}>
        {children}
      </div>
    </div>
  );
}
