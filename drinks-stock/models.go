// Package drinksstock provides the drinks stock service for the Pizza Vibe application.
// It manages drink stock levels and provides REST endpoints for stock operations.
package drinksstock

// ItemResponse represents the response for a single stock item query.
type ItemResponse struct {
	Item     string `json:"item"`
	Quantity int    `json:"quantity"`
}

// AcquireResponse represents the response when acquiring an item from stock.
type AcquireResponse struct {
	Item              string `json:"item"`
	Status            string `json:"status"`
	RemainingQuantity int    `json:"remainingQuantity"`
}

// Status constants for stock acquisition responses.
const (
	StatusAcquired = "ACQUIRED"
	StatusEmpty    = "EMPTY"
)

// DefaultStock returns the default drinks stock levels.
func DefaultStock() map[string]int {
	return map[string]int{
		"Beer":        10,
		"Coke":        10,
		"Water":       10,
		"OrangeJuice": 10,
	}
}
