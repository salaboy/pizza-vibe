import Icon from '@/components/Icon';
import styles from './BikeItem.module.css';

interface BikeItemProps {
  number: number;
  status: 'idle' | 'delivering';
  className?: string;
}

export default function BikeItem({ number, status, className }: BikeItemProps) {
  return (
    <div className={`${styles.item} ${styles[status]}${className ? ` ${className}` : ''}`}>
      <span className={styles.badge}><span>{number}</span></span>
      <span className={styles.icon}>
        <Icon name="bikes" />
      </span>
      <span className={styles.label}>{status === 'delivering' ? 'Delivering' : 'Available'}</span>
    </div>
  );
}
