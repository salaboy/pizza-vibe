import styles from './Button.module.css';

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  children: React.ReactNode;
  color?: 'default' | 'danger';
  className?: string;
}

export default function Button({ children, color = 'default', className, ...props }: ButtonProps) {
  const colorClass = color === 'danger' ? styles.danger : '';

  return (
    <button
      className={`${styles.button}${colorClass ? ` ${colorClass}` : ''}${className ? ` ${className}` : ''}`}
      {...props}
    >
     <span> {children}</span>
    </button>
  );
}
