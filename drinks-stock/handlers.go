package drinksstock

import (
	"encoding/json"
	"log/slog"
	"net/http"
	"sync"

	"github.com/go-chi/chi/v5"
)

// DrinksStock manages drink stock levels and provides HTTP handlers.
type DrinksStock struct {
	mu    sync.RWMutex
	stock map[string]int
}

// NewDrinksStock creates a new DrinksStock instance with default stock levels.
func NewDrinksStock() *DrinksStock {
	return &DrinksStock{
		stock: DefaultStock(),
	}
}

// NewDrinksStockWithStock creates a new DrinksStock instance with custom stock levels.
func NewDrinksStockWithStock(stock map[string]int) *DrinksStock {
	return &DrinksStock{
		stock: stock,
	}
}

// Reset resets the drinks stock to default levels. Used for testing.
func (ds *DrinksStock) Reset() {
	ds.mu.Lock()
	defer ds.mu.Unlock()
	ds.stock = DefaultStock()
}

// HandleGetAll handles GET /drinks-stock requests.
// Returns a JSON object with all items and their quantities.
func (ds *DrinksStock) HandleGetAll(w http.ResponseWriter, r *http.Request) {
	ds.mu.RLock()
	defer ds.mu.RUnlock()

	slog.Info("getting all drinks stock items")

	w.Header().Set("Content-Type", "application/json")
	if err := json.NewEncoder(w).Encode(ds.stock); err != nil {
		slog.Error("failed to encode drinks stock", "error", err)
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
}

// HandleGetItem handles GET /drinks-stock/{item} requests.
// Returns the quantity of a specific item, or 404 if not found.
func (ds *DrinksStock) HandleGetItem(w http.ResponseWriter, r *http.Request) {
	item := chi.URLParam(r, "item")

	ds.mu.RLock()
	qty, ok := ds.stock[item]
	ds.mu.RUnlock()

	if !ok {
		slog.Warn("item not found", "item", item)
		http.Error(w, "Item not found", http.StatusNotFound)
		return
	}

	slog.Info("getting drinks stock item", "item", item, "quantity", qty)

	w.Header().Set("Content-Type", "application/json")
	if err := json.NewEncoder(w).Encode(ItemResponse{Item: item, Quantity: qty}); err != nil {
		slog.Error("failed to encode item response", "error", err)
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
}

// HandleAcquireItem handles POST /drinks-stock/{item} requests.
// Decreases the item quantity by 1 and returns ACQUIRED or EMPTY status.
func (ds *DrinksStock) HandleAcquireItem(w http.ResponseWriter, r *http.Request) {
	item := chi.URLParam(r, "item")

	ds.mu.Lock()
	qty, ok := ds.stock[item]
	if !ok {
		ds.mu.Unlock()
		slog.Warn("item not found for acquisition", "item", item)
		http.Error(w, "Item not found", http.StatusNotFound)
		return
	}

	var status string
	if qty == 0 {
		status = StatusEmpty
		slog.Info("item is empty", "item", item)
	} else {
		ds.stock[item] = qty - 1
		qty = ds.stock[item]
		status = StatusAcquired
		slog.Info("item acquired", "item", item, "remainingQuantity", qty)
	}
	ds.mu.Unlock()

	w.Header().Set("Content-Type", "application/json")
	if err := json.NewEncoder(w).Encode(AcquireResponse{
		Item:              item,
		Status:            status,
		RemainingQuantity: qty,
	}); err != nil {
		slog.Error("failed to encode acquire response", "error", err)
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
}

// AddQuantityRequest represents the request body for adding quantity to an item.
type AddQuantityRequest struct {
	Quantity int `json:"quantity"`
}

// HandleAddQuantity handles POST /drinks-stock/{item}/add requests.
// Increases the item quantity by the specified amount.
func (ds *DrinksStock) HandleAddQuantity(w http.ResponseWriter, r *http.Request) {
	item := chi.URLParam(r, "item")

	var req AddQuantityRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		slog.Warn("invalid request body", "error", err)
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	ds.mu.Lock()
	qty, ok := ds.stock[item]
	if !ok {
		ds.mu.Unlock()
		slog.Warn("item not found for adding quantity", "item", item)
		http.Error(w, "Item not found", http.StatusNotFound)
		return
	}

	ds.stock[item] = qty + req.Quantity
	newQty := ds.stock[item]
	ds.mu.Unlock()

	slog.Info("quantity added", "item", item, "added", req.Quantity, "newQuantity", newQty)

	w.Header().Set("Content-Type", "application/json")
	if err := json.NewEncoder(w).Encode(ItemResponse{
		Item:     item,
		Quantity: newQty,
	}); err != nil {
		slog.Error("failed to encode add quantity response", "error", err)
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
}
