package drinksstock

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/go-chi/chi/v5"
)

// TestHandleGetAll tests the GET /drinks-stock endpoint
func TestHandleGetAll(t *testing.T) {
	ds := NewDrinksStock()

	r := chi.NewRouter()
	r.Get("/drinks-stock", ds.HandleGetAll)

	req, err := http.NewRequest("GET", "/drinks-stock", nil)
	if err != nil {
		t.Fatal(err)
	}

	rr := httptest.NewRecorder()
	r.ServeHTTP(rr, req)

	if status := rr.Code; status != http.StatusOK {
		t.Errorf("handler returned wrong status code: got %v want %v", status, http.StatusOK)
	}

	var stock map[string]int
	if err := json.Unmarshal(rr.Body.Bytes(), &stock); err != nil {
		t.Errorf("failed to unmarshal response: %v", err)
	}

	expectedItems := map[string]int{
		"Beer":        10,
		"Coke":        10,
		"DietCoke":    10,
		"OrangeJuice": 10,
	}

	for item, expectedQty := range expectedItems {
		if qty, ok := stock[item]; !ok {
			t.Errorf("expected item %s not found in stock", item)
		} else if qty != expectedQty {
			t.Errorf("expected quantity %d for %s, got %d", expectedQty, item, qty)
		}
	}
}

// TestHandleGetItem tests the GET /drinks-stock/{item} endpoint
func TestHandleGetItem(t *testing.T) {
	ds := NewDrinksStock()

	r := chi.NewRouter()
	r.Get("/drinks-stock/{item}", ds.HandleGetItem)

	req, err := http.NewRequest("GET", "/drinks-stock/Beer", nil)
	if err != nil {
		t.Fatal(err)
	}

	rr := httptest.NewRecorder()
	r.ServeHTTP(rr, req)

	if status := rr.Code; status != http.StatusOK {
		t.Errorf("handler returned wrong status code: got %v want %v", status, http.StatusOK)
	}

	var response ItemResponse
	if err := json.Unmarshal(rr.Body.Bytes(), &response); err != nil {
		t.Errorf("failed to unmarshal response: %v", err)
	}

	if response.Item != "Beer" {
		t.Errorf("expected item Beer, got %s", response.Item)
	}
	if response.Quantity != 10 {
		t.Errorf("expected quantity 10, got %d", response.Quantity)
	}
}

// TestHandleGetItemNotFound tests GET /drinks-stock/{item} for non-existent item
func TestHandleGetItemNotFound(t *testing.T) {
	ds := NewDrinksStock()

	r := chi.NewRouter()
	r.Get("/drinks-stock/{item}", ds.HandleGetItem)

	req, err := http.NewRequest("GET", "/drinks-stock/NonExistent", nil)
	if err != nil {
		t.Fatal(err)
	}

	rr := httptest.NewRecorder()
	r.ServeHTTP(rr, req)

	if status := rr.Code; status != http.StatusNotFound {
		t.Errorf("handler returned wrong status code: got %v want %v", status, http.StatusNotFound)
	}
}

// TestHandleAcquireItem tests POST /drinks-stock/{item} - successfully acquiring an item
func TestHandleAcquireItem(t *testing.T) {
	ds := NewDrinksStock()

	r := chi.NewRouter()
	r.Post("/drinks-stock/{item}", ds.HandleAcquireItem)

	req, err := http.NewRequest("POST", "/drinks-stock/Beer", nil)
	if err != nil {
		t.Fatal(err)
	}

	rr := httptest.NewRecorder()
	r.ServeHTTP(rr, req)

	if status := rr.Code; status != http.StatusOK {
		t.Errorf("handler returned wrong status code: got %v want %v", status, http.StatusOK)
	}

	var response AcquireResponse
	if err := json.Unmarshal(rr.Body.Bytes(), &response); err != nil {
		t.Errorf("failed to unmarshal response: %v", err)
	}

	if response.Status != StatusAcquired {
		t.Errorf("expected status ACQUIRED, got %s", response.Status)
	}
	if response.Item != "Beer" {
		t.Errorf("expected item Beer, got %s", response.Item)
	}
	if response.RemainingQuantity != 9 {
		t.Errorf("expected remaining quantity 9, got %d", response.RemainingQuantity)
	}
}

// TestHandleAcquireItemEmpty tests POST /drinks-stock/{item} when item is empty
func TestHandleAcquireItemEmpty(t *testing.T) {
	ds := NewDrinksStockWithStock(map[string]int{
		"Beer":        0,
		"Coke":        10,
		"DietCoke":    10,
		"OrangeJuice": 10,
	})

	r := chi.NewRouter()
	r.Post("/drinks-stock/{item}", ds.HandleAcquireItem)

	req, err := http.NewRequest("POST", "/drinks-stock/Beer", nil)
	if err != nil {
		t.Fatal(err)
	}

	rr := httptest.NewRecorder()
	r.ServeHTTP(rr, req)

	if status := rr.Code; status != http.StatusOK {
		t.Errorf("handler returned wrong status code: got %v want %v", status, http.StatusOK)
	}

	var response AcquireResponse
	if err := json.Unmarshal(rr.Body.Bytes(), &response); err != nil {
		t.Errorf("failed to unmarshal response: %v", err)
	}

	if response.Status != StatusEmpty {
		t.Errorf("expected status EMPTY, got %s", response.Status)
	}
	if response.Item != "Beer" {
		t.Errorf("expected item Beer, got %s", response.Item)
	}
	if response.RemainingQuantity != 0 {
		t.Errorf("expected remaining quantity 0, got %d", response.RemainingQuantity)
	}
}

// TestHandleAcquireItemNotFound tests POST /drinks-stock/{item} for non-existent item
func TestHandleAcquireItemNotFound(t *testing.T) {
	ds := NewDrinksStock()

	r := chi.NewRouter()
	r.Post("/drinks-stock/{item}", ds.HandleAcquireItem)

	req, err := http.NewRequest("POST", "/drinks-stock/NonExistent", nil)
	if err != nil {
		t.Fatal(err)
	}

	rr := httptest.NewRecorder()
	r.ServeHTTP(rr, req)

	if status := rr.Code; status != http.StatusNotFound {
		t.Errorf("handler returned wrong status code: got %v want %v", status, http.StatusNotFound)
	}
}

// TestHandleAddQuantity tests POST /drinks-stock/{item}/add - adding quantity to an item
func TestHandleAddQuantity(t *testing.T) {
	ds := NewDrinksStock()

	r := chi.NewRouter()
	r.Post("/drinks-stock/{item}/add", ds.HandleAddQuantity)

	body := strings.NewReader(`{"quantity": 5}`)
	req, err := http.NewRequest("POST", "/drinks-stock/Beer/add", body)
	if err != nil {
		t.Fatal(err)
	}
	req.Header.Set("Content-Type", "application/json")

	rr := httptest.NewRecorder()
	r.ServeHTTP(rr, req)

	if status := rr.Code; status != http.StatusOK {
		t.Errorf("handler returned wrong status code: got %v want %v", status, http.StatusOK)
	}

	var response ItemResponse
	if err := json.Unmarshal(rr.Body.Bytes(), &response); err != nil {
		t.Errorf("failed to unmarshal response: %v", err)
	}

	if response.Item != "Beer" {
		t.Errorf("expected item Beer, got %s", response.Item)
	}
	if response.Quantity != 15 {
		t.Errorf("expected quantity 15, got %d", response.Quantity)
	}
}

// TestHandleAddQuantityNotFound tests POST /drinks-stock/{item}/add for non-existent item
func TestHandleAddQuantityNotFound(t *testing.T) {
	ds := NewDrinksStock()

	r := chi.NewRouter()
	r.Post("/drinks-stock/{item}/add", ds.HandleAddQuantity)

	body := strings.NewReader(`{"quantity": 5}`)
	req, err := http.NewRequest("POST", "/drinks-stock/NonExistent/add", body)
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

// TestHandleAddQuantityInvalidBody tests POST /drinks-stock/{item}/add with invalid JSON
func TestHandleAddQuantityInvalidBody(t *testing.T) {
	ds := NewDrinksStock()

	r := chi.NewRouter()
	r.Post("/drinks-stock/{item}/add", ds.HandleAddQuantity)

	body := strings.NewReader(`{invalid json}`)
	req, err := http.NewRequest("POST", "/drinks-stock/Beer/add", body)
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
