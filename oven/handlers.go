package oven

import (
	"encoding/json"
	"log/slog"
	"math/rand"
	"net/http"
	"sync"
	"time"

	"github.com/go-chi/chi/v5"
)

// OvenServiceConfig holds configuration for the OvenService.
type OvenServiceConfig struct {
	MinReleaseDuration time.Duration
	MaxReleaseDuration time.Duration
}

// DefaultConfig returns the default configuration with 5-20 second release times.
func DefaultConfig() OvenServiceConfig {
	return OvenServiceConfig{
		MinReleaseDuration: 5 * time.Second,
		MaxReleaseDuration: 20 * time.Second,
	}
}

// OvenService manages pizza ovens and provides HTTP handlers.
type OvenService struct {
	mu     sync.RWMutex
	ovens  map[string]*Oven
	config OvenServiceConfig
}

// NewOvenService creates a new OvenService instance with default ovens and config.
func NewOvenService() *OvenService {
	return &OvenService{
		ovens:  DefaultOvens(),
		config: DefaultConfig(),
	}
}

// NewOvenServiceWithConfig creates a new OvenService instance with custom config.
func NewOvenServiceWithConfig(config OvenServiceConfig) *OvenService {
	return &OvenService{
		ovens:  DefaultOvens(),
		config: config,
	}
}

// NewOvenServiceWithOvens creates a new OvenService instance with custom ovens.
func NewOvenServiceWithOvens(ovens map[string]*Oven) *OvenService {
	return &OvenService{
		ovens:  ovens,
		config: DefaultConfig(),
	}
}

// Reset resets the ovens to default state. Used for testing.
func (s *OvenService) Reset() {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.ovens = DefaultOvens()
}

// randomReleaseDuration returns a random duration between min and max release times.
func (s *OvenService) randomReleaseDuration() time.Duration {
	minMs := s.config.MinReleaseDuration.Milliseconds()
	maxMs := s.config.MaxReleaseDuration.Milliseconds()
	randomMs := minMs + rand.Int63n(maxMs-minMs+1)
	return time.Duration(randomMs) * time.Millisecond
}

// scheduleRelease releases an oven after the specified duration.
func (s *OvenService) scheduleRelease(ovenID string, duration time.Duration) {
	time.Sleep(duration)

	s.mu.Lock()
	defer s.mu.Unlock()

	oven, ok := s.ovens[ovenID]
	if !ok {
		return
	}

	if oven.Status == StatusReserved {
		previousUser := oven.User
		oven.Status = StatusAvailable
		oven.User = ""
		oven.UpdatedAt = time.Now()
		slog.Info("oven auto-released", "ovenId", ovenID, "previousUser", previousUser)
	}
}

// HandleGetAll handles GET /ovens/ requests.
// Returns a JSON array of all ovens with their status.
func (s *OvenService) HandleGetAll(w http.ResponseWriter, r *http.Request) {
	s.mu.RLock()
	defer s.mu.RUnlock()

	ovenList := make([]Oven, 0, len(s.ovens))
	for _, oven := range s.ovens {
		ovenList = append(ovenList, *oven)
	}

	slog.Info("getting all ovens", "count", len(ovenList))

	w.Header().Set("Content-Type", "application/json")
	if err := json.NewEncoder(w).Encode(ovenList); err != nil {
		slog.Error("failed to encode ovens", "error", err)
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
}

// HandleGetByID handles GET /ovens/{ovenId} requests.
// Returns the status of a specific oven, or 404 if not found.
func (s *OvenService) HandleGetByID(w http.ResponseWriter, r *http.Request) {
	ovenID := chi.URLParam(r, "ovenId")

	s.mu.RLock()
	oven, ok := s.ovens[ovenID]
	s.mu.RUnlock()

	if !ok {
		slog.Warn("oven not found", "ovenId", ovenID)
		http.Error(w, "Oven not found", http.StatusNotFound)
		return
	}

	slog.Info("getting oven", "ovenId", ovenID, "status", oven.Status)

	w.Header().Set("Content-Type", "application/json")
	if err := json.NewEncoder(w).Encode(oven); err != nil {
		slog.Error("failed to encode oven", "error", err)
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
}

// HandleReserve handles POST /ovens/{ovenId} requests.
// Reserves an oven for a user. Requires 'user' query parameter.
// Returns 409 Conflict if oven is already reserved.
func (s *OvenService) HandleReserve(w http.ResponseWriter, r *http.Request) {
	ovenID := chi.URLParam(r, "ovenId")
	user := r.URL.Query().Get("user")

	if user == "" {
		slog.Warn("missing user parameter", "ovenId", ovenID)
		http.Error(w, "User parameter is required", http.StatusBadRequest)
		return
	}

	s.mu.Lock()
	oven, ok := s.ovens[ovenID]
	if !ok {
		s.mu.Unlock()
		slog.Warn("oven not found for reservation", "ovenId", ovenID)
		http.Error(w, "Oven not found", http.StatusNotFound)
		return
	}

	if oven.Status == StatusReserved {
		s.mu.Unlock()
		slog.Warn("oven already reserved", "ovenId", ovenID, "currentUser", oven.User)
		http.Error(w, "Oven is already reserved", http.StatusConflict)
		return
	}

	oven.Status = StatusReserved
	oven.User = user
	oven.UpdatedAt = time.Now()
	s.mu.Unlock()

	// Schedule auto-release after random duration
	releaseDuration := s.randomReleaseDuration()
	go s.scheduleRelease(ovenID, releaseDuration)

	slog.Info("oven reserved", "ovenId", ovenID, "user", user, "releaseIn", releaseDuration)

	w.Header().Set("Content-Type", "application/json")
	if err := json.NewEncoder(w).Encode(oven); err != nil {
		slog.Error("failed to encode oven", "error", err)
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
}

