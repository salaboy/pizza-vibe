import Link from 'next/link';
import styles from './HeaderNavItem.module.css';

interface HeaderNavItemProps {
  href: string;
  label: string;
  isActive?: boolean;
}

export default function HeaderNavItem({ href, label, isActive = false }: HeaderNavItemProps) {
  return (
    <Link
      href={href}
      className={`${styles.navItem} ${isActive ? styles.active : styles.default}`}
    >
      <span>{label}</span>
    </Link>
  );
}
