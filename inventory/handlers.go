package inventory

import (
	"encoding/json"
	"log/slog"
	"net/http"
	"sync"

	"github.com/go-chi/chi/v5"
)

// Inventory manages pizza ingredient stock levels and provides HTTP handlers.
type Inventory struct {
	mu    sync.RWMutex
	stock map[string]int
}

// NewInventory creates a new Inventory instance with default stock levels.
func NewInventory() *Inventory {
	return &Inventory{
		stock: DefaultInventory(),
	}
}

// NewInventoryWithStock creates a new Inventory instance with custom stock levels.
func NewInventoryWithStock(stock map[string]int) *Inventory {
	return &Inventory{
		stock: stock,
	}
}

// Reset resets the inventory to default stock levels. Used for testing.
func (inv *Inventory) Reset() {
	inv.mu.Lock()
	defer inv.mu.Unlock()
	inv.stock = DefaultInventory()
}

// HandleGetAll handles GET /inventory requests.
// Returns a JSON object with all items and their quantities.
func (inv *Inventory) HandleGetAll(w http.ResponseWriter, r *http.Request) {
	inv.mu.RLock()
	defer inv.mu.RUnlock()

	slog.InfoContext(r.Context(), "getting all inventory items")

	w.Header().Set("Content-Type", "application/json")
	if err := json.NewEncoder(w).Encode(inv.stock); err != nil {
		slog.ErrorContext(r.Context(), "failed to encode inventory", "error", err)
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
}

// HandleGetItem handles GET /inventory/{item} requests.
// Returns the quantity of a specific item, or 404 if not found.
func (inv *Inventory) HandleGetItem(w http.ResponseWriter, r *http.Request) {
	item := chi.URLParam(r, "item")

	inv.mu.RLock()
	qty, ok := inv.stock[item]
	inv.mu.RUnlock()

	if !ok {
		slog.WarnContext(r.Context(), "item not found", "item", item)
		http.Error(w, "Item not found", http.StatusNotFound)
		return
	}

	slog.InfoContext(r.Context(), "getting inventory item", "item", item, "quantity", qty)

	w.Header().Set("Content-Type", "application/json")
	if err := json.NewEncoder(w).Encode(ItemResponse{Item: item, Quantity: qty}); err != nil {
		slog.ErrorContext(r.Context(), "failed to encode item response", "error", err)
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
}

// HandleAcquireItem handles POST /inventory/{item} requests.
// Decreases the item quantity by 1 and returns ACQUIRED or EMPTY status.
func (inv *Inventory) HandleAcquireItem(w http.ResponseWriter, r *http.Request) {
	item := chi.URLParam(r, "item")

	inv.mu.Lock()
	qty, ok := inv.stock[item]
	if !ok {
		inv.mu.Unlock()
		slog.WarnContext(r.Context(), "item not found for acquisition", "item", item)
		http.Error(w, "Item not found", http.StatusNotFound)
		return
	}

	var status string
	if qty == 0 {
		status = StatusEmpty
		slog.WarnContext(r.Context(), "item is empty", "item", item)
	} else {
		inv.stock[item] = qty - 1
		qty = inv.stock[item]
		status = StatusAcquired
		slog.InfoContext(r.Context(), "item acquired", "item", item, "remainingQuantity", qty)
	}
	inv.mu.Unlock()

	w.Header().Set("Content-Type", "application/json")
	if err := json.NewEncoder(w).Encode(AcquireResponse{
		Item:              item,
		Status:            status,
		RemainingQuantity: qty,
	}); err != nil {
		slog.ErrorContext(r.Context(), "failed to encode acquire response", "error", err)
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
}

// AddQuantityRequest represents the request body for adding quantity to an item.
type AddQuantityRequest struct {
	Quantity int `json:"quantity"`
}

// HandleAddQuantity handles POST /inventory/{item}/add requests.
// Increases the item quantity by the specified amount.
func (inv *Inventory) HandleAddQuantity(w http.ResponseWriter, r *http.Request) {
	item := chi.URLParam(r, "item")

	var req AddQuantityRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		slog.WarnContext(r.Context(), "invalid request body", "error", err)
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	inv.mu.Lock()
	qty, ok := inv.stock[item]
	if !ok {
		inv.mu.Unlock()
		slog.WarnContext(r.Context(), "item not found for adding quantity", "item", item)
		http.Error(w, "Item not found", http.StatusNotFound)
		return
	}

	inv.stock[item] = qty + req.Quantity
	newQty := inv.stock[item]
	inv.mu.Unlock()

	slog.InfoContext(r.Context(), "quantity added", "item", item, "added", req.Quantity, "newQuantity", newQty)

	w.Header().Set("Content-Type", "application/json")
	if err := json.NewEncoder(w).Encode(ItemResponse{
		Item:     item,
		Quantity: newQty,
	}); err != nil {
		slog.ErrorContext(r.Context(), "failed to encode add quantity response", "error", err)
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
}
