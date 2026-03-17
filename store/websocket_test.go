package store

import (
	"bytes"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/google/uuid"
	"github.com/gorilla/websocket"
)

// TestWebSocketConnection verifies that clients can connect via WebSocket using an orderId.
func TestWebSocketConnection(t *testing.T) {
	store := NewStore()
	router := chi.NewRouter()
	router.Get("/ws", store.HandleWebSocket)

	server := httptest.NewServer(router)
	defer server.Close()

	wsURL := "ws" + strings.TrimPrefix(server.URL, "http") + "/ws?orderId=" + uuid.New().String()

	conn, _, err := websocket.DefaultDialer.Dial(wsURL, nil)
	if err != nil {
		t.Fatalf("failed to connect to WebSocket: %v", err)
	}
	defer conn.Close()
}

// TestWebSocketReceivesOrderUpdates verifies that a client only receives events for its orderId.
func TestWebSocketReceivesOrderUpdates(t *testing.T) {
	store := NewStore()
	router := chi.NewRouter()
	router.Post("/order", store.HandleCreateOrder)
	router.Post("/events", store.HandleEvent)
	router.Get("/ws", store.HandleWebSocket)

	server := httptest.NewServer(router)
	defer server.Close()

	// Create an order first
	orderReq := CreateOrderRequest{
		OrderItems: []OrderItem{
			{PizzaType: "Margherita", Quantity: 1},
		},
		OrderData: "WebSocket test",
	}
	orderBody, _ := json.Marshal(orderReq)
	resp, err := http.Post(server.URL+"/order", "application/json", bytes.NewReader(orderBody))
	if err != nil {
		t.Fatalf("failed to create order: %v", err)
	}
	defer resp.Body.Close()

	var createdOrder Order
	json.NewDecoder(resp.Body).Decode(&createdOrder)

	// Connect using the orderId as the WebSocket identifier
	wsURL := "ws" + strings.TrimPrefix(server.URL, "http") + "/ws?orderId=" + createdOrder.OrderID.String()
	conn, _, err := websocket.DefaultDialer.Dial(wsURL, nil)
	if err != nil {
		t.Fatalf("failed to connect to WebSocket: %v", err)
	}
	defer conn.Close()

	// Send an event for this order
	eventReq := OrderEvent{
		OrderID: createdOrder.OrderID.String(),
		Status:  "cooking",
		Source:  "kitchen",
	}
	eventBody, _ := json.Marshal(eventReq)
	resp2, err := http.Post(server.URL+"/events", "application/json", bytes.NewReader(eventBody))
	if err != nil {
		t.Fatalf("failed to send event: %v", err)
	}
	defer resp2.Body.Close()

	// Read the update from WebSocket with a timeout
	conn.SetReadDeadline(time.Now().Add(2 * time.Second))
	_, message, err := conn.ReadMessage()
	if err != nil {
		t.Fatalf("failed to read WebSocket message: %v", err)
	}

	var event WebSocketEvent
	if err := json.Unmarshal(message, &event); err != nil {
		t.Fatalf("failed to unmarshal WebSocket message: %v", err)
	}

	if event.OrderID != createdOrder.OrderID.String() {
		t.Errorf("expected OrderID %v, got %v", createdOrder.OrderID.String(), event.OrderID)
	}
	if event.Status != "cooking" {
		t.Errorf("expected status 'cooking', got '%s'", event.Status)
	}
	if event.Source != "kitchen" {
		t.Errorf("expected source 'kitchen', got '%s'", event.Source)
	}
	if event.Timestamp == "" {
		t.Error("expected non-empty timestamp")
	}
}

// TestWebSocketConnectionWithOrderID verifies that clients can connect using an orderId.
func TestWebSocketConnectionWithOrderID(t *testing.T) {
	store := NewStore()
	router := chi.NewRouter()
	router.Get("/ws", store.HandleWebSocket)

	server := httptest.NewServer(router)
	defer server.Close()

	orderID := uuid.New().String()
	wsURL := "ws" + strings.TrimPrefix(server.URL, "http") + "/ws?orderId=" + orderID

	conn, _, err := websocket.DefaultDialer.Dial(wsURL, nil)
	if err != nil {
		t.Fatalf("failed to connect to WebSocket with orderId: %v", err)
	}
	defer conn.Close()

	if !store.hub.HasClient(orderID) {
		t.Error("expected connection to be registered for orderId")
	}
}

// TestWebSocketConnectionWithoutOrderIDIsRejected verifies connections without orderId are rejected.
func TestWebSocketConnectionWithoutOrderIDIsRejected(t *testing.T) {
	store := NewStore()
	router := chi.NewRouter()
	router.Get("/ws", store.HandleWebSocket)

	server := httptest.NewServer(router)
	defer server.Close()

	wsURL := "ws" + strings.TrimPrefix(server.URL, "http") + "/ws"

	_, resp, err := websocket.DefaultDialer.Dial(wsURL, nil)
	if err == nil {
		t.Fatal("expected connection to fail without orderId")
	}
	if resp != nil && resp.StatusCode != http.StatusBadRequest {
		t.Errorf("expected status 400, got %d", resp.StatusCode)
	}
}

// TestWebSocketOnlyDeliversToMatchingOrder verifies that events are sent only to the
// WebSocket connection registered for the matching orderId.
func TestWebSocketOnlyDeliversToMatchingOrder(t *testing.T) {
	store := NewStore()
	router := chi.NewRouter()
	router.Post("/order", store.HandleCreateOrder)
	router.Post("/events", store.HandleEvent)
	router.Get("/ws", store.HandleWebSocket)

	server := httptest.NewServer(router)
	defer server.Close()

	// Create two orders
	makeOrder := func() Order {
		body, _ := json.Marshal(CreateOrderRequest{OrderItems: []OrderItem{{PizzaType: "Margherita", Quantity: 1}}})
		resp, _ := http.Post(server.URL+"/order", "application/json", bytes.NewReader(body))
		defer resp.Body.Close()
		var o Order
		json.NewDecoder(resp.Body).Decode(&o)
		return o
	}
	order1 := makeOrder()
	order2 := makeOrder()

	// Connect each client with its own orderId
	connect := func(orderID string) *websocket.Conn {
		wsURL := "ws" + strings.TrimPrefix(server.URL, "http") + "/ws?orderId=" + orderID
		conn, _, err := websocket.DefaultDialer.Dial(wsURL, nil)
		if err != nil {
			t.Fatalf("failed to connect for orderId %s: %v", orderID, err)
		}
		return conn
	}
	conn1 := connect(order1.OrderID.String())
	defer conn1.Close()
	conn2 := connect(order2.OrderID.String())
	defer conn2.Close()

	// Send an event for order1 only
	eventBody, _ := json.Marshal(OrderEvent{
		OrderID: order1.OrderID.String(),
		Status:  "cooking",
		Source:  "kitchen",
	})
	http.Post(server.URL+"/events", "application/json", bytes.NewReader(eventBody))

	// conn1 should receive the event
	conn1.SetReadDeadline(time.Now().Add(2 * time.Second))
	_, msg, err := conn1.ReadMessage()
	if err != nil {
		t.Fatalf("conn1 expected to receive event: %v", err)
	}
	var event WebSocketEvent
	json.Unmarshal(msg, &event)
	if event.Status != "cooking" {
		t.Errorf("conn1 expected status 'cooking', got '%s'", event.Status)
	}

	// conn2 should NOT receive any event (different order)
	conn2.SetReadDeadline(time.Now().Add(300 * time.Millisecond))
	_, _, err = conn2.ReadMessage()
	if err == nil {
		t.Error("conn2 should not receive events for a different orderId")
	}
}

// TestMultipleWebSocketClients verifies that each client only receives events for its own order.
func TestMultipleWebSocketClients(t *testing.T) {
	store := NewStore()
	router := chi.NewRouter()
	router.Post("/order", store.HandleCreateOrder)
	router.Post("/events", store.HandleEvent)
	router.Get("/ws", store.HandleWebSocket)

	server := httptest.NewServer(router)
	defer server.Close()

	// Create two orders
	makeOrder := func(pizzaType string) Order {
		body, _ := json.Marshal(CreateOrderRequest{OrderItems: []OrderItem{{PizzaType: pizzaType, Quantity: 1}}})
		resp, _ := http.Post(server.URL+"/order", "application/json", bytes.NewReader(body))
		defer resp.Body.Close()
		var o Order
		json.NewDecoder(resp.Body).Decode(&o)
		return o
	}
	order1 := makeOrder("Margherita")
	order2 := makeOrder("Pepperoni")

	// Connect clients keyed by their own orderId
	wsURL1 := "ws" + strings.TrimPrefix(server.URL, "http") + "/ws?orderId=" + order1.OrderID.String()
	wsURL2 := "ws" + strings.TrimPrefix(server.URL, "http") + "/ws?orderId=" + order2.OrderID.String()

	conn1, _, err := websocket.DefaultDialer.Dial(wsURL1, nil)
	if err != nil {
		t.Fatalf("failed to connect client 1: %v", err)
	}
	defer conn1.Close()

	conn2, _, err := websocket.DefaultDialer.Dial(wsURL2, nil)
	if err != nil {
		t.Fatalf("failed to connect client 2: %v", err)
	}
	defer conn2.Close()

	// Send event for order1
	body1, _ := json.Marshal(OrderEvent{OrderID: order1.OrderID.String(), Status: "cooking", Source: "kitchen"})
	http.Post(server.URL+"/events", "application/json", bytes.NewReader(body1))

	// Send event for order2
	body2, _ := json.Marshal(OrderEvent{OrderID: order2.OrderID.String(), Status: "delivering", Source: "bikes"})
	http.Post(server.URL+"/events", "application/json", bytes.NewReader(body2))

	// conn1 receives cooking event
	conn1.SetReadDeadline(time.Now().Add(2 * time.Second))
	_, msg1, err := conn1.ReadMessage()
	if err != nil {
		t.Fatalf("client 1 failed to read: %v", err)
	}
	var event1 WebSocketEvent
	json.Unmarshal(msg1, &event1)
	if event1.Status != "cooking" {
		t.Errorf("client 1 expected status 'cooking', got '%s'", event1.Status)
	}

	// conn2 receives delivering event
	conn2.SetReadDeadline(time.Now().Add(2 * time.Second))
	_, msg2, err := conn2.ReadMessage()
	if err != nil {
		t.Fatalf("client 2 failed to read: %v", err)
	}
	var event2 WebSocketEvent
	json.Unmarshal(msg2, &event2)
	if event2.Status != "delivering" {
		t.Errorf("client 2 expected status 'delivering', got '%s'", event2.Status)
	}
}