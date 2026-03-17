'use client';

import { useState } from 'react';
import styles from './CartItem.module.css';
import QuantitySelector from '@/components/QuantitySelector';

interface CartItemProps {
  name: string;
  unitPrice: number;
  quantity: number;
  onQuantityChange?: (quantity: number) => void;
  onDelete?: () => void;
  className?: string;
}

export default function CartItem({
  name,
  unitPrice,
  quantity,
  onQuantityChange,
  onDelete,
  className,
}: CartItemProps) {
  const [qty, setQty] = useState(quantity);

  const handleIncrement = () => {
    const newQty = qty + 1;
    setQty(newQty);
    onQuantityChange?.(newQty);
  };

  const handleDecrement = () => {
    const newQty = qty - 1;
    setQty(newQty);
    onQuantityChange?.(newQty);
  };

  const handleDelete = () => {
    setQty(0);
    onDelete?.();
  };

  return (
    <div className={`${styles.cartItem}${className ? ` ${className}` : ''}`}>
      <span className={styles.name}>{name}</span>
      <span className={styles.price}>${unitPrice * qty}</span>
      <QuantitySelector
        value={qty}
        min={1}
        deleteAtMin
        onIncrement={handleIncrement}
        onDecrement={handleDecrement}
        onDelete={handleDelete}
        className={styles.quantitySelector}
      />
    </div>
  );
}
