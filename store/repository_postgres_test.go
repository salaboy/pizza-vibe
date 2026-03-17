//go:build integration

package store

import (
	"os"
	"testing"

	"github.com/google/uuid"
)

func newPostgresRepo(t *testing.T) *PostgresRepository {
	t.Helper()
	dbURL := os.Getenv("DATABASE_URL")
	if dbURL == "" {
		t.Skip("DATABASE_URL not set, skipping integration test")
	}
	repo, err := NewPostgresRepository(dbURL)
	if err != nil {
		t.Fatalf("failed to create PostgresRepository: %v", err)
	}
	t.Cleanup(func() {
		repo.db.Exec("DELETE FROM order_events")
		repo.db.Exec("DELETE FROM orders")
		repo.Close()
	})
	return repo
}

func TestPostgres_CreateAndGetOrder(t *testing.T) {
	repo := newPostgresRepo(t)

	order := &Order{
		OrderID:     uuid.New(),
		OrderItems:  []OrderItem{{PizzaType: "Margherita", Quantity: 2}},
		DrinkItems:  []DrinkItem{{DrinkType: "Cola", Quantity: 1}},
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
	if len(got.OrderItems) != 1 {
		t.Errorf("expected 1 order item, got %d", len(got.OrderItems))
	}
	if len(got.DrinkItems) != 1 {
		t.Errorf("expected 1 drink item, got %d", len(got.DrinkItems))
	}
}

func TestPostgres_GetOrderNotFound(t *testing.T) {
	repo := newPostgresRepo(t)

	_, exists := repo.GetOrder(uuid.New())
	if exists {
		t.Error("expected order to not exist")
	}
}

func TestPostgres_GetAllOrders(t *testing.T) {
	repo := newPostgresRepo(t)

	repo.CreateOrder(&Order{OrderID: uuid.New(), OrderItems: []OrderItem{{PizzaType: "Margherita", Quantity: 1}}, OrderStatus: "pending"})
	repo.CreateOrder(&Order{OrderID: uuid.New(), OrderItems: []OrderItem{{PizzaType: "Pepperoni", Quantity: 1}}, OrderStatus: "pending"})

	orders, err := repo.GetAllOrders()
	if err != nil {
		t.Fatalf("GetAllOrders failed: %v", err)
	}
	if len(orders) != 2 {
		t.Errorf("expected 2 orders, got %d", len(orders))
	}
}

func TestPostgres_GetAllOrdersEmpty(t *testing.T) {
	repo := newPostgresRepo(t)

	orders, err := repo.GetAllOrders()
	if err != nil {
		t.Fatalf("GetAllOrders failed: %v", err)
	}
	if len(orders) != 0 {
		t.Errorf("expected 0 orders, got %d", len(orders))
	}
}

func TestPostgres_UpdateOrderStatus(t *testing.T) {
	repo := newPostgresRepo(t)

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

func TestPostgres_UpdateOrderStatusNotFound(t *testing.T) {
	repo := newPostgresRepo(t)

	if repo.UpdateOrderStatus(uuid.New(), "COOKED") {
		t.Error("expected UpdateOrderStatus to return false for non-existent order")
	}
}

func TestPostgres_TrackAndGetEvents(t *testing.T) {
	repo := newPostgresRepo(t)

	orderID := uuid.New()
	// Must create the order first (FK constraint).
	repo.CreateOrder(&Order{OrderID: orderID, OrderItems: []OrderItem{{PizzaType: "Margherita", Quantity: 1}}, OrderStatus: "pending"})

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
	// Verify OrderID is returned as string UUID
	if got[0].OrderID != orderID.String() {
		t.Errorf("expected OrderID '%s', got '%s'", orderID.String(), got[0].OrderID)
	}
}

func TestPostgres_GetOrderEventsEmpty(t *testing.T) {
	repo := newPostgresRepo(t)

	orderID := uuid.New()
	repo.CreateOrder(&Order{OrderID: orderID, OrderItems: []OrderItem{{PizzaType: "Margherita", Quantity: 1}}, OrderStatus: "pending"})

	got, err := repo.GetOrderEvents(orderID)
	if err != nil {
		t.Fatalf("GetOrderEvents failed: %v", err)
	}
	if got != nil {
		t.Errorf("expected nil events for order without events, got %v", got)
	}
}
