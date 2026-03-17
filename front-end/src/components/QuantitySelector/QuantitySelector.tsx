'use client';

import styles from './QuantitySelector.module.css';
import SelectorButton from './SelectorButton';

interface QuantitySelectorProps {
  value: number;
  min?: number;
  max?: number;
  deleteAtMin?: boolean;
  onIncrement?: () => void;
  onDecrement?: () => void;
  onDelete?: () => void;
  className?: string;
}

export default function QuantitySelector({
  value,
  min = 0,
  max,
  deleteAtMin = false,
  onIncrement,
  onDecrement,
  onDelete,
  className,
}: QuantitySelectorProps) {
  const isAtMin = value <= min;
  const isAtMax = max !== undefined && value >= max;
  const showDelete = deleteAtMin && value <= min;

  const handleDecrement = () => {
    if (showDelete) {
      onDelete?.();
    } else {
      onDecrement?.();
    }
  };

  return (
    <div className={`${styles.quantitySelector}${className ? ` ${className}` : ''}`}>
      <SelectorButton
        icon={showDelete ? 'delete' : 'minus'}
        type={showDelete ? 'delete' : 'default'}
        onClick={handleDecrement}
        disabled={!showDelete && isAtMin}
        aria-label={showDelete ? 'Delete item' : 'Decrease quantity'}
      />
      <span className={styles.quantity}>{value}</span>
      <SelectorButton
        icon="add"
        type="default"
        onClick={onIncrement}
        disabled={isAtMax}
        aria-label="Increase quantity"
      />
    </div>
  );
}
