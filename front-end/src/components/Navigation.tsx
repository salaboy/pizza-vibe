'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';

export default function Navigation() {
  const pathname = usePathname();

  const navItems = [
    { href: '/', label: 'Order' },
    { href: '/order', label: 'Order (New UI)' },
    { href: '/inventory', label: 'Inventory' },
    { href: '/drinks-stock', label: 'Drinks Stock' },
    { href: '/oven', label: 'Ovens' },
    { href: '/bikes', label: 'Bikes' },
    { href: '/management', label: 'Management' },
    { href: '/chat', label: 'Orders (Chat)' },
  ];

  return (
    <nav>
      <div>Pizza Vibe</div>
      <ul>
        {navItems.map((item) => (
          <li key={item.href}>
            <Link href={item.href}>
              {item.label}
            </Link>
          </li>
        ))}
      </ul>
    </nav>
  );
}
