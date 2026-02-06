// Package kitchen provides the kitchen service for the Pizza Vibe application.
// It handles cooking pizza orders by processing order items with simulated cooking times.
package kitchen

import "github.com/google/uuid"

// OrderItem represents a single item in an order, containing the pizza type
// and the quantity requested.
type OrderItem struct {
	PizzaType string `json:"pizzaType"`
	Quantity  int    `json:"quantity"`
}

// CookRequest represents the request body for cooking an order.
// It contains the order ID and the items to be cooked.
type CookRequest struct {
	OrderID    uuid.UUID   `json:"orderId"`
	OrderItems []OrderItem `json:"orderItems"`
}

// CookResponse represents the response returned after accepting a cook request.
type CookResponse struct {
	OrderID uuid.UUID `json:"orderId"`
	Status  string    `json:"status"`
	Message string    `json:"message,omitempty"`
}

// CookedItem represents a single item that has been cooked, including the time
// it took to cook.
type CookedItem struct {
	PizzaType   string `json:"pizzaType"`
	Quantity    int    `json:"quantity"`
	CookingTime int    `json:"cookingTime"` // in seconds
}

// AgentCookRequest represents the request body for the cooking-agent.
// It contains a list of pizza names to be cooked.
type AgentCookRequest struct {
	Pizzas []string `json:"pizzas"`
}

// CookingUpdate represents a streaming update from the cooking agent.
// These updates inform the client about the current action being performed.
type CookingUpdate struct {
	Type      string `json:"type"`      // Type of update: "action", "progress", "result", "partial"
	Action    string `json:"action"`    // The action being performed
	Message   string `json:"message"`   // Human-readable message describing the update
	ToolName  string `json:"toolName"`  // The name of the tool being executed (if applicable)
	ToolInput string `json:"toolInput"` // The input to the tool (if applicable)
}
