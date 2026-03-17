'use client';

import { useState, useEffect, useCallback } from 'react';
import styles from './page.module.css';

interface InventoryItem {
  item: string;
  quantity: number;
}

interface DrinkItem {
  item: string;
  quantity: number;
}

interface Oven {
  id: string;
  status: string;
  user?: string;
  progress: number;
  updatedAt: string;
}

interface Bike {
  id: string;
  status: string;
  user?: string;
  orderId?: string;
  updatedAt: string;
}

const INVENTORY_EMOJI: Record<string, string> = {
  PizzaDough: '🫓',
  Mozzarella: '🧀',
  Sauce: '🍅',
  Pepperoni: '🥓',
  Pineapple: '🍍',
};

const DRINK_EMOJI: Record<string, string> = {
  Beer: '🍺',
  Coke: '🥤',
  Wine: '🍷',
  Water: '💧',
  Juice: '🧃',
  Coffee: '☕',
  Lemonade: '🍋',
};

function getInventoryEmoji(item: string): string {
  return INVENTORY_EMOJI[item] || '📦';
}

function getDrinkEmoji(item: string): string {
  return DRINK_EMOJI[item] || '🥤';
}

const STORE_SERVICE_URL = process.env.NEXT_PUBLIC_STORE_SERVICE_URL || '';

export default function MgmtPage() {
  const [inventory, setInventory] = useState<InventoryItem[]>([]);
  const [drinks, setDrinks] = useState<DrinkItem[]>([]);
  const [ovens, setOvens] = useState<Oven[]>([]);
  const [bikes, setBikes] = useState<Bike[]>([]);

  const [inventoryOk, setInventoryOk] = useState(true);
  const [drinksOk, setDrinksOk] = useState(true);
  const [ovensOk, setOvensOk] = useState(true);
  const [bikesOk, setBikesOk] = useState(true);

  const fetchAll = useCallback(async () => {
    const base = STORE_SERVICE_URL;

    try {
      const res = await fetch(`${base}/api/inventory`);
      if (res.ok) { setInventory(await res.json()); setInventoryOk(true); }
      else setInventoryOk(false);
    } catch { setInventoryOk(false); }

    try {
      const res = await fetch(`${base}/api/drinks-stock`);
      if (res.ok) { setDrinks(await res.json()); setDrinksOk(true); }
      else setDrinksOk(false);
    } catch { setDrinksOk(false); }

    try {
      const res = await fetch(`${base}/api/oven`);
      if (res.ok) { setOvens(await res.json()); setOvensOk(true); }
      else setOvensOk(false);
    } catch { setOvensOk(false); }

    try {
      const res = await fetch(`${base}/api/bikes`);
      if (res.ok) { setBikes(await res.json()); setBikesOk(true); }
      else setBikesOk(false);
    } catch { setBikesOk(false); }
  }, []);

  useEffect(() => {
    fetchAll();
    const interval = setInterval(fetchAll, 1000);
    return () => clearInterval(interval);
  }, [fetchAll]);

  return (
    <main className={styles.page}>
      <h2 className={styles.title}>Management Dashboard</h2>

      <div className={styles.grid}>
        {/* Inventory */}
        <div className={styles.box}>
          <div className={styles.boxHeader}>
            <h3 className={styles.boxTitle}>📦 Inventory</h3>
            <div className={`${styles.statusDot} ${!inventoryOk ? styles.statusDotError : ''}`} />
          </div>
          {!inventoryOk && <p className={styles.errorText}>Service unavailable</p>}
          <div className={styles.items}>
            {inventory.map((item) => (
              <div key={item.item} className={styles.item}>
                <span className={styles.itemEmoji}>{getInventoryEmoji(item.item)}</span>
                <div>
                  <div className={styles.itemLabel}>{item.item}</div>
                  <div className={styles.itemQty}>{item.quantity}</div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Drinks Stock */}
        <div className={styles.box}>
          <div className={styles.boxHeader}>
            <h3 className={styles.boxTitle}>🍹 Drinks Stock</h3>
            <div className={`${styles.statusDot} ${!drinksOk ? styles.statusDotError : ''}`} />
          </div>
          {!drinksOk && <p className={styles.errorText}>Service unavailable</p>}
          <div className={styles.items}>
            {drinks.map((item) => (
              <div key={item.item} className={styles.item}>
                <span className={styles.itemEmoji}>{getDrinkEmoji(item.item)}</span>
                <div>
                  <div className={styles.itemLabel}>{item.item}</div>
                  <div className={styles.itemQty}>{item.quantity}</div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Ovens */}
        <div className={styles.box}>
          <div className={styles.boxHeader}>
            <h3 className={styles.boxTitle}>🔥 Ovens</h3>
            <div className={`${styles.statusDot} ${!ovensOk ? styles.statusDotError : ''}`} />
          </div>
          {!ovensOk && <p className={styles.errorText}>Service unavailable</p>}
          {ovens.map((oven) => (
            <div key={oven.id} className={styles.unitRow}>
              <span className={styles.unitEmoji}>
                {oven.status === 'AVAILABLE' ? '🟢' : '🍕'}
              </span>
              <div className={styles.unitInfo}>
                <span className={styles.unitId}>{oven.id}</span>
                <span className={`${styles.unitStatus} ${oven.status === 'AVAILABLE' ? styles.available : styles.reserved}`}>
                  {oven.status === 'AVAILABLE' ? 'Available' : `Cooking${oven.user ? ` (${oven.user})` : ''}`}
                </span>
                {oven.status === 'RESERVED' && (
                  <div className={styles.progressBar}>
                    <div className={styles.progressFill} style={{ width: `${oven.progress}%` }} />
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>

        {/* Bikes */}
        <div className={styles.box}>
          <div className={styles.boxHeader}>
            <h3 className={styles.boxTitle}>🚲 Bikes</h3>
            <div className={`${styles.statusDot} ${!bikesOk ? styles.statusDotError : ''}`} />
          </div>
          {!bikesOk && <p className={styles.errorText}>Service unavailable</p>}
          {bikes.map((bike) => (
            <div key={bike.id} className={styles.unitRow}>
              <span className={styles.unitEmoji}>
                {bike.status === 'AVAILABLE' ? '🟢' : '🚴'}
              </span>
              <div className={styles.unitInfo}>
                <span className={styles.unitId}>{bike.id}</span>
                <span className={`${styles.unitStatus} ${bike.status === 'AVAILABLE' ? styles.available : styles.reserved}`}>
                  {bike.status === 'AVAILABLE' ? 'Available' : `Delivering${bike.user ? ` (${bike.user})` : ''}`}
                </span>
              </div>
            </div>
          ))}
        </div>
      </div>
    </main>
  );
}
