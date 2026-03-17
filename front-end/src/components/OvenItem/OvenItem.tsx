import Icon from '@/components/Icon';
import styles from './OvenItem.module.css';

interface OvenItemProps {
  number: number;
  status: 'idle' | 'cooking';
  className?: string;
}

export default function OvenItem({ number, status, className }: OvenItemProps) {
  return (
    <div className={`${styles.item} ${styles[status]}${className ? ` ${className}` : ''}`}>
      <span className={styles.badge}><span>{number}</span></span>
      <span className={styles.icon}>
        <Icon name="kitchen" />
      </span>
      <span className={styles.label}>{status === 'cooking' ? 'Cooking' : 'Available'}</span>
    </div>
  );
}
