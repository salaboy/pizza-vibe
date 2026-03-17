'use client';

import { usePathname } from 'next/navigation';
import Logo from '@/components/Logo';
import HeaderNavItem from './HeaderNavItem';
import styles from './Header.module.css';

const navItems = [
  { href: '/', label: 'Store' },
  { href: '/kitchen', label: 'Kitchen' },
  { href: '/delivery', label: 'Delivery' },
  { href: '/management', label: 'Management' },
];

export default function Header() {
  const pathname = usePathname();

  return (
    <header className={styles.header}>
      <Logo />
      <nav className={styles.navigation}>
        {navItems.map((item) => (
          <HeaderNavItem
            key={item.href}
            href={item.href}
            label={item.label}
            isActive={pathname === item.href}
          />
        ))}
      </nav>
    </header>
  );
}
