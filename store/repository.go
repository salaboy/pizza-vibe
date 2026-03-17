package store

import "github.com/google/uuid"

// OrderRepository defines the interface for order persistence.
type OrderRepository interface {
	CreateOrder(order *Order) error
	GetOrder(orderID uuid.UUID) (*Order, bool)
	GetAllOrders() ([]*Order, error)
	UpdateOrderStatus(orderID uuid.UUID, status string) bool
	TrackEvent(orderID uuid.UUID, event OrderEvent) error
	GetOrderEvents(orderID uuid.UUID) ([]OrderEvent, error)
	TrackAgentEvent(event AgentEvent) error
	GetAgentEventsByAgentID(agentID string) ([]AgentEvent, error)
	GetAllAgentEvents() ([]AgentEvent, error)
	DeleteAllAgentEvents() error
}
