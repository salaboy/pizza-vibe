package store

import (
	"testing"

	"github.com/google/uuid"
)

func TestMemoryRepo_CreateAndGetOrder(t *testing.T) {
	repo := NewMemoryRepository()

	order := &Order{
		OrderID:     uuid.New(),
		OrderItems:  []OrderItem{{PizzaType: "Margherita", Quantity: 2}},
		OrderData:   "Test",
		OrderStatus: "pending",
	}

	if err := repo.CreateOrder(order); err != nil {
		t.Fatalf("CreateOrder failed: %v", err)
	}

	got, exists := repo.GetOrder(order.OrderID)
	if !exists {
		t.Fatal("expected order to exist")
	}
	if got.OrderID != order.OrderID {
		t.Errorf("expected OrderID %s, got %s", order.OrderID, got.OrderID)
	}
	if got.OrderStatus != "pending" {
		t.Errorf("expected status 'pending', got '%s'", got.OrderStatus)
	}
}

func TestMemoryRepo_GetOrderNotFound(t *testing.T) {
	repo := NewMemoryRepository()

	_, exists := repo.GetOrder(uuid.New())
	if exists {
		t.Error("expected order to not exist")
	}
}

func TestMemoryRepo_GetAllOrders(t *testing.T) {
	repo := NewMemoryRepository()

	order1 := &Order{OrderID: uuid.New(), OrderItems: []OrderItem{{PizzaType: "Margherita", Quantity: 1}}, OrderStatus: "pending"}
	order2 := &Order{OrderID: uuid.New(), OrderItems: []OrderItem{{PizzaType: "Pepperoni", Quantity: 1}}, OrderStatus: "pending"}

	repo.CreateOrder(order1)
	repo.CreateOrder(order2)

	orders, err := repo.GetAllOrders()
	if err != nil {
		t.Fatalf("GetAllOrders failed: %v", err)
	}
	if len(orders) != 2 {
		t.Errorf("expected 2 orders, got %d", len(orders))
	}
}

func TestMemoryRepo_GetAllOrdersEmpty(t *testing.T) {
	repo := NewMemoryRepository()

	orders, err := repo.GetAllOrders()
	if err != nil {
		t.Fatalf("GetAllOrders failed: %v", err)
	}
	if len(orders) != 0 {
		t.Errorf("expected 0 orders, got %d", len(orders))
	}
}

func TestMemoryRepo_UpdateOrderStatus(t *testing.T) {
	repo := NewMemoryRepository()

	order := &Order{OrderID: uuid.New(), OrderItems: []OrderItem{{PizzaType: "Margherita", Quantity: 1}}, OrderStatus: "pending"}
	repo.CreateOrder(order)

	if !repo.UpdateOrderStatus(order.OrderID, "COOKED") {
		t.Error("expected UpdateOrderStatus to return true")
	}

	got, _ := repo.GetOrder(order.OrderID)
	if got.OrderStatus != "COOKED" {
		t.Errorf("expected status 'COOKED', got '%s'", got.OrderStatus)
	}
}

func TestMemoryRepo_UpdateOrderStatusNotFound(t *testing.T) {
	repo := NewMemoryRepository()

	if repo.UpdateOrderStatus(uuid.New(), "COOKED") {
		t.Error("expected UpdateOrderStatus to return false for non-existent order")
	}
}

func TestMemoryRepo_TrackAndGetEvents(t *testing.T) {
	repo := NewMemoryRepository()

	orderID := uuid.New()
	events := []OrderEvent{
		{OrderID: orderID.String(), Status: "cooking", Source: "kitchen"},
		{OrderID: orderID.String(), Status: "in oven", Source: "kitchen"},
		{OrderID: orderID.String(), Status: "COOKED", Source: "kitchen"},
	}

	for _, e := range events {
		if err := repo.TrackEvent(orderID, e); err != nil {
			t.Fatalf("TrackEvent failed: %v", err)
		}
	}

	got, err := repo.GetOrderEvents(orderID)
	if err != nil {
		t.Fatalf("GetOrderEvents failed: %v", err)
	}
	if len(got) != 3 {
		t.Fatalf("expected 3 events, got %d", len(got))
	}
	if got[0].Status != "cooking" {
		t.Errorf("expected first event 'cooking', got '%s'", got[0].Status)
	}
	if got[2].Status != "COOKED" {
		t.Errorf("expected third event 'COOKED', got '%s'", got[2].Status)
	}
}

func TestMemoryRepo_GetOrderEventsEmpty(t *testing.T) {
	repo := NewMemoryRepository()

	got, err := repo.GetOrderEvents(uuid.New())
	if err != nil {
		t.Fatalf("GetOrderEvents failed: %v", err)
	}
	if got != nil {
		t.Errorf("expected nil events for unknown order, got %v", got)
	}
}
