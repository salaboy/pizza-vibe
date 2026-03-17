'use client';

import Icon from '@/components/Icon';
import Button from '@/components/Button';
import StatusIndicator from '@/components/StatusIndicator';
import InventoryItem from '@/components/InventoryItem';
import OvenItem from '@/components/OvenItem';
import BikeItem from '@/components/BikeItem';
import DashboardBlock from '@/components/DashboardBlock';
import DashboardPanels from '@/components/DashboardPanels';
import AgentBlock from '@/components/AgentBlock';
import MessageTurn from '@/components/Chat/MessageTurn';

export default function ComponentsPage() {
  return (
    <main style={{ padding: '48px', display: 'flex', flexDirection: 'column', gap: '48px' }}>
      <h1>Components Showcase</h1>

      {/* Icons */}
      <section>
        <h2>Icon</h2>
        <div style={{ display: 'flex', gap: '24px', flexWrap: 'wrap', alignItems: 'center' }}>
          {(['minus', 'add', 'delete', 'send', 'check', 'order', 'kitchen', 'delivery', 'drinks', 'inventory', 'bikes'] as const).map((name) => (
            <div key={name} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px' }}>
              <div style={{ width: 40, height: 40, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <Icon name={name} />
              </div>
              <span style={{ fontSize: 12, color: '#3b4943' }}>{name}</span>
            </div>
          ))}
        </div>
      </section>
      {/* Button */}
      <section>
        <h2>Button</h2>
        <div style={{ display: 'flex', gap: '24px', flexWrap: 'wrap', alignItems: 'center' }}>
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px' }}>
            <Button>Place Order</Button>
            <span style={{ fontSize: 12, color: '#3b4943' }}>Default</span>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px' }}>
            <Button disabled>Place Order</Button>
            <span style={{ fontSize: 12, color: '#3b4943' }}>Default Disabled</span>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px' }}>
            <Button color="danger">Clear all</Button>
            <span style={{ fontSize: 12, color: '#3b4943' }}>Danger</span>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px' }}>
            <Button color="danger" disabled>Clear all</Button>
            <span style={{ fontSize: 12, color: '#3b4943' }}>Danger Disabled</span>
          </div>
        </div>
      </section>
      {/* InventoryItem */}
      <section>
        <h2>InventoryItem</h2>
        <div style={{ display: 'flex', gap: '24px', flexWrap: 'wrap', alignItems: 'center' }}>
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px' }}>
            <InventoryItem emoji="🍺" quantity={50} />
            <span style={{ fontSize: 12, color: '#3b4943' }}>Default</span>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px' }}>
            <InventoryItem emoji="🧀" quantity={12} changed />
            <span style={{ fontSize: 12, color: '#3b4943' }}>Changed</span>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px' }}>
            <InventoryItem emoji="🍕" quantity={8} />
            <span style={{ fontSize: 12, color: '#3b4943' }}>Default</span>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px' }}>
            <InventoryItem emoji="🫒" quantity={3} changed />
            <span style={{ fontSize: 12, color: '#3b4943' }}>Changed</span>
          </div>
        </div>
      </section>

      {/* OvenItem */}
      <section>
        <h2>OvenItem</h2>
        <div style={{ display: 'flex', gap: '24px', flexWrap: 'wrap', alignItems: 'center' }}>
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px' }}>
            <OvenItem number={1} status="idle" />
            <span style={{ fontSize: 12, color: '#3b4943' }}>Idle</span>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px' }}>
            <OvenItem number={2} status="cooking" />
            <span style={{ fontSize: 12, color: '#3b4943' }}>Cooking</span>
          </div>
        </div>
      </section>

      {/* BikeItem */}
      <section>
        <h2>BikeItem</h2>
        <div style={{ display: 'flex', gap: '24px', flexWrap: 'wrap', alignItems: 'center' }}>
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px' }}>
            <BikeItem number={1} status="idle" />
            <span style={{ fontSize: 12, color: '#3b4943' }}>Idle</span>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px' }}>
            <BikeItem number={2} status="delivering" />
            <span style={{ fontSize: 12, color: '#3b4943' }}>Delivering</span>
          </div>
        </div>
      </section>

      {/* DashboardBlock */}
      <section>
        <h2>DashboardBlock</h2>
        <div style={{ display: 'flex', gap: '24px', flexWrap: 'wrap', alignItems: 'flex-start' }}>
          <DashboardBlock icon="drinks" title="Drinks Stock" status="active">
            <InventoryItem emoji="🍺" quantity={50} />
            <InventoryItem emoji="🥤" quantity={30} />
            <InventoryItem emoji="🧃" quantity={15} changed />
            <InventoryItem emoji="☕" quantity={20} />
          </DashboardBlock>
          <DashboardBlock icon="kitchen" title="Ovens" status="active">
            <OvenItem number={1} status="cooking" />
            <OvenItem number={2} status="idle" />
            <OvenItem number={3} status="idle" />
          </DashboardBlock>
        </div>
      </section>

      {/* DashboardPanels */}
      <section>
        <h2>DashboardPanels</h2>
        <DashboardPanels>
          <DashboardBlock icon="drinks" title="Drinks Stock" status="active">
            <InventoryItem emoji="🍺" quantity={50} />
            <InventoryItem emoji="🥤" quantity={30} />
            <InventoryItem emoji="🧃" quantity={15} changed />
          </DashboardBlock>
          <DashboardBlock icon="inventory" title="Inventory" status="active">
            <InventoryItem emoji="🧀" quantity={12} />
            <InventoryItem emoji="🍕" quantity={8} changed />
          </DashboardBlock>
          <DashboardBlock icon="kitchen" title="Ovens" status="active">
            <OvenItem number={1} status="cooking" />
            <OvenItem number={2} status="idle" />
            <OvenItem number={3} status="idle" />
            <OvenItem number={4} status="idle" />
          </DashboardBlock>
          <DashboardBlock icon="bikes" title="Bikes" status="active">
            <BikeItem number={1} status="idle" />
            <BikeItem number={2} status="delivering" />
            <BikeItem number={3} status="idle" />
            <BikeItem number={4} status="idle" />
          </DashboardBlock>
        </DashboardPanels>
      </section>

      {/* AgentBlock */}
      <section>
        <h2>AgentBlock</h2>
        <div style={{ display: 'flex', gap: '24px', flexWrap: 'wrap', alignItems: 'flex-start' }}>
          <AgentBlock emoji="👩‍💼" title="Store Manager" status="default">
            <MessageTurn messages={['Welcome to Pizza Vibe! What kind of pizza are you in the mood for today?', 'Following message']} type="bot" size="small" />
            <MessageTurn messages={['Following message']} type="user" size="small" />
            <MessageTurn messages={['Welcome to Pizza Vibe! What kind of pizza are you in the mood for today?', 'Following message']} type="bot" size="small" />
          </AgentBlock>
          <AgentBlock emoji="👨‍🍳" title="Cooking Agent" status="talking">
            <MessageTurn messages={['Welcome to Pizza Vibe! What kind of pizza are you in the mood for today?', 'Following message']} type="bot" size="small" />
            <MessageTurn messages={['Following message']} type="user" size="small" />
            <MessageTurn messages={['Welcome to Pizza Vibe! What kind of pizza are you in the mood for today?', 'Following message']} type="bot" size="small" />
          </AgentBlock>
        </div>
      </section>

      {/* StatusIndicator */}
      <section>
        <h2>StatusIndicator</h2>
        <div style={{ display: 'flex', gap: '24px', flexWrap: 'wrap', alignItems: 'center' }}>
          {(['active', 'inactive', 'failed'] as const).map((status) => (
            <div key={status} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px' }}>
              <StatusIndicator status={status} />
              <span style={{ fontSize: 12, color: '#3b4943' }}>{status}</span>
            </div>
          ))}
        </div>
      </section>
    </main>
  );
}
