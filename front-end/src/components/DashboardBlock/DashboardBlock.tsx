import Icon, { IconName } from '@/components/Icon';
import StatusIndicator from '@/components/StatusIndicator';
import styles from './DashboardBlock.module.css';

interface DashboardBlockProps {
  icon: IconName;
  title: string;
  status: 'active' | 'inactive' | 'failed';
  children: React.ReactNode;
  className?: string;
}

export default function DashboardBlock({ icon, title, status, children, className }: DashboardBlockProps) {
  return (
    <div className={`${styles.block}${className ? ` ${className}` : ''}`}>
      <div className={styles.header}>
        <Icon name={icon} />
        <span className={styles.title}>{title}</span>
        <StatusIndicator status={status} />
      </div>
      <div className={styles.items}>
        {children}
      </div>
    </div>
  );
}
