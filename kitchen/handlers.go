// Package kitchen provides the kitchen service for the Pizza Vibe application.
// It handles cooking pizza orders by processing order items with simulated cooking times.
package kitchen

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"log/slog"
	"math/rand"
	"net/http"
	"time"

	"github.com/google/uuid"
)

// KitchenConfig contains configuration options for the Kitchen service.
type KitchenConfig struct {
	StoreURL        string
	CookingAgentURL string
	CookingTimeFunc func() int // Returns cooking time in seconds for each item (deprecated)
}

// OrderEvent represents an event sent to the store service.
type OrderEvent struct {
	OrderID uuid.UUID `json:"orderId"`
	Status  string    `json:"status"`
	Source  string    `json:"source"`
}

// Kitchen manages pizza cooking operations and provides HTTP handlers for the kitchen service.
type Kitchen struct {
	rng             *rand.Rand
	storeURL        string
	cookingAgentURL string
	httpClient      *http.Client
	cookingTimeFunc func() int
}

// NewKitchen creates a new Kitchen instance with a seeded random number generator.
func NewKitchen() *Kitchen {
	rng := rand.New(rand.NewSource(time.Now().UnixNano()))
	return &Kitchen{
		rng:             rng,
		storeURL:        "http://store:8080",
		cookingAgentURL: "http://cooking-agent:8087",
		httpClient: &http.Client{
			Timeout: 60 * time.Second,
		},
		cookingTimeFunc: func() int { return rng.Intn(10) + 1 },
	}
}

// NewKitchenWithConfig creates a new Kitchen instance with the given configuration.
func NewKitchenWithConfig(config KitchenConfig) *Kitchen {
	k := NewKitchen()
	if config.StoreURL != "" {
		k.storeURL = config.StoreURL
	}
	if config.CookingAgentURL != "" {
		k.cookingAgentURL = config.CookingAgentURL
	}
	if config.CookingTimeFunc != nil {
		k.cookingTimeFunc = config.CookingTimeFunc
	}
	return k
}

// HandleCook handles POST /cook requests to cook pizza order items.
// It validates the request and starts cooking the items asynchronously.
// Each item takes a random time from 1 to 10 seconds to cook.
func (k *Kitchen) HandleCook(w http.ResponseWriter, r *http.Request) {
	var req CookRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "Invalid JSON", http.StatusBadRequest)
		return
	}

	// Validate that at least one item is provided
	if len(req.OrderItems) == 0 {
		http.Error(w, "Order must contain at least one item", http.StatusBadRequest)
		return
	}

	slog.Info("cook request received", "orderId", req.OrderID, "items", len(req.OrderItems))

	// Start cooking in a goroutine (background; detach from request context)
	go k.cookItems(context.Background(), req.OrderID, req.OrderItems)

	// Return accepted response immediately
	resp := CookResponse{
		OrderID: req.OrderID,
		Status:  "cooking",
		Message: fmt.Sprintf("Started cooking %d item(s)", len(req.OrderItems)),
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusAccepted)
	json.NewEncoder(w).Encode(resp)
}

// cookItems calls the cooking-agent for each pizza in the order.
// It sends an event for each streaming update and a DONE event when all pizzas are cooked.
func (k *Kitchen) cookItems(ctx context.Context, orderID uuid.UUID, items []OrderItem) {
	for _, item := range items {
		for i := 0; i < item.Quantity; i++ {
			select {
			case <-ctx.Done():
				slog.Warn("cooking cancelled", "orderId", orderID, "error", ctx.Err())
				return
			default:
			}

			// Call cooking-agent with streaming to get updates
			updates := make(chan CookingUpdate, 100)
			go func() {
				err := k.callCookingAgentStream(ctx, item.PizzaType, updates)
				if err != nil {
					slog.Error("failed to cook pizza via agent", "orderId", orderID, "pizzaType", item.PizzaType, "error", err)
					k.sendEvent(ctx, orderID, fmt.Sprintf("failed to cook %s: %v", item.PizzaType, err))
				}
			}()

			// Forward each update to the store
			var result string
			for update := range updates {
				// Only forward action events (not partial response tokens)
				if update.Type == "action" && update.Action != "" {
					slog.Info("cooking update", "orderId", orderID, "pizzaType", item.PizzaType, "action", update.Action, "message", update.Message)
					k.sendEvent(ctx, orderID, update.Action)
				}
				if update.Type == "result" {
					result = update.Message
				}
			}

			slog.Info("pizza cooked by agent", "orderId", orderID, "pizzaType", item.PizzaType, "result", result)
			k.sendEvent(ctx, orderID, fmt.Sprintf("cooked %s (%d/%d)", item.PizzaType, i+1, item.Quantity))
		}
	}
	slog.Info("all items cooked", "orderId", orderID)

	// Send DONE event
	k.sendEvent(ctx, orderID, "DONE")
}

// callCookingAgent sends a request to the cooking-agent streaming endpoint
// and returns a channel of CookingUpdate events.
func (k *Kitchen) callCookingAgent(ctx context.Context, pizzaType string) (string, error) {
	updates := make(chan CookingUpdate, 100)

	err := k.callCookingAgentStream(ctx, pizzaType, updates)
	if err != nil {
		return "", err
	}

	// Drain remaining updates (in case they weren't consumed)
	var result string
	for update := range updates {
		if update.Type == "result" {
			result = update.Message
		}
	}
	return result, nil
}

// callCookingAgentStream sends a request to the cooking-agent streaming endpoint
// and sends CookingUpdate events to the provided channel.
func (k *Kitchen) callCookingAgentStream(ctx context.Context, pizzaType string, updates chan<- CookingUpdate) error {
	defer close(updates)

	agentReq := AgentCookRequest{
		Pizzas: []string{pizzaType},
	}

	body, err := json.Marshal(agentReq)
	if err != nil {
		return fmt.Errorf("failed to marshal agent request: %w", err)
	}

	req, err := http.NewRequestWithContext(ctx, http.MethodPost, k.cookingAgentURL+"/cook/stream", bytes.NewReader(body))
	if err != nil {
		return fmt.Errorf("failed to create agent request: %w", err)
	}
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Accept", "text/event-stream")

	resp, err := k.httpClient.Do(req)
	if err != nil {
		return fmt.Errorf("failed to call cooking agent: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return fmt.Errorf("cooking agent returned status %d", resp.StatusCode)
	}

	// Parse SSE stream
	return k.parseSSEStream(resp.Body, updates)
}

// parseSSEStream parses a Server-Sent Events stream and sends updates to the channel.
func (k *Kitchen) parseSSEStream(body interface{ Read([]byte) (int, error) }, updates chan<- CookingUpdate) error {
	buf := make([]byte, 4096)
	var dataBuffer bytes.Buffer

	for {
		n, err := body.Read(buf)
		if n > 0 {
			dataBuffer.Write(buf[:n])

			// Process complete SSE events
			for {
				data := dataBuffer.Bytes()
				idx := bytes.Index(data, []byte("\n\n"))
				if idx == -1 {
					break
				}

				eventData := data[:idx]
				dataBuffer.Next(idx + 2)

				// Parse SSE event
				if bytes.HasPrefix(eventData, []byte("data: ")) {
					jsonData := eventData[6:] // Skip "data: "
					var update CookingUpdate
					if err := json.Unmarshal(jsonData, &update); err != nil {
						slog.Warn("failed to parse SSE event", "error", err, "data", string(jsonData))
						continue
					}
					updates <- update
				}
			}
		}
		if err != nil {
			if err.Error() == "EOF" {
				return nil
			}
			return err
		}
	}
}

// sendEvent sends an event to the store service.
func (k *Kitchen) sendEvent(ctx context.Context, orderID uuid.UUID, status string) {
	event := OrderEvent{
		OrderID: orderID,
		Status:  status,
		Source:  "kitchen",
	}

	body, err := json.Marshal(event)
	if err != nil {
		slog.Error("failed to marshal event", "orderId", orderID, "error", err)
		return
	}

	req, err := http.NewRequestWithContext(ctx, http.MethodPost, k.storeURL+"/events", bytes.NewReader(body))
	if err != nil {
		slog.Error("failed to create event request", "orderId", orderID, "error", err)
		return
	}
	req.Header.Set("Content-Type", "application/json")

	resp, err := k.httpClient.Do(req)
	if err != nil {
		slog.Error("failed to send event to store", "orderId", orderID, "status", status, "error", err)
		return
	}
	defer resp.Body.Close()
}
