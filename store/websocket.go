package store

import (
	"encoding/json"
	"log/slog"
	"net/http"
	"sync"
	"time"

	"github.com/gorilla/websocket"
)

// OrderUpdate represents an order status update sent to WebSocket clients.
type OrderUpdate struct {
	OrderID   string `json:"orderId"`
	Status    string `json:"status"`
	Source    string `json:"source"`
	Message   string `json:"message,omitempty"`
	ToolName  string `json:"toolName,omitempty"`
	ToolInput string `json:"toolInput,omitempty"`
}

// WebSocketEvent represents the event format sent to frontend clients via WebSocket.
type WebSocketEvent struct {
	OrderID   string `json:"orderId"`
	Status    string `json:"status"`
	Source    string `json:"source"`
	Timestamp string `json:"timestamp"`
	Message   string `json:"message,omitempty"`
	ToolName  string `json:"toolName,omitempty"`
	ToolInput string `json:"toolInput,omitempty"`
}

// WebSocketHub manages WebSocket client connections keyed by orderId.
type WebSocketHub struct {
	mu      sync.RWMutex
	clients map[string]*websocket.Conn // keyed by orderId
}

// NewWebSocketHub creates a new WebSocketHub instance.
func NewWebSocketHub() *WebSocketHub {
	return &WebSocketHub{
		clients: make(map[string]*websocket.Conn),
	}
}

// AddClient registers a WebSocket connection for the given orderId.
func (h *WebSocketHub) AddClient(orderID string, conn *websocket.Conn) {
	h.mu.Lock()
	defer h.mu.Unlock()
	h.clients[orderID] = conn
}

// RemoveClient unregisters the WebSocket connection for the given orderId.
func (h *WebSocketHub) RemoveClient(orderID string) {
	h.mu.Lock()
	defer h.mu.Unlock()
	delete(h.clients, orderID)
}

// HasClient reports whether there is an active connection for the given orderId.
func (h *WebSocketHub) HasClient(orderID string) bool {
	h.mu.RLock()
	defer h.mu.RUnlock()
	_, exists := h.clients[orderID]
	return exists
}

// SendToOrder sends a message to the WebSocket connection registered for orderID.
// If no connection exists for that order the message is silently dropped.
func (h *WebSocketHub) SendToOrder(orderID string, message []byte) {
	h.mu.RLock()
	conn, exists := h.clients[orderID]
	h.mu.RUnlock()

	if !exists {
		return
	}

	if err := conn.WriteMessage(websocket.TextMessage, message); err != nil {
		slog.Error("websocket write error", "orderId", orderID, "error", err)
		h.RemoveClient(orderID)
	}
}

var upgrader = websocket.Upgrader{
	CheckOrigin: func(r *http.Request) bool {
		return true // Allow all origins for development
	},
}

// HandleWebSocket handles WebSocket connection requests from frontend clients.
// It upgrades the HTTP connection to WebSocket and registers the connection
// keyed by the orderId query parameter so that only events for that order
// are forwarded to this client.
func (s *Store) HandleWebSocket(w http.ResponseWriter, r *http.Request) {
	orderID := r.URL.Query().Get("orderId")
	if orderID == "" {
		http.Error(w, "orderId query parameter is required", http.StatusBadRequest)
		return
	}

	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		slog.Error("websocket upgrade error", "error", err)
		return
	}

	s.hub.AddClient(orderID, conn)
	slog.Info("websocket client connected", "orderId", orderID)

	// Keep connection open and handle disconnection
	go func() {
		defer func() {
			s.hub.RemoveClient(orderID)
			conn.Close()
			slog.Info("websocket client disconnected", "orderId", orderID)
		}()

		for {
			_, _, err := conn.ReadMessage()
			if err != nil {
				break
			}
		}
	}()
}

// BroadcastOrderUpdate sends an order update only to the WebSocket client
// that is registered for the event's orderId.
func (s *Store) BroadcastOrderUpdate(update OrderUpdate) {
	event := WebSocketEvent{
		OrderID:   update.OrderID,
		Status:    update.Status,
		Source:    update.Source,
		Timestamp: time.Now().UTC().Format(time.RFC3339),
		Message:   update.Message,
		ToolName:  update.ToolName,
		ToolInput: update.ToolInput,
	}
	message, err := json.Marshal(event)
	if err != nil {
		slog.Error("failed to marshal websocket event", "error", err)
		return
	}
	s.hub.SendToOrder(update.OrderID, message)
}