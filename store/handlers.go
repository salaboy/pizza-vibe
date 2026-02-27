package store

import (
	"bytes"
	"context"
	"encoding/json"
	"log/slog"
	"net/http"

	"github.com/google/uuid"
)

// CreateOrderRequest represents the request body for creating a new order.
type CreateOrderRequest struct {
	OrderItems []OrderItem `json:"orderItems"`
	DrinkItems []DrinkItem `json:"drinkItems,omitempty"`
	OrderData  string      `json:"orderData"`
}

// ProcessOrderRequest represents the request sent to the store management agent.
type ProcessOrderRequest struct {
	OrderID    uuid.UUID   `json:"orderId"`
	OrderItems []OrderItem `json:"orderItems"`
	DrinkItems []DrinkItem `json:"drinkItems"`
}

// Store manages pizza orders and provides HTTP handlers for the store service.
type Store struct {
	repo       OrderRepository
	hub        *WebSocketHub
	agentURL   string
	httpClient *http.Client
}

// NewStore creates a new Store instance with in-memory storage and WebSocket hub.
func NewStore() *Store {
	return &Store{
		repo:       NewMemoryRepository(),
		hub:        NewWebSocketHub(),
		agentURL:   "http://store-mgmt-agent:9999",
		httpClient: &http.Client{},
	}
}

// NewStoreWithRepo creates a new Store instance with the given repository.
func NewStoreWithRepo(repo OrderRepository) *Store {
	return &Store{
		repo:       repo,
		hub:        NewWebSocketHub(),
		agentURL:   "http://store-mgmt-agent:9999",
		httpClient: &http.Client{},
	}
}

// SetAgentURL sets the URL of the store management agent.
func (s *Store) SetAgentURL(url string) {
	s.agentURL = url
}

// HandleCreateOrder handles POST /order requests to create new pizza orders.
// It validates the request, generates a UUID for the order, and stores it.
func (s *Store) HandleCreateOrder(w http.ResponseWriter, r *http.Request) {
	var req CreateOrderRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "Invalid JSON", http.StatusBadRequest)
		return
	}

	// Validate that at least one item is provided
	if len(req.OrderItems) == 0 && len(req.DrinkItems) == 0 {
		http.Error(w, "Order must contain at least one item", http.StatusBadRequest)
		return
	}

	// Create new order with generated UUID
	order := &Order{
		OrderID:     uuid.New(),
		OrderItems:  req.OrderItems,
		DrinkItems:  req.DrinkItems,
		OrderData:   req.OrderData,
		OrderStatus: "pending",
	}

	// Store the order
	if err := s.repo.CreateOrder(order); err != nil {
		slog.Error("failed to create order", "error", err)
		http.Error(w, "Failed to create order", http.StatusInternalServerError)
		return
	}

	slog.Info("order created", "orderId", order.OrderID, "items", len(order.OrderItems))

	// Send order to the store management agent (background; detach from request context)
	go s.callStoreMgmtAgent(context.Background(), order)

	// Return the created order
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(order)
}

// callStoreMgmtAgent sends a process order request to the store management agent.
func (s *Store) callStoreMgmtAgent(ctx context.Context, order *Order) {
	processReq := ProcessOrderRequest{
		OrderID:    order.OrderID,
		OrderItems: order.OrderItems,
		DrinkItems: order.DrinkItems,
	}

	body, err := json.Marshal(processReq)
	if err != nil {
		slog.Error("failed to marshal process order request", "orderId", order.OrderID, "error", err)
		return
	}

	req, err := http.NewRequestWithContext(ctx, http.MethodPost, s.agentURL+"/mgmt/processOrder", bytes.NewReader(body))
	if err != nil {
		slog.Error("failed to create agent request", "orderId", order.OrderID, "error", err)
		return
	}
	req.Header.Set("Content-Type", "application/json")

	resp, err := s.httpClient.Do(req)
	if err != nil {
		slog.Error("failed to call store management agent", "orderId", order.OrderID, "error", err)
		return
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		slog.Warn("store management agent returned unexpected status", "orderId", order.OrderID, "status", resp.StatusCode)
	}
}

// GetOrder retrieves an order by its UUID.
func (s *Store) GetOrder(orderID uuid.UUID) (*Order, bool) {
	return s.repo.GetOrder(orderID)
}

// UpdateOrderStatus updates the status of an existing order.
func (s *Store) UpdateOrderStatus(orderID uuid.UUID, status string) bool {
	return s.repo.UpdateOrderStatus(orderID, status)
}

// OrderEvent represents an event received from kitchen, delivery, or agent services.
type OrderEvent struct {
	OrderID   string `json:"orderId"`
	Status    string `json:"status"`
	Source    string `json:"source"`
	Message   string `json:"message,omitempty"`
	ToolName  string `json:"toolName,omitempty"`
	ToolInput string `json:"toolInput,omitempty"`
}

// HandleEvent handles POST /events requests to receive order status updates.
// It broadcasts the update to all connected WebSocket clients and, when the
// orderId is a valid UUID for a known order, updates the order status.
func (s *Store) HandleEvent(w http.ResponseWriter, r *http.Request) {
	var event OrderEvent
	if err := json.NewDecoder(r.Body).Decode(&event); err != nil {
		http.Error(w, "Invalid JSON", http.StatusBadRequest)
		return
	}

	// Try to update order status if orderId is a valid UUID for a known order.
	// A failed lookup is not fatal — progress events must still reach the UI.
	orderID, parseErr := uuid.Parse(event.OrderID)
	if parseErr == nil {
		// Only transition order status for meaningful state changes:
		// PENDING → COOKED → DELIVERED. Intermediate progress events
		// (e.g. oven_progress, ON_ROUTE) are tracked but do not alter the order status.
		switch event.Status {
		case "COOKING", "COOKED", "DELIVERING", "DELIVERED":
			if !s.UpdateOrderStatus(orderID, event.Status) {
				slog.Warn("order not found for event", "orderId", event.OrderID, "status", event.Status, "source", event.Source)
			}
		}
		s.trackEvent(orderID, event)
	} else {
		slog.Warn("event has non-UUID orderId, broadcasting without order update", "orderId", event.OrderID, "source", event.Source)
	}

	slog.Info("order event received", "orderId", event.OrderID, "status", event.Status, "source", event.Source)

	// Always broadcast to WebSocket clients so the UI receives every progress event.
	s.BroadcastOrderUpdate(OrderUpdate{
		OrderID:   event.OrderID,
		Status:    event.Status,
		Source:    event.Source,
		Message:   event.Message,
		ToolName:  event.ToolName,
		ToolInput: event.ToolInput,
	})

	w.WriteHeader(http.StatusOK)
}

// trackEvent stores an event in the order's event history keyed by UUID.
func (s *Store) trackEvent(orderID uuid.UUID, event OrderEvent) {
	if err := s.repo.TrackEvent(orderID, event); err != nil {
		slog.Error("failed to track event", "orderId", orderID, "error", err)
	}
}

// GetOrderEvents retrieves all events for a given order ID.
func (s *Store) GetOrderEvents(orderID uuid.UUID) []OrderEvent {
	events, err := s.repo.GetOrderEvents(orderID)
	if err != nil {
		slog.Error("failed to get order events", "orderId", orderID, "error", err)
		return nil
	}
	return events
}

// HandleGetOrders handles GET /orders requests to retrieve all orders.
func (s *Store) HandleGetOrders(w http.ResponseWriter, r *http.Request) {
	orders, err := s.repo.GetAllOrders()
	if err != nil {
		slog.Error("failed to get orders", "error", err)
		http.Error(w, "Failed to retrieve orders", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(orders)
}

// HandleGetEvents handles GET /events requests to retrieve events for a specific order.
func (s *Store) HandleGetEvents(w http.ResponseWriter, r *http.Request) {
	orderIDStr := r.URL.Query().Get("orderId")
	if orderIDStr == "" {
		http.Error(w, "orderId query parameter is required", http.StatusBadRequest)
		return
	}

	orderID, err := uuid.Parse(orderIDStr)
	if err != nil {
		http.Error(w, "Invalid orderId format", http.StatusBadRequest)
		return
	}

	events := s.GetOrderEvents(orderID)
	if events == nil {
		events = []OrderEvent{}
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(events)
}
