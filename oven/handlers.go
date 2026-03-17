package oven

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

// cookingState tracks when an oven was reserved and for how long.
type cookingState struct {
	reservedAt      time.Time
	releaseDuration time.Duration
}

// OvenService manages pizza ovens and provides HTTP handlers.
type OvenService struct {
	mu           sync.RWMutex
	ovens        map[string]*Oven
	config       OvenServiceConfig
	cookingState map[string]*cookingState
}

// NewOvenService creates a new OvenService instance with default ovens and config.
func NewOvenService() *OvenService {
	return &OvenService{
		ovens:        DefaultOvens(),
		config:       DefaultConfig(),
		cookingState: make(map[string]*cookingState),
	}
}

// NewOvenServiceWithConfig creates a new OvenService instance with custom config.
func NewOvenServiceWithConfig(config OvenServiceConfig) *OvenService {
	return &OvenService{
		ovens:        DefaultOvens(),
		config:       config,
		cookingState: make(map[string]*cookingState),
	}
}

// NewOvenServiceWithOvens creates a new OvenService instance with custom ovens.
func NewOvenServiceWithOvens(ovens map[string]*Oven) *OvenService {
	return &OvenService{
		ovens:        ovens,
		config:       DefaultConfig(),
		cookingState: make(map[string]*cookingState),
	}
}

// Reset resets the ovens to default state. Used for testing.
func (s *OvenService) Reset() {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.ovens = DefaultOvens()
	s.cookingState = make(map[string]*cookingState)
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
		delete(s.cookingState, ovenID)
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
	sort.Slice(ovenList, func(i, j int) bool {
		return ovenList[i].ID < ovenList[j].ID
	})

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
	if !ok {
		s.mu.RUnlock()
		slog.Warn("oven not found", "ovenId", ovenID)
		http.Error(w, "Oven not found", http.StatusNotFound)
		return
	}

	// Compute progress for reserved ovens
	resp := *oven
	if oven.Status == StatusReserved {
		if cs, exists := s.cookingState[ovenID]; exists {
			elapsed := time.Since(cs.reservedAt)
			pct := int(elapsed * 100 / cs.releaseDuration)
			if pct > 99 {
				pct = 99
			}
			resp.Progress = pct
		}
	}
	s.mu.RUnlock()

	slog.Info("getting oven", "ovenId", ovenID, "status", resp.Status, "progress", resp.Progress)

	w.Header().Set("Content-Type", "application/json")
	if err := json.NewEncoder(w).Encode(resp); err != nil {
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

	// Schedule auto-release after random duration
	releaseDuration := s.randomReleaseDuration()
	s.cookingState[ovenID] = &cookingState{
		reservedAt:      time.Now(),
		releaseDuration: releaseDuration,
	}
	s.mu.Unlock()

	go s.scheduleRelease(ovenID, releaseDuration)

	slog.Info("oven reserved", "ovenId", ovenID, "user", user, "releaseIn", releaseDuration)

	w.Header().Set("Content-Type", "application/json")
	if err := json.NewEncoder(w).Encode(oven); err != nil {
		slog.Error("failed to encode oven", "error", err)
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
}

