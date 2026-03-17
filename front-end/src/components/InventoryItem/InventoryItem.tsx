import styles from './InventoryItem.module.css';

interface InventoryItemProps {
  emoji: string;
  quantity: number;
  changed?: boolean;
  className?: string;
}

export default function InventoryItem({ emoji, quantity, changed = false, className }: InventoryItemProps) {
  return (
    <div className={`${styles.item}${changed ? ` ${styles.changed}` : ''}${className ? ` ${className}` : ''}`}>
      <span className={styles.emoji}>{emoji}</span>
      <span className={styles.quantity}>{quantity}</span>
    </div>
  );
}
