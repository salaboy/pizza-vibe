// Package store provides the store service for the Pizza Vibe application.
// It handles pizza orders, receives events from kitchen and delivery services,
// and sends real-time updates to frontend clients via WebSocket.
package store

import (
	"encoding/json"
	"fmt"
	"math"
	"time"

	"github.com/google/uuid"
)

// OrderItem represents a single item in an order, containing the pizza type
// and the quantity requested.
type OrderItem struct {
	PizzaType string `json:"pizzaType"`
	Quantity  int    `json:"quantity"`
}

// FlexTimestamp is a time.Time that can unmarshal from both RFC 3339 strings
// and numeric epoch seconds (as produced by Java's Jackson for Instant).
// It always marshals as an RFC 3339 string.
type FlexTimestamp struct {
	time.Time
}

func (ft FlexTimestamp) MarshalJSON() ([]byte, error) {
	return json.Marshal(ft.Time)
}

func (ft *FlexTimestamp) UnmarshalJSON(data []byte) error {
	// Try RFC 3339 string first
	var t time.Time
	if err := json.Unmarshal(data, &t); err == nil {
		ft.Time = t
		return nil
	}

	// Try numeric epoch seconds (e.g. 1710230400 or 1710230400.123456789)
	var f float64
	if err := json.Unmarshal(data, &f); err == nil {
		sec, frac := math.Modf(f)
		ft.Time = time.Unix(int64(sec), int64(frac*1e9))
		return nil
	}

	return fmt.Errorf("timestamp: cannot parse %s", string(data))
}

// AgentEvent represents an event emitted by an agent during order processing.
// Kind must be one of "request", "response", or "error".
type AgentEvent struct {
	AgentID   string        `json:"agentId"`
	Kind      string        `json:"kind"`
	Text      string        `json:"text"`
	Timestamp FlexTimestamp `json:"timestamp"`
}

// DrinkItem represents a single drink in an order, containing the drink type
// and the quantity requested.
type DrinkItem struct {
	DrinkType string `json:"drinkType"`
	Quantity  int    `json:"quantity"`
}

// Order represents a pizza order with a unique identifier, items, additional data,
// and current status.
type Order struct {
	OrderID     uuid.UUID   `json:"orderId"`
	OrderItems  []OrderItem `json:"orderItems"`
	DrinkItems  []DrinkItem `json:"drinkItems,omitempty"`
	OrderData   string      `json:"orderData"`
	OrderStatus string      `json:"orderStatus"`
}
