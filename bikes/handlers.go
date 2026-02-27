package bikes

import (
	"bytes"
	"encoding/json"
	"fmt"
	"log/slog"
	"math/rand"
	"net/http"
	"sort"
	"sync"
	"time"

	"github.com/go-chi/chi/v5"
)

type BikeService struct {
	mu              sync.RWMutex
	bikes           map[string]*Bike
	eventEmitter    func(event BikeEvent)
	releaseDuration func() time.Duration
	storeURL        string
	httpClient      *http.Client
}

// storeOrderEvent matches the OrderEvent structure expected by the store /events endpoint.
type storeOrderEvent struct {
	OrderID string `json:"orderId"`
	Status  string `json:"status"`
	Source  string `json:"source"`
	Message string `json:"message,omitempty"`
}

func defaultReleaseDuration() time.Duration {
	return time.Duration(10+rand.Intn(11)) * time.Second
}

func NewBikeService() *BikeService {
	return &BikeService{
		bikes:           DefaultBikes(),
		releaseDuration: defaultReleaseDuration,
		storeURL:        "http://store:8080",
		httpClient:      &http.Client{Timeout: 10 * time.Second},
	}
}

func NewBikeServiceWithBikes(bikes map[string]*Bike) *BikeService {
	return &BikeService{
		bikes: bikes,
	}
}

// SetStoreURL sets the store service URL for sending progress events.
func (s *BikeService) SetStoreURL(url string) {
	s.storeURL = url
}

func (s *BikeService) HandleGetAll(w http.ResponseWriter, r *http.Request) {
	s.mu.RLock()
	defer s.mu.RUnlock()

	slog.Info("getting all bikes")

	bikes := make([]Bike, 0, len(s.bikes))
	for _, b := range s.bikes {
		bikes = append(bikes, *b)
	}
	sort.Slice(bikes, func(i, j int) bool {
		return bikes[i].ID < bikes[j].ID
	})

	w.Header().Set("Content-Type", "application/json")
	if err := json.NewEncoder(w).Encode(bikes); err != nil {
		slog.Error("failed to encode bikes", "error", err)
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
}

func (s *BikeService) HandleGetBike(w http.ResponseWriter, r *http.Request) {
	bikeID := chi.URLParam(r, "bikeId")

	s.mu.RLock()
	bike, ok := s.bikes[bikeID]
	s.mu.RUnlock()

	if !ok {
		slog.Warn("bike not found", "bikeId", bikeID)
		http.Error(w, "Bike not found", http.StatusNotFound)
		return
	}

	slog.Info("getting bike", "bikeId", bikeID, "status", bike.Status)

	w.Header().Set("Content-Type", "application/json")
	if err := json.NewEncoder(w).Encode(bike); err != nil {
		slog.Error("failed to encode bike response", "error", err)
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
}

func (s *BikeService) HandleReserveBike(w http.ResponseWriter, r *http.Request) {
	bikeID := chi.URLParam(r, "bikeId")

	var req ReserveRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		slog.Warn("invalid request body", "error", err)
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	if req.User == "" {
		slog.Warn("user is required to reserve a bike")
		http.Error(w, "User is required", http.StatusBadRequest)
		return
	}

	s.mu.Lock()
	bike, ok := s.bikes[bikeID]
	if !ok {
		s.mu.Unlock()
		slog.Warn("bike not found for reservation", "bikeId", bikeID)
		http.Error(w, "Bike not found", http.StatusNotFound)
		return
	}

	if bike.Status != StatusAvailable {
		s.mu.Unlock()
		slog.Warn("bike not available", "bikeId", bikeID, "status", bike.Status)
		http.Error(w, "Bike is not available", http.StatusConflict)
		return
	}

	now := time.Now()
	bike.Status = StatusReserved
	bike.User = req.User
	bike.OrderID = req.OrderID
	bike.UpdatedAt = now
	s.mu.Unlock()

	slog.Info("bike reserved", "bikeId", bikeID, "user", req.User, "orderId", req.OrderID)

	// Start auto-release goroutine
	go s.autoRelease(bikeID, req.User)

	w.Header().Set("Content-Type", "application/json")
	if err := json.NewEncoder(w).Encode(bike); err != nil {
		slog.Error("failed to encode reserve response", "error", err)
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
}

func (s *BikeService) autoRelease(bikeID string, user string) {
	duration := s.releaseDuration()
	slog.Info("auto-release scheduled", "bikeId", bikeID, "duration", duration)

	// Look up the orderId for this bike
	s.mu.RLock()
	orderID := ""
	if bike, ok := s.bikes[bikeID]; ok {
		orderID = bike.OrderID
	}
	s.mu.RUnlock()

	s.sendOrderMessageEvent(orderID, bikeID, "DELIVERING", "preparing to deliver")
	eventInterval := duration / 4
	steps := 4
	for i := 1; i <= steps; i++ {
		time.Sleep(eventInterval)
		progress := (i * 100) / steps // 25, 50, 75, 100

		if s.eventEmitter != nil {
			s.eventEmitter(BikeEvent{
				BikeID:    bikeID,
				Status:    StatusReserved,
				User:      user,
				OrderID:   orderID,
				Progress:  progress,
				Timestamp: time.Now(),
			})
		}
		s.sendProgressToStore(orderID, bikeID, progress)
		slog.Info("bike on route", "bikeId", bikeID, "user", user, "progress", progress)
	}

	s.mu.Lock()
	bike, ok := s.bikes[bikeID]
	if ok && bike.Status == StatusReserved {
		bike.Status = StatusAvailable
		bike.User = ""
		bike.OrderID = ""
		bike.UpdatedAt = time.Now()
		slog.Info("bike auto-released", "bikeId", bikeID)
	}
	s.mu.Unlock()
	s.sendOrderMessageEvent(orderID, bikeID, "DELIVERED", "order delivered")
}

// sendProgressToStore sends a bike progress event to the store service /events endpoint.
func (s *BikeService) sendProgressToStore(orderID, bikeID string, progress int) {
	if s.storeURL == "" {
		return
	}
	event := storeOrderEvent{
		OrderID: orderID,
		Status:  "ON_ROUTE",
		Source:  "bikes",
		Message: fmt.Sprintf("Bike %s delivery progress: %d%%", bikeID, progress),
	}
	body, err := json.Marshal(event)
	if err != nil {
		slog.Error("failed to marshal bike progress", "error", err)
		return
	}
	resp, err := s.httpClient.Post(s.storeURL+"/events", "application/json", bytes.NewReader(body))
	if err != nil {
		slog.Warn("failed to send progress to store", "bikeId", bikeID, "error", err)
		return
	}
	defer resp.Body.Close()
}

// sendOrderDeliveredEvent sends an event to the store when the order is delivered.
func (s *BikeService) sendOrderMessageEvent(orderID, bikeID string, status string, message string) {
	if s.storeURL == "" {
		return
	}
	event := storeOrderEvent{
		OrderID: orderID,
		Status:  status,
		Source:  "bikes",
		Message: fmt.Sprintf("Bike %s %s the order: %s", bikeID, message, orderID),
	}
	body, err := json.Marshal(event)
	if err != nil {
		slog.Error("failed to marshal bike progress", "error", err)
		return
	}
	resp, err := s.httpClient.Post(s.storeURL+"/events", "application/json", bytes.NewReader(body))
	if err != nil {
		slog.Warn("failed to send progress to store", "bikeId", bikeID, "error", err)
		return
	}
	defer resp.Body.Close()
}
