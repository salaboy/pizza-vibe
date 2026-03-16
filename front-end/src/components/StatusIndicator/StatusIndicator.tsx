import styles from './StatusIndicator.module.css';

type Status = 'active' | 'inactive' | 'failed';

interface StatusIndicatorProps {
  status: Status;
  className?: string;
}

export default function StatusIndicator({ status, className }: StatusIndicatorProps) {
  return (
    <span
      className={`${styles.indicator} ${styles[status]}${className ? ` ${className}` : ''}`}
      role="status"
      aria-label={status}
    />
  );
}
