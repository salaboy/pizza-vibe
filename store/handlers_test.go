package store

import (
	"bytes"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/google/uuid"
)

// TestPostOrder verifies that POST /order creates a new order and returns it with a UUID.
func TestPostOrder(t *testing.T) {
	store := NewStore()
	router := chi.NewRouter()
	router.Post("/order", store.HandleCreateOrder)

	// Create order request payload
	reqBody := CreateOrderRequest{
		OrderItems: []OrderItem{
			{PizzaType: "Margherita", Quantity: 2},
			{PizzaType: "Pepperoni", Quantity: 1},
		},
		OrderData: "Ring the doorbell",
	}
	body, _ := json.Marshal(reqBody)

	req := httptest.NewRequest(http.MethodPost, "/order", bytes.NewReader(body))
	req.Header.Set("Content-Type", "application/json")
	rec := httptest.NewRecorder()

	router.ServeHTTP(rec, req)

	if rec.Code != http.StatusCreated {
		t.Errorf("expected status 201 Created, got %d", rec.Code)
	}

	var response Order
	err := json.Unmarshal(rec.Body.Bytes(), &response)
	if err != nil {
		t.Fatalf("failed to unmarshal response: %v", err)
	}

	// Verify OrderID is a valid UUID
	if response.OrderID == uuid.Nil {
		t.Error("expected valid OrderID, got nil UUID")
	}

	// Verify order items
	if len(response.OrderItems) != 2 {
		t.Errorf("expected 2 order items, got %d", len(response.OrderItems))
	}

	// Verify order status is set to "pending"
	if response.OrderStatus != "pending" {
		t.Errorf("expected OrderStatus 'pending', got '%s'", response.OrderStatus)
	}

	// Verify order data
	if response.OrderData != "Ring the doorbell" {
		t.Errorf("expected OrderData 'Ring the doorbell', got '%s'", response.OrderData)
	}
}

// TestPostOrderInvalidJSON verifies that POST /order returns 400 for invalid JSON.
func TestPostOrderInvalidJSON(t *testing.T) {
	store := NewStore()
	router := chi.NewRouter()
	router.Post("/order", store.HandleCreateOrder)

	req := httptest.NewRequest(http.MethodPost, "/order", bytes.NewReader([]byte("invalid json")))
	req.Header.Set("Content-Type", "application/json")
	rec := httptest.NewRecorder()

	router.ServeHTTP(rec, req)

	if rec.Code != http.StatusBadRequest {
		t.Errorf("expected status 400 Bad Request, got %d", rec.Code)
	}
}

// TestPostOrderEmptyItems verifies that POST /order returns 400 when no items provided.
func TestPostOrderEmptyItems(t *testing.T) {
	store := NewStore()
	router := chi.NewRouter()
	router.Post("/order", store.HandleCreateOrder)

	reqBody := CreateOrderRequest{
		OrderItems: []OrderItem{},
		OrderData:  "Empty order",
	}
	body, _ := json.Marshal(reqBody)

	req := httptest.NewRequest(http.MethodPost, "/order", bytes.NewReader(body))
	req.Header.Set("Content-Type", "application/json")
	rec := httptest.NewRecorder()

	router.ServeHTTP(rec, req)

	if rec.Code != http.StatusBadRequest {
		t.Errorf("expected status 400 Bad Request, got %d", rec.Code)
	}
}

// TestPostEvents verifies that POST /events updates order status from kitchen/delivery events.
func TestPostEvents(t *testing.T) {
	store := NewStore()
	router := chi.NewRouter()
	router.Post("/order", store.HandleCreateOrder)
	router.Post("/events", store.HandleEvent)

	// First, create an order
	orderReq := CreateOrderRequest{
		OrderItems: []OrderItem{
			{PizzaType: "Margherita", Quantity: 1},
		},
		OrderData: "Test order",
	}
	orderBody, _ := json.Marshal(orderReq)
	createReq := httptest.NewRequest(http.MethodPost, "/order", bytes.NewReader(orderBody))
	createReq.Header.Set("Content-Type", "application/json")
	createRec := httptest.NewRecorder()
	router.ServeHTTP(createRec, createReq)

	var createdOrder Order
	json.Unmarshal(createRec.Body.Bytes(), &createdOrder)

	// Now send an event to update the order status
	eventReq := OrderEvent{
		OrderID: createdOrder.OrderID.String(),
		Status:  "cooking",
		Source:  "kitchen",
	}
	eventBody, _ := json.Marshal(eventReq)
	req := httptest.NewRequest(http.MethodPost, "/events", bytes.NewReader(eventBody))
	req.Header.Set("Content-Type", "application/json")
	rec := httptest.NewRecorder()

	router.ServeHTTP(rec, req)

	if rec.Code != http.StatusOK {
		t.Errorf("expected status 200 OK, got %d", rec.Code)
	}

	// Verify the order status was updated
	order, exists := store.GetOrder(createdOrder.OrderID)
	if !exists {
		t.Fatal("order not found")
	}
	if order.OrderStatus != "cooking" {
		t.Errorf("expected OrderStatus 'cooking', got '%s'", order.OrderStatus)
	}
}

// TestPostEventsUnknownOrderID verifies that POST /events returns 200 even for a non-existent
// order, because progress events must always be broadcast to the UI regardless of order lookup.
func TestPostEventsUnknownOrderID(t *testing.T) {
	store := NewStore()
	router := chi.NewRouter()
	router.Post("/events", store.HandleEvent)

	eventReq := OrderEvent{
		OrderID: uuid.New().String(), // Valid UUID but no matching order
		Status:  "cooking",
		Source:  "kitchen",
	}
	eventBody, _ := json.Marshal(eventReq)
	req := httptest.NewRequest(http.MethodPost, "/events", bytes.NewReader(eventBody))
	req.Header.Set("Content-Type", "application/json")
	rec := httptest.NewRecorder()

	router.ServeHTTP(rec, req)

	if rec.Code != http.StatusOK {
		t.Errorf("expected status 200 OK, got %d", rec.Code)
	}
}

// TestPostEventsInvalidJSON verifies that POST /events returns 400 for invalid JSON.
func TestPostEventsInvalidJSON(t *testing.T) {
	store := NewStore()
	router := chi.NewRouter()
	router.Post("/events", store.HandleEvent)

	req := httptest.NewRequest(http.MethodPost, "/events", bytes.NewReader([]byte("invalid")))
	req.Header.Set("Content-Type", "application/json")
	rec := httptest.NewRecorder()

	router.ServeHTTP(rec, req)

	if rec.Code != http.StatusBadRequest {
		t.Errorf("expected status 400 Bad Request, got %d", rec.Code)
	}
}

// TestPostOrderCallsStoreMgmtAgent verifies that POST /order calls the store management agent
// with a ProcessOrderRequest containing the orderId and orderItems.
func TestPostOrderCallsStoreMgmtAgent(t *testing.T) {
	var receivedRequest ProcessOrderRequest
	agentCalled := make(chan bool, 1)
	agentServer := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path == "/mgmt/processOrder" && r.Method == http.MethodPost {
			json.NewDecoder(r.Body).Decode(&receivedRequest)
			w.WriteHeader(http.StatusOK)
			agentCalled <- true
		}
	}))
	defer agentServer.Close()

	store := NewStore()
	store.SetAgentURL(agentServer.URL)

	router := chi.NewRouter()
	router.Post("/order", store.HandleCreateOrder)

	reqBody := CreateOrderRequest{
		OrderItems: []OrderItem{
			{PizzaType: "Margherita", Quantity: 2},
		},
		OrderData: "Test order",
	}
	body, _ := json.Marshal(reqBody)

	req := httptest.NewRequest(http.MethodPost, "/order", bytes.NewReader(body))
	req.Header.Set("Content-Type", "application/json")
	rec := httptest.NewRecorder()

	router.ServeHTTP(rec, req)

	if rec.Code != http.StatusCreated {
		t.Errorf("expected status 201 Created, got %d", rec.Code)
	}

	// Wait for the agent to be called (with timeout)
	select {
	case <-agentCalled:
		// Agent was called
	case <-time.After(2 * time.Second):
		t.Fatal("timed out waiting for store management agent to be called")
	}

	// Verify the agent received the correct order data
	var response Order
	json.Unmarshal(rec.Body.Bytes(), &response)

	if receivedRequest.OrderID != response.OrderID {
		t.Errorf("expected agent to receive orderId %s, got %s", response.OrderID, receivedRequest.OrderID)
	}

	if len(receivedRequest.OrderItems) != 1 {
		t.Errorf("expected agent to receive 1 order item, got %d", len(receivedRequest.OrderItems))
	}

	if receivedRequest.OrderItems[0].PizzaType != "Margherita" {
		t.Errorf("expected agent to receive Margherita pizza, got %s", receivedRequest.OrderItems[0].PizzaType)
	}
}

// TestEventsAreTrackedPerOrderID verifies that events are tracked per order ID.
func TestEventsAreTrackedPerOrderID(t *testing.T) {
	store := NewStore()
	router := chi.NewRouter()
	router.Post("/order", store.HandleCreateOrder)
	router.Post("/events", store.HandleEvent)

	// Create an order
	orderReq := CreateOrderRequest{
		OrderItems: []OrderItem{
			{PizzaType: "Margherita", Quantity: 1},
		},
		OrderData: "Test order",
	}
	orderBody, _ := json.Marshal(orderReq)
	createReq := httptest.NewRequest(http.MethodPost, "/order", bytes.NewReader(orderBody))
	createReq.Header.Set("Content-Type", "application/json")
	createRec := httptest.NewRecorder()
	router.ServeHTTP(createRec, createReq)

	var createdOrder Order
	json.Unmarshal(createRec.Body.Bytes(), &createdOrder)

	// Send multiple events
	events := []OrderEvent{
		{OrderID: createdOrder.OrderID.String(), Status: "cooking", Source: "kitchen"},
		{OrderID: createdOrder.OrderID.String(), Status: "preparing pizza", Source: "kitchen"},
		{OrderID: createdOrder.OrderID.String(), Status: "in oven", Source: "kitchen"},
	}

	for _, event := range events {
		eventBody, _ := json.Marshal(event)
		req := httptest.NewRequest(http.MethodPost, "/events", bytes.NewReader(eventBody))
		req.Header.Set("Content-Type", "application/json")
		rec := httptest.NewRecorder()
		router.ServeHTTP(rec, req)
	}

	// Verify all events are tracked
	trackedEvents := store.GetOrderEvents(createdOrder.OrderID)
	if len(trackedEvents) != 3 {
		t.Errorf("expected 3 tracked events, got %d", len(trackedEvents))
	}

	// Verify event order
	if trackedEvents[0].Status != "cooking" {
		t.Errorf("expected first event status 'cooking', got '%s'", trackedEvents[0].Status)
	}
	if trackedEvents[1].Status != "preparing pizza" {
		t.Errorf("expected second event status 'preparing pizza', got '%s'", trackedEvents[1].Status)
	}
	if trackedEvents[2].Status != "in oven" {
		t.Errorf("expected third event status 'in oven', got '%s'", trackedEvents[2].Status)
	}
}

// TestDoneEventUpdatesStatusToDone verifies that a DONE event updates the order status to DONE.
func TestDoneEventUpdatesStatusToDone(t *testing.T) {
	store := NewStore()
	router := chi.NewRouter()
	router.Post("/order", store.HandleCreateOrder)
	router.Post("/events", store.HandleEvent)

	// Create an order
	orderReq := CreateOrderRequest{
		OrderItems: []OrderItem{
			{PizzaType: "Margherita", Quantity: 1},
		},
		OrderData: "Test order",
	}
	orderBody, _ := json.Marshal(orderReq)
	createReq := httptest.NewRequest(http.MethodPost, "/order", bytes.NewReader(orderBody))
	createReq.Header.Set("Content-Type", "application/json")
	createRec := httptest.NewRecorder()
	router.ServeHTTP(createRec, createReq)

	var createdOrder Order
	json.Unmarshal(createRec.Body.Bytes(), &createdOrder)

	// Send DONE event
	doneEvent := OrderEvent{
		OrderID: createdOrder.OrderID.String(),
		Status:  "DONE",
		Source:  "kitchen",
	}
	eventBody, _ := json.Marshal(doneEvent)
	req := httptest.NewRequest(http.MethodPost, "/events", bytes.NewReader(eventBody))
	req.Header.Set("Content-Type", "application/json")
	rec := httptest.NewRecorder()
	router.ServeHTTP(rec, req)

	if rec.Code != http.StatusOK {
		t.Errorf("expected status 200 OK, got %d", rec.Code)
	}

	// Verify the order status is now DONE
	order, exists := store.GetOrder(createdOrder.OrderID)
	if !exists {
		t.Fatal("order not found")
	}
	if order.OrderStatus != "DONE" {
		t.Errorf("expected OrderStatus 'DONE', got '%s'", order.OrderStatus)
	}
}

// TestGetOrders verifies that GET /orders returns all orders.
func TestGetOrders(t *testing.T) {
	store := NewStore()
	router := chi.NewRouter()
	router.Post("/order", store.HandleCreateOrder)
	router.Get("/orders", store.HandleGetOrders)

	// Create two orders
	orderReq1 := CreateOrderRequest{
		OrderItems: []OrderItem{
			{PizzaType: "Margherita", Quantity: 2},
		},
		OrderData: "Order 1",
	}
	orderReq2 := CreateOrderRequest{
		OrderItems: []OrderItem{
			{PizzaType: "Pepperoni", Quantity: 1},
		},
		OrderData: "Order 2",
	}

	body1, _ := json.Marshal(orderReq1)
	req1 := httptest.NewRequest(http.MethodPost, "/order", bytes.NewReader(body1))
	req1.Header.Set("Content-Type", "application/json")
	rec1 := httptest.NewRecorder()
	router.ServeHTTP(rec1, req1)

	body2, _ := json.Marshal(orderReq2)
	req2 := httptest.NewRequest(http.MethodPost, "/order", bytes.NewReader(body2))
	req2.Header.Set("Content-Type", "application/json")
	rec2 := httptest.NewRecorder()
	router.ServeHTTP(rec2, req2)

	// GET /orders
	getReq := httptest.NewRequest(http.MethodGet, "/orders", nil)
	getRec := httptest.NewRecorder()
	router.ServeHTTP(getRec, getReq)

	if getRec.Code != http.StatusOK {
		t.Errorf("expected status 200 OK, got %d", getRec.Code)
	}

	var orders []Order
	err := json.Unmarshal(getRec.Body.Bytes(), &orders)
	if err != nil {
		t.Fatalf("failed to unmarshal response: %v", err)
	}

	if len(orders) != 2 {
		t.Errorf("expected 2 orders, got %d", len(orders))
	}
}

// TestGetOrdersEmpty verifies that GET /orders returns empty array when no orders exist.
func TestGetOrdersEmpty(t *testing.T) {
	store := NewStore()
	router := chi.NewRouter()
	router.Get("/orders", store.HandleGetOrders)

	req := httptest.NewRequest(http.MethodGet, "/orders", nil)
	rec := httptest.NewRecorder()
	router.ServeHTTP(rec, req)

	if rec.Code != http.StatusOK {
		t.Errorf("expected status 200 OK, got %d", rec.Code)
	}

	var orders []Order
	err := json.Unmarshal(rec.Body.Bytes(), &orders)
	if err != nil {
		t.Fatalf("failed to unmarshal response: %v", err)
	}

	if len(orders) != 0 {
		t.Errorf("expected 0 orders, got %d", len(orders))
	}
}

// TestGetEventsForOrder verifies that GET /events returns events for a specific order.
func TestGetEventsForOrder(t *testing.T) {
	store := NewStore()
	router := chi.NewRouter()
	router.Post("/order", store.HandleCreateOrder)
	router.Post("/events", store.HandleEvent)
	router.Get("/events", store.HandleGetEvents)

	// Create an order
	orderReq := CreateOrderRequest{
		OrderItems: []OrderItem{
			{PizzaType: "Margherita", Quantity: 1},
		},
		OrderData: "Test order",
	}
	orderBody, _ := json.Marshal(orderReq)
	createReq := httptest.NewRequest(http.MethodPost, "/order", bytes.NewReader(orderBody))
	createReq.Header.Set("Content-Type", "application/json")
	createRec := httptest.NewRecorder()
	router.ServeHTTP(createRec, createReq)

	var createdOrder Order
	json.Unmarshal(createRec.Body.Bytes(), &createdOrder)

	// Send multiple events
	events := []OrderEvent{
		{OrderID: createdOrder.OrderID.String(), Status: "cooking", Source: "kitchen"},
		{OrderID: createdOrder.OrderID.String(), Status: "in oven", Source: "kitchen"},
		{OrderID: createdOrder.OrderID.String(), Status: "DONE", Source: "kitchen"},
	}

	for _, event := range events {
		eventBody, _ := json.Marshal(event)
		req := httptest.NewRequest(http.MethodPost, "/events", bytes.NewReader(eventBody))
		req.Header.Set("Content-Type", "application/json")
		rec := httptest.NewRecorder()
		router.ServeHTTP(rec, req)
	}

	// GET /events?orderId={orderId}
	getReq := httptest.NewRequest(http.MethodGet, "/events?orderId="+createdOrder.OrderID.String(), nil)
	getRec := httptest.NewRecorder()
	router.ServeHTTP(getRec, getReq)

	if getRec.Code != http.StatusOK {
		t.Errorf("expected status 200 OK, got %d", getRec.Code)
	}

	var returnedEvents []OrderEvent
	err := json.Unmarshal(getRec.Body.Bytes(), &returnedEvents)
	if err != nil {
		t.Fatalf("failed to unmarshal response: %v", err)
	}

	if len(returnedEvents) != 3 {
		t.Errorf("expected 3 events, got %d", len(returnedEvents))
	}

	if returnedEvents[0].Status != "cooking" {
		t.Errorf("expected first event status 'cooking', got '%s'", returnedEvents[0].Status)
	}
}

// TestGetEventsWithoutOrderId verifies that GET /events returns 400 when orderId is missing.
func TestGetEventsWithoutOrderId(t *testing.T) {
	store := NewStore()
	router := chi.NewRouter()
	router.Get("/events", store.HandleGetEvents)

	req := httptest.NewRequest(http.MethodGet, "/events", nil)
	rec := httptest.NewRecorder()
	router.ServeHTTP(rec, req)

	if rec.Code != http.StatusBadRequest {
		t.Errorf("expected status 400 Bad Request, got %d", rec.Code)
	}
}

// TestGetEventsInvalidOrderId verifies that GET /events returns 400 for invalid UUID.
func TestGetEventsInvalidOrderId(t *testing.T) {
	store := NewStore()
	router := chi.NewRouter()
	router.Get("/events", store.HandleGetEvents)

	req := httptest.NewRequest(http.MethodGet, "/events?orderId=invalid-uuid", nil)
	rec := httptest.NewRecorder()
	router.ServeHTTP(rec, req)

	if rec.Code != http.StatusBadRequest {
		t.Errorf("expected status 400 Bad Request, got %d", rec.Code)
	}
}

// TestGetEventsEmptyForOrder verifies that GET /events returns empty array for order without events.
func TestGetEventsEmptyForOrder(t *testing.T) {
	store := NewStore()
	router := chi.NewRouter()
	router.Post("/order", store.HandleCreateOrder)
	router.Get("/events", store.HandleGetEvents)

	// Create an order but don't send any events
	orderReq := CreateOrderRequest{
		OrderItems: []OrderItem{
			{PizzaType: "Margherita", Quantity: 1},
		},
		OrderData: "Test order",
	}
	orderBody, _ := json.Marshal(orderReq)
	createReq := httptest.NewRequest(http.MethodPost, "/order", bytes.NewReader(orderBody))
	createReq.Header.Set("Content-Type", "application/json")
	createRec := httptest.NewRecorder()
	router.ServeHTTP(createRec, createReq)

	var createdOrder Order
	json.Unmarshal(createRec.Body.Bytes(), &createdOrder)

	// GET /events?orderId={orderId}
	getReq := httptest.NewRequest(http.MethodGet, "/events?orderId="+createdOrder.OrderID.String(), nil)
	getRec := httptest.NewRecorder()
	router.ServeHTTP(getRec, getReq)

	if getRec.Code != http.StatusOK {
		t.Errorf("expected status 200 OK, got %d", getRec.Code)
	}

	var returnedEvents []OrderEvent
	err := json.Unmarshal(getRec.Body.Bytes(), &returnedEvents)
	if err != nil {
		t.Fatalf("failed to unmarshal response: %v", err)
	}

	if len(returnedEvents) != 0 {
		t.Errorf("expected 0 events, got %d", len(returnedEvents))
	}
}

// TestEventOnlyUpdatesOrderStatus verifies that events only update the order status
// without triggering any downstream service calls.
func TestEventOnlyUpdatesOrderStatus(t *testing.T) {
	store := NewStore()
	router := chi.NewRouter()
	router.Post("/order", store.HandleCreateOrder)
	router.Post("/events", store.HandleEvent)

	// Create an order
	orderReq := CreateOrderRequest{
		OrderItems: []OrderItem{
			{PizzaType: "Margherita", Quantity: 2},
			{PizzaType: "Pepperoni", Quantity: 1},
		},
		OrderData: "Test order",
	}
	orderBody, _ := json.Marshal(orderReq)
	createReq := httptest.NewRequest(http.MethodPost, "/order", bytes.NewReader(orderBody))
	createReq.Header.Set("Content-Type", "application/json")
	createRec := httptest.NewRecorder()
	router.ServeHTTP(createRec, createReq)

	var createdOrder Order
	json.Unmarshal(createRec.Body.Bytes(), &createdOrder)

	statuses := []string{"COOKING", "DONE", "ON_ROUTE", "DELIVERED"}
	for _, status := range statuses {
		event := OrderEvent{
			OrderID: createdOrder.OrderID.String(),
			Status:  status,
			Source:  "agent",
		}
		eventBody, _ := json.Marshal(event)
		req := httptest.NewRequest(http.MethodPost, "/events", bytes.NewReader(eventBody))
		req.Header.Set("Content-Type", "application/json")
		rec := httptest.NewRecorder()
		router.ServeHTTP(rec, req)

		if rec.Code != http.StatusOK {
			t.Errorf("status %s: expected 200 OK, got %d", status, rec.Code)
		}

		order, exists := store.GetOrder(createdOrder.OrderID)
		if !exists {
			t.Fatalf("order not found after %s event", status)
		}
		if order.OrderStatus != status {
			t.Errorf("expected OrderStatus '%s', got '%s'", status, order.OrderStatus)
		}
	}
}

// TestDeliveredEventUpdatesOrderStatus verifies that a DELIVERED event from delivery
// updates the order status to DELIVERED.
func TestDeliveredEventUpdatesOrderStatus(t *testing.T) {
	store := NewStore()
	router := chi.NewRouter()
	router.Post("/order", store.HandleCreateOrder)
	router.Post("/events", store.HandleEvent)

	// Create an order
	orderReq := CreateOrderRequest{
		OrderItems: []OrderItem{
			{PizzaType: "Margherita", Quantity: 1},
		},
		OrderData: "Test order",
	}
	orderBody, _ := json.Marshal(orderReq)
	createReq := httptest.NewRequest(http.MethodPost, "/order", bytes.NewReader(orderBody))
	createReq.Header.Set("Content-Type", "application/json")
	createRec := httptest.NewRecorder()
	router.ServeHTTP(createRec, createReq)

	var createdOrder Order
	json.Unmarshal(createRec.Body.Bytes(), &createdOrder)

	// Send DELIVERED event from delivery
	deliveredEvent := OrderEvent{
		OrderID: createdOrder.OrderID.String(),
		Status:  "DELIVERED",
		Source:  "delivery",
	}
	eventBody, _ := json.Marshal(deliveredEvent)
	req := httptest.NewRequest(http.MethodPost, "/events", bytes.NewReader(eventBody))
	req.Header.Set("Content-Type", "application/json")
	rec := httptest.NewRecorder()
	router.ServeHTTP(rec, req)

	if rec.Code != http.StatusOK {
		t.Errorf("expected status 200 OK, got %d", rec.Code)
	}

	// Verify the order status is now DELIVERED
	order, exists := store.GetOrder(createdOrder.OrderID)
	if !exists {
		t.Fatal("order not found")
	}
	if order.OrderStatus != "DELIVERED" {
		t.Errorf("expected OrderStatus 'DELIVERED', got '%s'", order.OrderStatus)
	}
}

// TestEventPassesThroughCookingUpdateData verifies that rich cooking data (message, toolName, toolInput)
// is passed through from kitchen events to the WebSocket broadcast.
func TestEventPassesThroughCookingUpdateData(t *testing.T) {
	store := NewStore()
	router := chi.NewRouter()
	router.Post("/order", store.HandleCreateOrder)
	router.Post("/events", store.HandleEvent)

	// Create an order
	orderReq := CreateOrderRequest{
		OrderItems: []OrderItem{{PizzaType: "Margherita", Quantity: 1}},
	}
	orderBody, _ := json.Marshal(orderReq)
	createReq := httptest.NewRequest(http.MethodPost, "/order", bytes.NewReader(orderBody))
	createReq.Header.Set("Content-Type", "application/json")
	createRec := httptest.NewRecorder()
	router.ServeHTTP(createRec, createReq)

	var createdOrder Order
	json.Unmarshal(createRec.Body.Bytes(), &createdOrder)

	// Send an event with rich cooking data
	event := OrderEvent{
		OrderID:   createdOrder.OrderID.String(),
		Status:    "checking_inventory",
		Source:    "kitchen",
		Message:   "Checking available ingredients in inventory",
		ToolName:  "getInventory",
		ToolInput: "{}",
	}
	eventBody, _ := json.Marshal(event)
	req := httptest.NewRequest(http.MethodPost, "/events", bytes.NewReader(eventBody))
	req.Header.Set("Content-Type", "application/json")
	rec := httptest.NewRecorder()
	router.ServeHTTP(rec, req)

	if rec.Code != http.StatusOK {
		t.Errorf("expected status 200 OK, got %d", rec.Code)
	}

	// Verify the event is tracked with rich data
	trackedEvents := store.GetOrderEvents(createdOrder.OrderID)
	if len(trackedEvents) != 1 {
		t.Fatalf("expected 1 event, got %d", len(trackedEvents))
	}
	if trackedEvents[0].Message != "Checking available ingredients in inventory" {
		t.Errorf("expected message to be preserved, got %q", trackedEvents[0].Message)
	}
	if trackedEvents[0].ToolName != "getInventory" {
		t.Errorf("expected toolName to be preserved, got %q", trackedEvents[0].ToolName)
	}
}

// TestPostAgentEvent verifies that POST /agents-events stores an agent event.
func TestPostAgentEvent(t *testing.T) {
	store := NewStore()
	router := chi.NewRouter()
	router.Post("/agents-events", store.HandleAgentEvent)
	router.Get("/agents-events", store.HandleGetAgentEvents)

	event := AgentEvent{
		AgentID:   "cooking-agent",
		Kind:      "request",
		Text:      "Starting to cook the pizza",
		Timestamp: FlexTimestamp{time.Now().UTC()},
	}
	body, _ := json.Marshal(event)

	req := httptest.NewRequest(http.MethodPost, "/agents-events", bytes.NewReader(body))
	req.Header.Set("Content-Type", "application/json")
	rec := httptest.NewRecorder()
	router.ServeHTTP(rec, req)

	if rec.Code != http.StatusOK {
		t.Errorf("expected status 200 OK, got %d", rec.Code)
	}

	// Retrieve all events
	getReq := httptest.NewRequest(http.MethodGet, "/agents-events", nil)
	getRec := httptest.NewRecorder()
	router.ServeHTTP(getRec, getReq)

	if getRec.Code != http.StatusOK {
		t.Errorf("expected status 200 OK, got %d", getRec.Code)
	}

	var events []AgentEvent
	json.Unmarshal(getRec.Body.Bytes(), &events)
	if len(events) != 1 {
		t.Fatalf("expected 1 event, got %d", len(events))
	}
	if events[0].AgentID != "cooking-agent" {
		t.Errorf("expected agentId 'cooking-agent', got '%s'", events[0].AgentID)
	}
	if events[0].Kind != "request" {
		t.Errorf("expected kind 'request', got '%s'", events[0].Kind)
	}
	if events[0].Text != "Starting to cook the pizza" {
		t.Errorf("expected text 'Starting to cook the pizza', got '%s'", events[0].Text)
	}
}

// TestPostAgentEventMissingFields verifies that POST /agents-events returns 400 when required fields are missing.
func TestPostAgentEventMissingFields(t *testing.T) {
	store := NewStore()
	router := chi.NewRouter()
	router.Post("/agents-events", store.HandleAgentEvent)

	event := AgentEvent{
		Kind:      "request",
		Text:      "Some text",
		Timestamp: FlexTimestamp{time.Now().UTC()},
	}
	body, _ := json.Marshal(event)

	req := httptest.NewRequest(http.MethodPost, "/agents-events", bytes.NewReader(body))
	req.Header.Set("Content-Type", "application/json")
	rec := httptest.NewRecorder()
	router.ServeHTTP(rec, req)

	if rec.Code != http.StatusBadRequest {
		t.Errorf("expected status 400 Bad Request, got %d", rec.Code)
	}
}

// TestPostAgentEventInvalidKind verifies that POST /agents-events returns 400 for an invalid kind.
func TestPostAgentEventInvalidKind(t *testing.T) {
	store := NewStore()
	router := chi.NewRouter()
	router.Post("/agents-events", store.HandleAgentEvent)

	event := AgentEvent{
		AgentID:   "cooking-agent",
		Kind:      "unknown",
		Text:      "Some text",
		Timestamp: FlexTimestamp{time.Now().UTC()},
	}
	body, _ := json.Marshal(event)

	req := httptest.NewRequest(http.MethodPost, "/agents-events", bytes.NewReader(body))
	req.Header.Set("Content-Type", "application/json")
	rec := httptest.NewRecorder()
	router.ServeHTTP(rec, req)

	if rec.Code != http.StatusBadRequest {
		t.Errorf("expected status 400 Bad Request, got %d", rec.Code)
	}
}

// TestPostAgentEventInvalidJSON verifies that POST /agents-events returns 400 for invalid JSON.
func TestPostAgentEventInvalidJSON(t *testing.T) {
	store := NewStore()
	router := chi.NewRouter()
	router.Post("/agents-events", store.HandleAgentEvent)

	req := httptest.NewRequest(http.MethodPost, "/agents-events", bytes.NewReader([]byte("invalid")))
	req.Header.Set("Content-Type", "application/json")
	rec := httptest.NewRecorder()
	router.ServeHTTP(rec, req)

	if rec.Code != http.StatusBadRequest {
		t.Errorf("expected status 400 Bad Request, got %d", rec.Code)
	}
}

// TestPostAgentEventNumericTimestamp verifies that numeric epoch timestamps (from Java) are accepted.
func TestPostAgentEventNumericTimestamp(t *testing.T) {
	store := NewStore()
	router := chi.NewRouter()
	router.Post("/agents-events", store.HandleAgentEvent)
	router.Get("/agents-events", store.HandleGetAgentEvents)

	// Simulate Java Jackson serializing Instant as epoch seconds
	rawJSON := `{"agentId":"store-mgmt-agent","kind":"request","text":"Hello","timestamp":1710230400.123456789}`

	req := httptest.NewRequest(http.MethodPost, "/agents-events", bytes.NewReader([]byte(rawJSON)))
	req.Header.Set("Content-Type", "application/json")
	rec := httptest.NewRecorder()
	router.ServeHTTP(rec, req)

	if rec.Code != http.StatusOK {
		t.Errorf("expected status 200 OK, got %d; body: %s", rec.Code, rec.Body.String())
	}

	// Verify the event was stored and timestamp was parsed
	getReq := httptest.NewRequest(http.MethodGet, "/agents-events", nil)
	getRec := httptest.NewRecorder()
	router.ServeHTTP(getRec, getReq)

	var events []AgentEvent
	json.Unmarshal(getRec.Body.Bytes(), &events)
	if len(events) != 1 {
		t.Fatalf("expected 1 event, got %d", len(events))
	}
	if events[0].Timestamp.Year() != 2024 {
		t.Errorf("expected year 2024 from epoch 1710230400, got %d", events[0].Timestamp.Year())
	}
}

// TestPostAgentEventFiltersToolExecutionResultMessage verifies that events containing
// ToolExecutionResultMessage in the text are accepted but not stored.
func TestPostAgentEventFiltersToolExecutionResultMessage(t *testing.T) {
	store := NewStore()
	router := chi.NewRouter()
	router.Post("/agents-events", store.HandleAgentEvent)
	router.Get("/agents-events", store.HandleGetAgentEvents)

	event := AgentEvent{
		AgentID:   "store-mgmt-agent",
		Kind:      "request",
		Text:      "ToolExecutionResultMessage { id = 123, toolName = getInventory, text = ... }",
		Timestamp: FlexTimestamp{time.Now().UTC()},
	}
	body, _ := json.Marshal(event)

	req := httptest.NewRequest(http.MethodPost, "/agents-events", bytes.NewReader(body))
	req.Header.Set("Content-Type", "application/json")
	rec := httptest.NewRecorder()
	router.ServeHTTP(rec, req)

	if rec.Code != http.StatusOK {
		t.Errorf("expected status 200 OK, got %d", rec.Code)
	}

	// Verify the event was NOT stored
	getReq := httptest.NewRequest(http.MethodGet, "/agents-events", nil)
	getRec := httptest.NewRecorder()
	router.ServeHTTP(getRec, getReq)

	var events []AgentEvent
	json.Unmarshal(getRec.Body.Bytes(), &events)
	if len(events) != 0 {
		t.Errorf("expected 0 events (filtered), got %d", len(events))
	}
}

// TestGetAllAgentEvents verifies that GET /agents-events without filters returns all events.
func TestGetAllAgentEvents(t *testing.T) {
	store := NewStore()
	router := chi.NewRouter()
	router.Post("/agents-events", store.HandleAgentEvent)
	router.Get("/agents-events", store.HandleGetAgentEvents)

	events := []AgentEvent{
		{AgentID: "cooking-agent", Kind: "request", Text: "Event 1", Timestamp: FlexTimestamp{time.Now().UTC()}},
		{AgentID: "delivery-agent", Kind: "response", Text: "Event 2", Timestamp: FlexTimestamp{time.Now().UTC()}},
	}
	for _, event := range events {
		body, _ := json.Marshal(event)
		req := httptest.NewRequest(http.MethodPost, "/agents-events", bytes.NewReader(body))
		req.Header.Set("Content-Type", "application/json")
		rec := httptest.NewRecorder()
		router.ServeHTTP(rec, req)
	}

	req := httptest.NewRequest(http.MethodGet, "/agents-events", nil)
	rec := httptest.NewRecorder()
	router.ServeHTTP(rec, req)

	if rec.Code != http.StatusOK {
		t.Errorf("expected status 200 OK, got %d", rec.Code)
	}

	var returned []AgentEvent
	json.Unmarshal(rec.Body.Bytes(), &returned)
	if len(returned) != 2 {
		t.Errorf("expected 2 events, got %d", len(returned))
	}
}

// TestGetAgentEventsByAgentId verifies that GET /agents-events?agentId=X returns events for that agent.
func TestGetAgentEventsByAgentId(t *testing.T) {
	store := NewStore()
	router := chi.NewRouter()
	router.Post("/agents-events", store.HandleAgentEvent)
	router.Get("/agents-events", store.HandleGetAgentEvents)

	events := []AgentEvent{
		{AgentID: "cooking-agent", Kind: "request", Text: "Cooking event", Timestamp: FlexTimestamp{time.Now().UTC()}},
		{AgentID: "delivery-agent", Kind: "response", Text: "Delivery event", Timestamp: FlexTimestamp{time.Now().UTC()}},
		{AgentID: "cooking-agent", Kind: "response", Text: "Cooking done", Timestamp: FlexTimestamp{time.Now().UTC()}},
	}
	for _, event := range events {
		body, _ := json.Marshal(event)
		req := httptest.NewRequest(http.MethodPost, "/agents-events", bytes.NewReader(body))
		req.Header.Set("Content-Type", "application/json")
		rec := httptest.NewRecorder()
		router.ServeHTTP(rec, req)
	}

	req := httptest.NewRequest(http.MethodGet, "/agents-events?agentId=cooking-agent", nil)
	rec := httptest.NewRecorder()
	router.ServeHTTP(rec, req)

	if rec.Code != http.StatusOK {
		t.Errorf("expected status 200 OK, got %d", rec.Code)
	}

	var returned []AgentEvent
	json.Unmarshal(rec.Body.Bytes(), &returned)
	if len(returned) != 2 {
		t.Errorf("expected 2 cooking-agent events, got %d", len(returned))
	}
	for _, e := range returned {
		if e.AgentID != "cooking-agent" {
			t.Errorf("expected agentId 'cooking-agent', got '%s'", e.AgentID)
		}
	}
}

// TestGetAgentEventsEmpty verifies that GET /agents-events returns empty array when no events exist.
func TestGetAgentEventsEmpty(t *testing.T) {
	store := NewStore()
	router := chi.NewRouter()
	router.Get("/agents-events", store.HandleGetAgentEvents)

	req := httptest.NewRequest(http.MethodGet, "/agents-events", nil)
	rec := httptest.NewRecorder()
	router.ServeHTTP(rec, req)

	if rec.Code != http.StatusOK {
		t.Errorf("expected status 200 OK, got %d", rec.Code)
	}

	var events []AgentEvent
	json.Unmarshal(rec.Body.Bytes(), &events)
	if len(events) != 0 {
		t.Errorf("expected 0 events, got %d", len(events))
	}
}

// TestMultipleAgentEventsWithDifferentKinds verifies that multiple agent events with different kinds are tracked.
func TestMultipleAgentEventsWithDifferentKinds(t *testing.T) {
	store := NewStore()
	router := chi.NewRouter()
	router.Post("/agents-events", store.HandleAgentEvent)
	router.Get("/agents-events", store.HandleGetAgentEvents)

	events := []AgentEvent{
		{AgentID: "cooking-agent", Kind: "request", Text: "Checking inventory", Timestamp: FlexTimestamp{time.Now().UTC()}},
		{AgentID: "cooking-agent", Kind: "response", Text: "Putting pizza in oven", Timestamp: FlexTimestamp{time.Now().UTC()}},
		{AgentID: "delivery-agent", Kind: "error", Text: "Dispatching rider failed", Timestamp: FlexTimestamp{time.Now().UTC()}},
	}

	for _, event := range events {
		body, _ := json.Marshal(event)
		req := httptest.NewRequest(http.MethodPost, "/agents-events", bytes.NewReader(body))
		req.Header.Set("Content-Type", "application/json")
		rec := httptest.NewRecorder()
		router.ServeHTTP(rec, req)
		if rec.Code != http.StatusOK {
			t.Errorf("expected status 200 OK, got %d", rec.Code)
		}
	}

	getReq := httptest.NewRequest(http.MethodGet, "/agents-events", nil)
	getRec := httptest.NewRecorder()
	router.ServeHTTP(getRec, getReq)

	var returned []AgentEvent
	json.Unmarshal(getRec.Body.Bytes(), &returned)
	if len(returned) != 3 {
		t.Fatalf("expected 3 events, got %d", len(returned))
	}
	if returned[0].Kind != "request" {
		t.Errorf("expected first event kind 'request', got '%s'", returned[0].Kind)
	}
	if returned[1].Kind != "response" {
		t.Errorf("expected second event kind 'response', got '%s'", returned[1].Kind)
	}
	if returned[2].Kind != "error" {
		t.Errorf("expected third event kind 'error', got '%s'", returned[2].Kind)
	}
}

// TestDeleteAgentEvents verifies that DELETE /agents-events clears all agent events.
func TestDeleteAgentEvents(t *testing.T) {
	store := NewStore()
	router := chi.NewRouter()
	router.Post("/agents-events", store.HandleAgentEvent)
	router.Get("/agents-events", store.HandleGetAgentEvents)
	router.Delete("/agents-events", store.HandleDeleteAgentEvents)

	// Add some events
	events := []AgentEvent{
		{AgentID: "cooking-agent", Kind: "request", Text: "Event 1", Timestamp: FlexTimestamp{time.Now().UTC()}},
		{AgentID: "delivery-agent", Kind: "response", Text: "Event 2", Timestamp: FlexTimestamp{time.Now().UTC()}},
	}
	for _, event := range events {
		body, _ := json.Marshal(event)
		req := httptest.NewRequest(http.MethodPost, "/agents-events", bytes.NewReader(body))
		req.Header.Set("Content-Type", "application/json")
		rec := httptest.NewRecorder()
		router.ServeHTTP(rec, req)
	}

	// Delete all events
	delReq := httptest.NewRequest(http.MethodDelete, "/agents-events", nil)
	delRec := httptest.NewRecorder()
	router.ServeHTTP(delRec, delReq)

	if delRec.Code != http.StatusOK {
		t.Errorf("expected status 200 OK, got %d", delRec.Code)
	}

	// Verify events are cleared
	getReq := httptest.NewRequest(http.MethodGet, "/agents-events", nil)
	getRec := httptest.NewRecorder()
	router.ServeHTTP(getRec, getReq)

	var returned []AgentEvent
	json.Unmarshal(getRec.Body.Bytes(), &returned)
	if len(returned) != 0 {
		t.Errorf("expected 0 events after delete, got %d", len(returned))
	}
}

// TestDeliveryEventsAreTracked verifies that delivery progress events are tracked per orderId.
func TestDeliveryEventsAreTracked(t *testing.T) {
	store := NewStore()
	router := chi.NewRouter()
	router.Post("/order", store.HandleCreateOrder)
	router.Post("/events", store.HandleEvent)

	// Create an order
	orderReq := CreateOrderRequest{
		OrderItems: []OrderItem{
			{PizzaType: "Margherita", Quantity: 1},
		},
		OrderData: "Test order",
	}
	orderBody, _ := json.Marshal(orderReq)
	createReq := httptest.NewRequest(http.MethodPost, "/order", bytes.NewReader(orderBody))
	createReq.Header.Set("Content-Type", "application/json")
	createRec := httptest.NewRecorder()
	router.ServeHTTP(createRec, createReq)

	var createdOrder Order
	json.Unmarshal(createRec.Body.Bytes(), &createdOrder)

	// Send a series of delivery events
	deliveryEvents := []OrderEvent{
		{OrderID: createdOrder.OrderID.String(), Status: "delivering 33%", Source: "delivery"},
		{OrderID: createdOrder.OrderID.String(), Status: "delivering 66%", Source: "delivery"},
		{OrderID: createdOrder.OrderID.String(), Status: "delivering 100%", Source: "delivery"},
		{OrderID: createdOrder.OrderID.String(), Status: "DELIVERED", Source: "delivery"},
	}

	for _, event := range deliveryEvents {
		eventBody, _ := json.Marshal(event)
		req := httptest.NewRequest(http.MethodPost, "/events", bytes.NewReader(eventBody))
		req.Header.Set("Content-Type", "application/json")
		rec := httptest.NewRecorder()
		router.ServeHTTP(rec, req)
	}

	// Verify all events are tracked
	trackedEvents := store.GetOrderEvents(createdOrder.OrderID)
	if len(trackedEvents) != 4 {
		t.Errorf("expected 4 tracked delivery events, got %d", len(trackedEvents))
	}

	// Verify all events have correct source
	for _, e := range trackedEvents {
		if e.Source != "delivery" {
			t.Errorf("expected event source 'delivery', got '%s'", e.Source)
		}
	}

	// Verify final status is DELIVERED
	order, _ := store.GetOrder(createdOrder.OrderID)
	if order.OrderStatus != "DELIVERED" {
		t.Errorf("expected final OrderStatus 'DELIVERED', got '%s'", order.OrderStatus)
	}
}
