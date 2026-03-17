package bikes

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"strings"
	"sync"
	"testing"
	"time"

	"github.com/go-chi/chi/v5"
)

// TestHandleGetAll tests the GET /bikes endpoint
func TestHandleGetAll(t *testing.T) {
	svc := NewBikeService()

	r := chi.NewRouter()
	r.Get("/bikes", svc.HandleGetAll)

	req, err := http.NewRequest("GET", "/bikes", nil)
	if err != nil {
		t.Fatal(err)
	}

	rr := httptest.NewRecorder()
	r.ServeHTTP(rr, req)

	if status := rr.Code; status != http.StatusOK {
		t.Errorf("handler returned wrong status code: got %v want %v", status, http.StatusOK)
	}

	var bikes []Bike
	if err := json.Unmarshal(rr.Body.Bytes(), &bikes); err != nil {
		t.Fatalf("failed to unmarshal response: %v", err)
	}

	if len(bikes) != 3 {
		t.Errorf("expected 3 bikes, got %d", len(bikes))
	}

	for _, bike := range bikes {
		if bike.Status != StatusAvailable {
			t.Errorf("expected bike %s status AVAILABLE, got %s", bike.ID, bike.Status)
		}
	}
}

// TestHandleGetBike tests the GET /bikes/{bikeId} endpoint
func TestHandleGetBike(t *testing.T) {
	svc := NewBikeService()

	r := chi.NewRouter()
	r.Get("/bikes/{bikeId}", svc.HandleGetBike)

	req, err := http.NewRequest("GET", "/bikes/bike-1", nil)
	if err != nil {
		t.Fatal(err)
	}

	rr := httptest.NewRecorder()
	r.ServeHTTP(rr, req)

	if status := rr.Code; status != http.StatusOK {
		t.Errorf("handler returned wrong status code: got %v want %v", status, http.StatusOK)
	}

	var bike Bike
	if err := json.Unmarshal(rr.Body.Bytes(), &bike); err != nil {
		t.Fatalf("failed to unmarshal response: %v", err)
	}

	if bike.ID != "bike-1" {
		t.Errorf("expected bike ID bike-1, got %s", bike.ID)
	}
	if bike.Status != StatusAvailable {
		t.Errorf("expected status AVAILABLE, got %s", bike.Status)
	}
}

// TestHandleGetBikeNotFound tests GET /bikes/{bikeId} for non-existent bike
func TestHandleGetBikeNotFound(t *testing.T) {
	svc := NewBikeService()

	r := chi.NewRouter()
	r.Get("/bikes/{bikeId}", svc.HandleGetBike)

	req, err := http.NewRequest("GET", "/bikes/non-existent", nil)
	if err != nil {
		t.Fatal(err)
	}

	rr := httptest.NewRecorder()
	r.ServeHTTP(rr, req)

	if status := rr.Code; status != http.StatusNotFound {
		t.Errorf("handler returned wrong status code: got %v want %v", status, http.StatusNotFound)
	}
}

// TestHandleReserveBike tests POST /bikes/{bikeId} - successfully reserving a bike
func TestHandleReserveBike(t *testing.T) {
	svc := NewBikeService()

	r := chi.NewRouter()
	r.Post("/bikes/{bikeId}", svc.HandleReserveBike)

	body := strings.NewReader(`{"user": "john"}`)
	req, err := http.NewRequest("POST", "/bikes/bike-1", body)
	if err != nil {
		t.Fatal(err)
	}
	req.Header.Set("Content-Type", "application/json")

	rr := httptest.NewRecorder()
	r.ServeHTTP(rr, req)

	if status := rr.Code; status != http.StatusOK {
		t.Errorf("handler returned wrong status code: got %v want %v", status, http.StatusOK)
	}

	var bike Bike
	if err := json.Unmarshal(rr.Body.Bytes(), &bike); err != nil {
		t.Fatalf("failed to unmarshal response: %v", err)
	}

	if bike.ID != "bike-1" {
		t.Errorf("expected bike ID bike-1, got %s", bike.ID)
	}
	if bike.Status != StatusReserved {
		t.Errorf("expected status RESERVED, got %s", bike.Status)
	}
	if bike.User != "john" {
		t.Errorf("expected user john, got %s", bike.User)
	}
}

// TestHandleReserveBikeAlreadyReserved tests POST /bikes/{bikeId} when bike is already reserved
func TestHandleReserveBikeAlreadyReserved(t *testing.T) {
	svc := NewBikeServiceWithBikes(map[string]*Bike{
		"bike-1": {ID: "bike-1", Status: StatusReserved, User: "alice"},
	})

	r := chi.NewRouter()
	r.Post("/bikes/{bikeId}", svc.HandleReserveBike)

	body := strings.NewReader(`{"user": "bob"}`)
	req, err := http.NewRequest("POST", "/bikes/bike-1", body)
	if err != nil {
		t.Fatal(err)
	}
	req.Header.Set("Content-Type", "application/json")

	rr := httptest.NewRecorder()
	r.ServeHTTP(rr, req)

	if status := rr.Code; status != http.StatusConflict {
		t.Errorf("handler returned wrong status code: got %v want %v", status, http.StatusConflict)
	}
}

// TestHandleReserveBikeNotFound tests POST /bikes/{bikeId} for non-existent bike
func TestHandleReserveBikeNotFound(t *testing.T) {
	svc := NewBikeService()

	r := chi.NewRouter()
	r.Post("/bikes/{bikeId}", svc.HandleReserveBike)

	body := strings.NewReader(`{"user": "john"}`)
	req, err := http.NewRequest("POST", "/bikes/non-existent", body)
	if err != nil {
		t.Fatal(err)
	}
	req.Header.Set("Content-Type", "application/json")

	rr := httptest.NewRecorder()
	r.ServeHTTP(rr, req)

	if status := rr.Code; status != http.StatusNotFound {
		t.Errorf("handler returned wrong status code: got %v want %v", status, http.StatusNotFound)
	}
}

// TestHandleReserveBikeNoUser tests POST /bikes/{bikeId} without user parameter
func TestHandleReserveBikeNoUser(t *testing.T) {
	svc := NewBikeService()

	r := chi.NewRouter()
	r.Post("/bikes/{bikeId}", svc.HandleReserveBike)

	body := strings.NewReader(`{}`)
	req, err := http.NewRequest("POST", "/bikes/bike-1", body)
	if err != nil {
		t.Fatal(err)
	}
	req.Header.Set("Content-Type", "application/json")

	rr := httptest.NewRecorder()
	r.ServeHTTP(rr, req)

	if status := rr.Code; status != http.StatusBadRequest {
		t.Errorf("handler returned wrong status code: got %v want %v", status, http.StatusBadRequest)
	}
}

// TestHandleReserveBikeInvalidBody tests POST /bikes/{bikeId} with invalid JSON
func TestHandleReserveBikeInvalidBody(t *testing.T) {
	svc := NewBikeService()

	r := chi.NewRouter()
	r.Post("/bikes/{bikeId}", svc.HandleReserveBike)

	body := strings.NewReader(`{invalid json}`)
	req, err := http.NewRequest("POST", "/bikes/bike-1", body)
	if err != nil {
		t.Fatal(err)
	}
	req.Header.Set("Content-Type", "application/json")

	rr := httptest.NewRecorder()
	r.ServeHTTP(rr, req)

	if status := rr.Code; status != http.StatusBadRequest {
		t.Errorf("handler returned wrong status code: got %v want %v", status, http.StatusBadRequest)
	}
}

// TestReserveBikeUpdatesTimestamp tests that reserving a bike updates the timestamp
func TestReserveBikeUpdatesTimestamp(t *testing.T) {
	svc := NewBikeService()

	r := chi.NewRouter()
	r.Post("/bikes/{bikeId}", svc.HandleReserveBike)

	before := time.Now()

	body := strings.NewReader(`{"user": "john"}`)
	req, err := http.NewRequest("POST", "/bikes/bike-1", body)
	if err != nil {
		t.Fatal(err)
	}
	req.Header.Set("Content-Type", "application/json")

	rr := httptest.NewRecorder()
	r.ServeHTTP(rr, req)

	after := time.Now()

	var bike Bike
	if err := json.Unmarshal(rr.Body.Bytes(), &bike); err != nil {
		t.Fatalf("failed to unmarshal response: %v", err)
	}

	if bike.UpdatedAt.Before(before) || bike.UpdatedAt.After(after) {
		t.Errorf("expected updatedAt between %v and %v, got %v", before, after, bike.UpdatedAt)
	}
}

// TestBikeAutoRelease tests that a reserved bike automatically becomes available after the release duration
func TestBikeAutoRelease(t *testing.T) {
	svc := NewBikeService()
	// Override release duration to 100ms for fast testing
	svc.releaseDuration = func() time.Duration { return 100 * time.Millisecond }

	r := chi.NewRouter()
	r.Post("/bikes/{bikeId}", svc.HandleReserveBike)
	r.Get("/bikes/{bikeId}", svc.HandleGetBike)

	// Reserve the bike
	body := strings.NewReader(`{"user": "john"}`)
	req, err := http.NewRequest("POST", "/bikes/bike-1", body)
	if err != nil {
		t.Fatal(err)
	}
	req.Header.Set("Content-Type", "application/json")

	rr := httptest.NewRecorder()
	r.ServeHTTP(rr, req)

	if rr.Code != http.StatusOK {
		t.Fatalf("reserve returned wrong status: got %v want %v", rr.Code, http.StatusOK)
	}

	// Verify bike is reserved
	req2, _ := http.NewRequest("GET", "/bikes/bike-1", nil)
	rr2 := httptest.NewRecorder()
	r.ServeHTTP(rr2, req2)

	var reservedBike Bike
	json.Unmarshal(rr2.Body.Bytes(), &reservedBike)
	if reservedBike.Status != StatusReserved {
		t.Errorf("expected status RESERVED immediately after reserve, got %s", reservedBike.Status)
	}

	// Wait for auto-release
	time.Sleep(200 * time.Millisecond)

	// Verify bike is now available
	req3, _ := http.NewRequest("GET", "/bikes/bike-1", nil)
	rr3 := httptest.NewRecorder()
	r.ServeHTTP(rr3, req3)

	var releasedBike Bike
	json.Unmarshal(rr3.Body.Bytes(), &releasedBike)
	if releasedBike.Status != StatusAvailable {
		t.Errorf("expected status AVAILABLE after auto-release, got %s", releasedBike.Status)
	}
	if releasedBike.User != "" {
		t.Errorf("expected user to be cleared after auto-release, got %s", releasedBike.User)
	}
}

// TestBikeEmitsEventsWhileReserved tests that events are emitted while the bike is reserved
func TestBikeEmitsEventsWhileReserved(t *testing.T) {
	svc := NewBikeService()
	svc.releaseDuration = func() time.Duration { return 100 * time.Millisecond }

	var events []BikeEvent
	var eventsMu sync.Mutex
	svc.eventEmitter = func(event BikeEvent) {
		eventsMu.Lock()
		events = append(events, event)
		eventsMu.Unlock()
	}

	r := chi.NewRouter()
	r.Post("/bikes/{bikeId}", svc.HandleReserveBike)

	body := strings.NewReader(`{"user": "john"}`)
	req, err := http.NewRequest("POST", "/bikes/bike-1", body)
	if err != nil {
		t.Fatal(err)
	}
	req.Header.Set("Content-Type", "application/json")

	rr := httptest.NewRecorder()
	r.ServeHTTP(rr, req)

	if rr.Code != http.StatusOK {
		t.Fatalf("reserve returned wrong status: got %v want %v", rr.Code, http.StatusOK)
	}

	// Wait for auto-release to complete
	time.Sleep(200 * time.Millisecond)

	eventsMu.Lock()
	eventCount := len(events)
	eventsMu.Unlock()

	if eventCount == 0 {
		t.Error("expected events to be emitted while bike is reserved, got 0")
	}

	// Verify event content
	eventsMu.Lock()
	for _, event := range events {
		if event.BikeID != "bike-1" {
			t.Errorf("expected event bikeId bike-1, got %s", event.BikeID)
		}
		if event.Status != StatusReserved {
			t.Errorf("expected event status RESERVED, got %s", event.Status)
		}
		if event.User != "john" {
			t.Errorf("expected event user john, got %s", event.User)
		}
	}
	eventsMu.Unlock()
}

// TestBikePostsProgressToStoreService tests that autoRelease sends progress events
// to the store service /events endpoint using the OrderEvent structure.
func TestBikePostsProgressToStoreService(t *testing.T) {
	type storeOrderEvent struct {
		OrderID string `json:"orderId"`
		Status  string `json:"status"`
		Source  string `json:"source"`
		Message string `json:"message,omitempty"`
	}

	progressReceived := make(chan storeOrderEvent, 10)
	storeServer := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path == "/events" && r.Method == http.MethodPost {
			var event storeOrderEvent
			json.NewDecoder(r.Body).Decode(&event)
			progressReceived <- event
			w.WriteHeader(http.StatusOK)
		}
	}))
	defer storeServer.Close()

	svc := NewBikeService()
	svc.releaseDuration = func() time.Duration { return 100 * time.Millisecond }
	svc.storeURL = storeServer.URL

	r := chi.NewRouter()
	r.Post("/bikes/{bikeId}", svc.HandleReserveBike)

	body := strings.NewReader(`{"user": "john", "orderId": "order-456"}`)
	req, err := http.NewRequest("POST", "/bikes/bike-1", body)
	if err != nil {
		t.Fatal(err)
	}
	req.Header.Set("Content-Type", "application/json")

	rr := httptest.NewRecorder()
	r.ServeHTTP(rr, req)

	if rr.Code != http.StatusOK {
		t.Fatalf("reserve returned wrong status: got %v want %v", rr.Code, http.StatusOK)
	}

	// Collect progress events until autoRelease completes
	var events []storeOrderEvent
	timeout := time.After(2 * time.Second)
	for {
		select {
		case event := <-progressReceived:
			events = append(events, event)
			if len(events) == 4 {
				goto done
			}
		case <-timeout:
			t.Fatalf("timed out waiting for progress events, got %d", len(events))
		}
	}
done:
	// Verify 4 events with correct structure
	for i, event := range events {
		if event.OrderID != "order-456" {
			t.Errorf("event %d: expected orderId order-456, got %s", i, event.OrderID)
		}
		if event.Status != "ON_ROUTE" {
			t.Errorf("event %d: expected status ON_ROUTE, got %s", i, event.Status)
		}
		if event.Source != "bikes" {
			t.Errorf("event %d: expected source bikes, got %s", i, event.Source)
		}
		if event.Message == "" {
			t.Errorf("event %d: expected non-empty message", i)
		}
	}
}

// TestBikeEmitsProgressPercentage tests that events emitted during autoRelease include
// increasing progress percentages representing delivery completion.
func TestBikeEmitsProgressPercentage(t *testing.T) {
	svc := NewBikeService()
	svc.releaseDuration = func() time.Duration { return 100 * time.Millisecond }

	var events []BikeEvent
	var eventsMu sync.Mutex
	svc.eventEmitter = func(event BikeEvent) {
		eventsMu.Lock()
		events = append(events, event)
		eventsMu.Unlock()
	}

	r := chi.NewRouter()
	r.Post("/bikes/{bikeId}", svc.HandleReserveBike)

	body := strings.NewReader(`{"user": "john", "orderId": "order-123"}`)
	req, err := http.NewRequest("POST", "/bikes/bike-1", body)
	if err != nil {
		t.Fatal(err)
	}
	req.Header.Set("Content-Type", "application/json")

	rr := httptest.NewRecorder()
	r.ServeHTTP(rr, req)

	if rr.Code != http.StatusOK {
		t.Fatalf("reserve returned wrong status: got %v want %v", rr.Code, http.StatusOK)
	}

	// Wait for auto-release to complete
	time.Sleep(200 * time.Millisecond)

	eventsMu.Lock()
	defer eventsMu.Unlock()

	if len(events) != 4 {
		t.Fatalf("expected 4 events, got %d", len(events))
	}

	// Events should have progress 25, 50, 75, 100
	expectedProgress := []int{25, 50, 75, 100}
	for i, event := range events {
		if event.Progress != expectedProgress[i] {
			t.Errorf("event %d: expected progress %d, got %d", i, expectedProgress[i], event.Progress)
		}
		if event.OrderID != "order-123" {
			t.Errorf("event %d: expected orderId order-123, got %s", i, event.OrderID)
		}
	}
}
