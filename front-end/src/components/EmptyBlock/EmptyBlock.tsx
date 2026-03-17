import styles from './EmptyBlock.module.css';

interface EmptyBlockProps {
  children?: React.ReactNode;
  className?: string;
}

export default function EmptyBlock({ children, className }: EmptyBlockProps) {
  return (
    <div className={`${styles.emptyBlock}${className ? ` ${className}` : ''}`}>
      {children}
    </div>
  );
}
