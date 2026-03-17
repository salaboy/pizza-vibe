package store

import (
	"sync"

	"github.com/google/uuid"
)

// MemoryRepository implements OrderRepository using in-memory maps.
type MemoryRepository struct {
	mu          sync.RWMutex
	orders      map[uuid.UUID]*Order
	events      map[uuid.UUID][]OrderEvent
	agentEvents []AgentEvent
}

// NewMemoryRepository creates a new in-memory repository.
func NewMemoryRepository() *MemoryRepository {
	return &MemoryRepository{
		orders: make(map[uuid.UUID]*Order),
		events: make(map[uuid.UUID][]OrderEvent),
	}
}

func (m *MemoryRepository) CreateOrder(order *Order) error {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.orders[order.OrderID] = order
	return nil
}

func (m *MemoryRepository) GetOrder(orderID uuid.UUID) (*Order, bool) {
	m.mu.RLock()
	defer m.mu.RUnlock()
	order, exists := m.orders[orderID]
	return order, exists
}

func (m *MemoryRepository) GetAllOrders() ([]*Order, error) {
	m.mu.RLock()
	defer m.mu.RUnlock()
	orders := make([]*Order, 0, len(m.orders))
	for _, order := range m.orders {
		orders = append(orders, order)
	}
	return orders, nil
}

func (m *MemoryRepository) UpdateOrderStatus(orderID uuid.UUID, status string) bool {
	m.mu.Lock()
	defer m.mu.Unlock()
	order, exists := m.orders[orderID]
	if !exists {
		return false
	}
	order.OrderStatus = status
	return true
}

func (m *MemoryRepository) TrackEvent(orderID uuid.UUID, event OrderEvent) error {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.events[orderID] = append(m.events[orderID], event)
	return nil
}

func (m *MemoryRepository) GetOrderEvents(orderID uuid.UUID) ([]OrderEvent, error) {
	m.mu.RLock()
	defer m.mu.RUnlock()
	return m.events[orderID], nil
}

func (m *MemoryRepository) TrackAgentEvent(event AgentEvent) error {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.agentEvents = append(m.agentEvents, event)
	return nil
}

func (m *MemoryRepository) GetAgentEventsByAgentID(agentID string) ([]AgentEvent, error) {
	m.mu.RLock()
	defer m.mu.RUnlock()
	var result []AgentEvent
	for _, e := range m.agentEvents {
		if e.AgentID == agentID {
			result = append(result, e)
		}
	}
	return result, nil
}

func (m *MemoryRepository) GetAllAgentEvents() ([]AgentEvent, error) {
	m.mu.RLock()
	defer m.mu.RUnlock()
	result := make([]AgentEvent, len(m.agentEvents))
	copy(result, m.agentEvents)
	return result, nil
}

func (m *MemoryRepository) DeleteAllAgentEvents() error {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.agentEvents = nil
	return nil
}
