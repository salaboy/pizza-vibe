'use client';

import { useState } from 'react';
import styles from './Tabs.module.css';
import TabItem from './TabItem';

interface Tab {
  label: string;
  value: string;
  disabled?: boolean;
}

interface TabsProps {
  tabs: Tab[];
  defaultValue?: string;
  onTabChange?: (value: string) => void;
  className?: string;
}

export default function Tabs({ tabs, defaultValue, onTabChange, className }: TabsProps) {
  const [activeTab, setActiveTab] = useState(defaultValue ?? tabs[0]?.value);

  const handleTabClick = (tab: Tab) => {
    if (tab.disabled) return;
    setActiveTab(tab.value);
    onTabChange?.(tab.value);
  };

  return (
    <div
      className={`${styles.tabs}${className ? ` ${className}` : ''}`}
      role="tablist"
    >
      <div className={styles.buttonGroup}>
        <div className={styles.buttonWrapper}>
          {tabs.map((tab) => (
            <TabItem
              key={tab.value}
              active={activeTab === tab.value}
              disabled={tab.disabled}
              onClick={() => handleTabClick(tab)}
            >
              {tab.label}
            </TabItem>
          ))}
        </div>
      </div>
    </div>
  );
}
