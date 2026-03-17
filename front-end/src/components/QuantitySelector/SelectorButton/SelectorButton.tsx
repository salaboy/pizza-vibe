import Icon from '@/components/Icon';
import type { IconName } from '@/components/Icon';
import styles from './SelectorButton.module.css';

interface SelectorButtonProps {
  icon: IconName;
  type?: 'default' | 'delete';
  disabled?: boolean;
  onClick?: () => void;
  className?: string;
  'aria-label'?: string;
}

export default function SelectorButton({
  icon,
  type = 'default',
  disabled = false,
  onClick,
  className,
  'aria-label': ariaLabel,
}: SelectorButtonProps) {
  return (
    <button
      className={`${styles.selectorButton} ${styles[type]}${className ? ` ${className}` : ''}`}
      disabled={disabled}
      onClick={onClick}
      aria-label={ariaLabel}
    >
      <Icon name={icon} />
    </button>
  );
}
