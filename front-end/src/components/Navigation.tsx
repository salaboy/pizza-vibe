'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';

export default function Navigation() {
  const pathname = usePathname();

  const navItems = [
    { href: '/', label: 'Order' },
    { href: '/kitchen', label: 'Kitchen' },
    { href: '/delivery', label: 'Delivery' },
    { href: '/inventory', label: 'Inventory' },
    { href: '/oven', label: 'Ovens' },
    { href: '/bikes', label: 'Bikes' },
    { href: '/management', label: 'Management' },
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
