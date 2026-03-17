import styles from './PizzaItem.module.css';
import Button from '@/components/Button';

interface PizzaItemProps {
  name: string;
  price: number;
  description: string;
  image: string;
  onAdd?: () => void;
  className?: string;
}

export default function PizzaItem({
  name,
  price,
  description,
  image,
  onAdd,
  className,
}: PizzaItemProps) {
  return (
    <div
      className={`${styles.pizzaItem}${className ? ` ${className}` : ''}`}
      onClick={onAdd}
    >
      <div className={styles.illustrationContainer}>
        <img
          src={image}
          alt={name}
          className={styles.illustration}
        />
        <div className={styles.addButtonContainer}>
          <div className={styles.addButtonOuter}>
            <Button onClick={(e) => { e.stopPropagation(); onAdd?.(); }}>
              Add
            </Button>
          </div>
        </div>
      </div>
      <div className={styles.infoContainer}>
        <div className={styles.header}>
          <span className={styles.name}>{name}</span>
          <span className={styles.price}>${price}</span>
        </div>
        <p className={styles.description}>{description}</p>
      </div>
    </div>
  );
}
