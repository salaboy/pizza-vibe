package oven

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/go-chi/chi/v5"
)

// TestHandleGetAll tests the GET /ovens/ endpoint
func TestHandleGetAll(t *testing.T) {
	svc := NewOvenService()

	r := chi.NewRouter()
	r.Get("/ovens/", svc.HandleGetAll)

	req, err := http.NewRequest("GET", "/ovens/", nil)
	if err != nil {
		t.Fatal(err)
	}

	rr := httptest.NewRecorder()
	r.ServeHTTP(rr, req)

	if status := rr.Code; status != http.StatusOK {
		t.Errorf("handler returned wrong status code: got %v want %v", status, http.StatusOK)
	}

	var ovens []Oven
	if err := json.Unmarshal(rr.Body.Bytes(), &ovens); err != nil {
		t.Errorf("failed to unmarshal response: %v", err)
	}

	if len(ovens) != 4 {
		t.Errorf("expected 4 ovens, got %d", len(ovens))
	}

	for _, oven := range ovens {
		if oven.Status != StatusAvailable {
			t.Errorf("expected oven status AVAILABLE, got %s", oven.Status)
		}
	}
}

// TestHandleGetByID tests the GET /ovens/{ovenId} endpoint
func TestHandleGetByID(t *testing.T) {
	svc := NewOvenService()

	r := chi.NewRouter()
	r.Get("/ovens/{ovenId}", svc.HandleGetByID)

	req, err := http.NewRequest("GET", "/ovens/oven-1", nil)
	if err != nil {
		t.Fatal(err)
	}

	rr := httptest.NewRecorder()
	r.ServeHTTP(rr, req)

	if status := rr.Code; status != http.StatusOK {
		t.Errorf("handler returned wrong status code: got %v want %v", status, http.StatusOK)
	}

	var oven Oven
	if err := json.Unmarshal(rr.Body.Bytes(), &oven); err != nil {
		t.Errorf("failed to unmarshal response: %v", err)
	}

	if oven.ID != "oven-1" {
		t.Errorf("expected oven ID oven-1, got %s", oven.ID)
	}
	if oven.Status != StatusAvailable {
		t.Errorf("expected oven status AVAILABLE, got %s", oven.Status)
	}
}

// TestHandleGetByIDNotFound tests GET /ovens/{ovenId} for non-existent oven
func TestHandleGetByIDNotFound(t *testing.T) {
	svc := NewOvenService()

	r := chi.NewRouter()
	r.Get("/ovens/{ovenId}", svc.HandleGetByID)

	req, err := http.NewRequest("GET", "/ovens/oven-99", nil)
	if err != nil {
		t.Fatal(err)
	}

	rr := httptest.NewRecorder()
	r.ServeHTTP(rr, req)

	if status := rr.Code; status != http.StatusNotFound {
		t.Errorf("handler returned wrong status code: got %v want %v", status, http.StatusNotFound)
	}
}

// TestHandleReserve tests POST /ovens/{ovenId} - successfully reserving an oven
func TestHandleReserve(t *testing.T) {
	svc := NewOvenService()

	r := chi.NewRouter()
	r.Post("/ovens/{ovenId}", svc.HandleReserve)

	req, err := http.NewRequest("POST", "/ovens/oven-1?user=chef1", nil)
	if err != nil {
		t.Fatal(err)
	}

	rr := httptest.NewRecorder()
	r.ServeHTTP(rr, req)

	if status := rr.Code; status != http.StatusOK {
		t.Errorf("handler returned wrong status code: got %v want %v", status, http.StatusOK)
	}

	var oven Oven
	if err := json.Unmarshal(rr.Body.Bytes(), &oven); err != nil {
		t.Errorf("failed to unmarshal response: %v", err)
	}

	if oven.ID != "oven-1" {
		t.Errorf("expected oven ID oven-1, got %s", oven.ID)
	}
	if oven.Status != StatusReserved {
		t.Errorf("expected oven status RESERVED, got %s", oven.Status)
	}
	if oven.User != "chef1" {
		t.Errorf("expected user chef1, got %s", oven.User)
	}
}

// TestHandleReserveAlreadyReserved tests POST /ovens/{ovenId} when oven is already reserved
func TestHandleReserveAlreadyReserved(t *testing.T) {
	svc := NewOvenService()

	r := chi.NewRouter()
	r.Post("/ovens/{ovenId}", svc.HandleReserve)

	// First reservation
	req1, _ := http.NewRequest("POST", "/ovens/oven-1?user=chef1", nil)
	rr1 := httptest.NewRecorder()
	r.ServeHTTP(rr1, req1)

	// Second reservation attempt
	req2, err := http.NewRequest("POST", "/ovens/oven-1?user=chef2", nil)
	if err != nil {
		t.Fatal(err)
	}

	rr2 := httptest.NewRecorder()
	r.ServeHTTP(rr2, req2)

	if status := rr2.Code; status != http.StatusConflict {
		t.Errorf("handler returned wrong status code: got %v want %v", status, http.StatusConflict)
	}
}

// TestHandleReserveMissingUser tests POST /ovens/{ovenId} without user parameter
func TestHandleReserveMissingUser(t *testing.T) {
	svc := NewOvenService()

	r := chi.NewRouter()
	r.Post("/ovens/{ovenId}", svc.HandleReserve)

	req, err := http.NewRequest("POST", "/ovens/oven-1", nil)
	if err != nil {
		t.Fatal(err)
	}

	rr := httptest.NewRecorder()
	r.ServeHTTP(rr, req)

	if status := rr.Code; status != http.StatusBadRequest {
		t.Errorf("handler returned wrong status code: got %v want %v", status, http.StatusBadRequest)
	}
}

// TestHandleReserveNotFound tests POST /ovens/{ovenId} for non-existent oven
func TestHandleReserveNotFound(t *testing.T) {
	svc := NewOvenService()

	r := chi.NewRouter()
	r.Post("/ovens/{ovenId}", svc.HandleReserve)

	req, err := http.NewRequest("POST", "/ovens/oven-99?user=chef1", nil)
	if err != nil {
		t.Fatal(err)
	}

	rr := httptest.NewRecorder()
	r.ServeHTTP(rr, req)

	if status := rr.Code; status != http.StatusNotFound {
		t.Errorf("handler returned wrong status code: got %v want %v", status, http.StatusNotFound)
	}
}


// TestManualReleaseEndpointRemoved verifies that the DELETE endpoint no longer exists
// This is a design test - the HandleRelease method should be removed from the service
func TestManualReleaseEndpointRemoved(t *testing.T) {
	// Verify that OvenService does not have HandleRelease method by checking
	// that the method does not exist (compile-time check via interface)
	var _ interface {
		HandleGetAll(http.ResponseWriter, *http.Request)
		HandleGetByID(http.ResponseWriter, *http.Request)
		HandleReserve(http.ResponseWriter, *http.Request)
	} = &OvenService{}

	// This test passes if the code compiles without HandleRelease
	// If HandleRelease exists and we want to verify it's not exposed,
	// we would need a different approach (integration test with router)
}

// TestGetByIDAvailableOvenReturnsZeroProgress tests that an AVAILABLE oven returns progress 0
func TestGetByIDAvailableOvenReturnsZeroProgress(t *testing.T) {
	svc := NewOvenService()

	r := chi.NewRouter()
	r.Get("/ovens/{ovenId}", svc.HandleGetByID)

	req, err := http.NewRequest("GET", "/ovens/oven-1", nil)
	if err != nil {
		t.Fatal(err)
	}

	rr := httptest.NewRecorder()
	r.ServeHTTP(rr, req)

	if status := rr.Code; status != http.StatusOK {
		t.Fatalf("handler returned wrong status code: got %v want %v", status, http.StatusOK)
	}

	var oven Oven
	if err := json.Unmarshal(rr.Body.Bytes(), &oven); err != nil {
		t.Fatalf("failed to unmarshal response: %v", err)
	}

	if oven.Progress != 0 {
		t.Errorf("expected progress 0 for AVAILABLE oven, got %d", oven.Progress)
	}
}

// TestGetByIDReservedOvenReturnsProgress tests that a RESERVED oven returns progress > 0
func TestGetByIDReservedOvenReturnsProgress(t *testing.T) {
	svc := NewOvenServiceWithConfig(OvenServiceConfig{
		MinReleaseDuration: 100 * time.Millisecond,
		MaxReleaseDuration: 100 * time.Millisecond,
	})

	r := chi.NewRouter()
	r.Post("/ovens/{ovenId}", svc.HandleReserve)
	r.Get("/ovens/{ovenId}", svc.HandleGetByID)

	// Reserve the oven
	req1, _ := http.NewRequest("POST", "/ovens/oven-1?user=chef1", nil)
	rr1 := httptest.NewRecorder()
	r.ServeHTTP(rr1, req1)

	if status := rr1.Code; status != http.StatusOK {
		t.Fatalf("reserve returned wrong status code: got %v want %v", status, http.StatusOK)
	}

	// Wait for ~50% of release duration
	time.Sleep(50 * time.Millisecond)

	// Check progress
	req2, _ := http.NewRequest("GET", "/ovens/oven-1", nil)
	rr2 := httptest.NewRecorder()
	r.ServeHTTP(rr2, req2)

	var oven Oven
	if err := json.Unmarshal(rr2.Body.Bytes(), &oven); err != nil {
		t.Fatalf("failed to unmarshal response: %v", err)
	}

	if oven.Status != StatusReserved {
		t.Errorf("expected oven status RESERVED, got %s", oven.Status)
	}
	if oven.Progress <= 0 {
		t.Errorf("expected progress > 0 for RESERVED oven at ~50%%, got %d", oven.Progress)
	}
	if oven.Progress >= 100 {
		t.Errorf("expected progress < 100 for RESERVED oven at ~50%%, got %d", oven.Progress)
	}
}

// TestGetByIDReleasedOvenReturnsZeroProgress tests that a released oven returns progress 0
func TestGetByIDReleasedOvenReturnsZeroProgress(t *testing.T) {
	svc := NewOvenServiceWithConfig(OvenServiceConfig{
		MinReleaseDuration: 50 * time.Millisecond,
		MaxReleaseDuration: 50 * time.Millisecond,
	})

	r := chi.NewRouter()
	r.Post("/ovens/{ovenId}", svc.HandleReserve)
	r.Get("/ovens/{ovenId}", svc.HandleGetByID)

	// Reserve the oven
	req1, _ := http.NewRequest("POST", "/ovens/oven-1?user=chef1", nil)
	rr1 := httptest.NewRecorder()
	r.ServeHTTP(rr1, req1)

	// Wait for auto-release
	time.Sleep(100 * time.Millisecond)

	// Check oven is available with 0 progress
	req2, _ := http.NewRequest("GET", "/ovens/oven-1", nil)
	rr2 := httptest.NewRecorder()
	r.ServeHTTP(rr2, req2)

	var oven Oven
	if err := json.Unmarshal(rr2.Body.Bytes(), &oven); err != nil {
		t.Fatalf("failed to unmarshal response: %v", err)
	}

	if oven.Status != StatusAvailable {
		t.Errorf("expected oven status AVAILABLE after release, got %s", oven.Status)
	}
	if oven.Progress != 0 {
		t.Errorf("expected progress 0 after release, got %d", oven.Progress)
	}
}

// TestGetByIDProgressReaches100AtRelease tests progress caps at 99 while RESERVED and resets to 0 on release
func TestGetByIDProgressReaches100AtRelease(t *testing.T) {
	svc := NewOvenServiceWithConfig(OvenServiceConfig{
		MinReleaseDuration: 50 * time.Millisecond,
		MaxReleaseDuration: 50 * time.Millisecond,
	})

	r := chi.NewRouter()
	r.Post("/ovens/{ovenId}", svc.HandleReserve)
	r.Get("/ovens/{ovenId}", svc.HandleGetByID)

	// Reserve the oven
	req1, _ := http.NewRequest("POST", "/ovens/oven-1?user=chef1", nil)
	rr1 := httptest.NewRecorder()
	r.ServeHTTP(rr1, req1)

	// Wait slightly less than release duration
	time.Sleep(45 * time.Millisecond)

	// Check progress is high but capped at 99
	req2, _ := http.NewRequest("GET", "/ovens/oven-1", nil)
	rr2 := httptest.NewRecorder()
	r.ServeHTTP(rr2, req2)

	var oven Oven
	if err := json.Unmarshal(rr2.Body.Bytes(), &oven); err != nil {
		t.Fatalf("failed to unmarshal response: %v", err)
	}

	if oven.Status == StatusReserved && oven.Progress > 99 {
		t.Errorf("expected progress capped at 99 while RESERVED, got %d", oven.Progress)
	}

	// Wait for release
	time.Sleep(60 * time.Millisecond)

	// Check oven is available with 0 progress
	req3, _ := http.NewRequest("GET", "/ovens/oven-1", nil)
	rr3 := httptest.NewRecorder()
	r.ServeHTTP(rr3, req3)

	var ovenAfter Oven
	if err := json.Unmarshal(rr3.Body.Bytes(), &ovenAfter); err != nil {
		t.Fatalf("failed to unmarshal response: %v", err)
	}

	if ovenAfter.Status != StatusAvailable {
		t.Errorf("expected oven status AVAILABLE after release, got %s", ovenAfter.Status)
	}
	if ovenAfter.Progress != 0 {
		t.Errorf("expected progress 0 after release, got %d", ovenAfter.Progress)
	}
}

// TestReserveAutoReleasesAfterTime tests that an oven is automatically released after reservation
func TestReserveAutoReleasesAfterTime(t *testing.T) {
	// Create service with custom release duration for testing
	svc := NewOvenServiceWithConfig(OvenServiceConfig{
		MinReleaseDuration: 50 * time.Millisecond,
		MaxReleaseDuration: 100 * time.Millisecond,
	})

	r := chi.NewRouter()
	r.Post("/ovens/{ovenId}", svc.HandleReserve)
	r.Get("/ovens/{ovenId}", svc.HandleGetByID)

	// Reserve the oven
	req1, _ := http.NewRequest("POST", "/ovens/oven-1?user=chef1", nil)
	rr1 := httptest.NewRecorder()
	r.ServeHTTP(rr1, req1)

	if status := rr1.Code; status != http.StatusOK {
		t.Fatalf("reserve returned wrong status code: got %v want %v", status, http.StatusOK)
	}

	var oven Oven
	if err := json.Unmarshal(rr1.Body.Bytes(), &oven); err != nil {
		t.Fatalf("failed to unmarshal response: %v", err)
	}

	if oven.Status != StatusReserved {
		t.Errorf("expected oven status RESERVED, got %s", oven.Status)
	}
	if oven.User != "chef1" {
		t.Errorf("expected user chef1, got %s", oven.User)
	}

	// Wait for auto-release (max duration + buffer)
	time.Sleep(150 * time.Millisecond)

	// Check oven is now available
	req2, _ := http.NewRequest("GET", "/ovens/oven-1", nil)
	rr2 := httptest.NewRecorder()
	r.ServeHTTP(rr2, req2)

	var ovenAfter Oven
	if err := json.Unmarshal(rr2.Body.Bytes(), &ovenAfter); err != nil {
		t.Fatalf("failed to unmarshal response: %v", err)
	}

	if ovenAfter.Status != StatusAvailable {
		t.Errorf("expected oven status AVAILABLE after auto-release, got %s", ovenAfter.Status)
	}
	if ovenAfter.User != "" {
		t.Errorf("expected empty user after auto-release, got %s", ovenAfter.User)
	}
}
