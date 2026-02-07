package bikes

import (
	"encoding/json"
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
}

func defaultReleaseDuration() time.Duration {
	return time.Duration(10+rand.Intn(11)) * time.Second
}

func NewBikeService() *BikeService {
	return &BikeService{
		bikes:           DefaultBikes(),
		releaseDuration: defaultReleaseDuration,
	}
}

func NewBikeServiceWithBikes(bikes map[string]*Bike) *BikeService {
	return &BikeService{
		bikes: bikes,
	}
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
	bike.UpdatedAt = now
	s.mu.Unlock()

	slog.Info("bike reserved", "bikeId", bikeID, "user", req.User)

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

	eventInterval := duration / 4
	elapsed := time.Duration(0)

	for elapsed < duration {
		time.Sleep(eventInterval)
		elapsed += eventInterval

		if s.eventEmitter != nil {
			s.eventEmitter(BikeEvent{
				BikeID:    bikeID,
				Status:    StatusReserved,
				User:      user,
				Timestamp: time.Now(),
			})
		}
		slog.Info("bike on route", "bikeId", bikeID, "user", user)
	}

	s.mu.Lock()
	bike, ok := s.bikes[bikeID]
	if ok && bike.Status == StatusReserved {
		bike.Status = StatusAvailable
		bike.User = ""
		bike.UpdatedAt = time.Now()
		slog.Info("bike auto-released", "bikeId", bikeID)
	}
	s.mu.Unlock()
}
