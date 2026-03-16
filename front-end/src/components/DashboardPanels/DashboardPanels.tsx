import styles from './DashboardPanels.module.css';

interface DashboardPanelsProps {
  children: React.ReactNode;
  className?: string;
}

export default function DashboardPanels({ children, className }: DashboardPanelsProps) {
  return (
    <div className={`${styles.panels}${className ? ` ${className}` : ''}`}>
      {children}
    </div>
  );
}
