import styles from './TabItem.module.css';

interface TabItemProps {
  children: React.ReactNode;
  active?: boolean;
  disabled?: boolean;
  onClick?: () => void;
  className?: string;
}

export default function TabItem({ children, active = false, disabled = false, onClick, className }: TabItemProps) {
  return (
    <button
      className={`${styles.tabItem}${active ? ` ${styles.active}` : ''}${disabled ? ` ${styles.disabled}` : ''}${className ? ` ${className}` : ''}`}
      onClick={disabled ? undefined : onClick}
      role="tab"
      aria-selected={active}
      aria-disabled={disabled}
      disabled={disabled}
    >
      {children}
    </button>
  );
}
