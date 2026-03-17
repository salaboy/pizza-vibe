package store

import (
	"encoding/json"
	"io"
	"log/slog"
	"net/http"
	"net/http/httputil"
	"net/url"
	"os"
	"path/filepath"
	"sort"
	"strings"
)

// ServiceProxy returns an http.Handler that reverse-proxies requests to targetURL.
// The incoming request path is forwarded as-is to the target.
func ServiceProxy(targetURL string) http.Handler {
	target, err := url.Parse(targetURL)
	if err != nil {
		slog.Error("invalid proxy target URL", "url", targetURL, "error", err)
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			http.Error(w, "proxy configuration error", http.StatusInternalServerError)
		})
	}
	proxy := httputil.NewSingleHostReverseProxy(target)
	return proxy
}

// InventoryListHandler returns a handler for GET /api/inventory that fetches
// from the inventory service and transforms the response from map[string]int
// to []{"item": string, "quantity": int}.
func InventoryListHandler(inventoryURL string) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		req, err := http.NewRequestWithContext(r.Context(), http.MethodGet, inventoryURL+"/inventory", nil)
		if err != nil {
			slog.Error("failed to create inventory request", "error", err)
			http.Error(w, "failed to fetch inventory", http.StatusInternalServerError)
			return
		}
		resp, err := http.DefaultClient.Do(req)
		if err != nil {
			slog.Error("failed to fetch inventory", "error", err)
			http.Error(w, "failed to fetch inventory", http.StatusBadGateway)
			return
		}
		defer resp.Body.Close()

		if resp.StatusCode != http.StatusOK {
			w.WriteHeader(resp.StatusCode)
			io.Copy(w, resp.Body)
			return
		}

		var data map[string]int
		if err := json.NewDecoder(resp.Body).Decode(&data); err != nil {
			slog.Error("failed to decode inventory response", "error", err)
			http.Error(w, "failed to decode inventory", http.StatusInternalServerError)
			return
		}

		type item struct {
			Item     string `json:"item"`
			Quantity int    `json:"quantity"`
		}
		result := make([]item, 0, len(data))
		for k, v := range data {
			result = append(result, item{Item: k, Quantity: v})
		}
		sort.Slice(result, func(i, j int) bool {
			return result[i].Item < result[j].Item
		})

		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(result)
	}
}

// DrinksStockListHandler returns a handler for GET /api/drinks-stock that fetches
// from the drinks-stock service and transforms the response from map[string]int
// to []{"item": string, "quantity": int}.
func DrinksStockListHandler(drinksStockURL string) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		req, err := http.NewRequestWithContext(r.Context(), http.MethodGet, drinksStockURL+"/drinks-stock", nil)
		if err != nil {
			slog.Error("failed to create drinks stock request", "error", err)
			http.Error(w, "failed to fetch drinks stock", http.StatusInternalServerError)
			return
		}
		resp, err := http.DefaultClient.Do(req)
		if err != nil {
			slog.Error("failed to fetch drinks stock", "error", err)
			http.Error(w, "failed to fetch drinks stock", http.StatusBadGateway)
			return
		}
		defer resp.Body.Close()

		if resp.StatusCode != http.StatusOK {
			w.WriteHeader(resp.StatusCode)
			io.Copy(w, resp.Body)
			return
		}

		var data map[string]int
		if err := json.NewDecoder(resp.Body).Decode(&data); err != nil {
			slog.Error("failed to decode drinks stock response", "error", err)
			http.Error(w, "failed to decode drinks stock", http.StatusInternalServerError)
			return
		}

		type item struct {
			Item     string `json:"item"`
			Quantity int    `json:"quantity"`
		}
		result := make([]item, 0, len(data))
		for k, v := range data {
			result = append(result, item{Item: k, Quantity: v})
		}
		sort.Slice(result, func(i, j int) bool {
			return result[i].Item < result[j].Item
		})

		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(result)
	}
}

// StaticFileHandler returns a handler that serves exported Next.js static files
// from the given directory. It supports clean URLs (e.g. /bikes → bikes.html)
// and falls back to index.html for SPA-style routing.
// If the directory does not exist (e.g. local dev mode), it returns a handler
// that passes through to the next handler (404).
func StaticFileHandler(dir string) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		// Check if static dir exists
		if _, err := os.Stat(dir); os.IsNotExist(err) {
			http.NotFound(w, r)
			return
		}

		path := r.URL.Path
		if path == "/" {
			path = "/index.html"
		}

		// Try exact file path
		fullPath := filepath.Join(dir, filepath.Clean(path))
		if _, err := os.Stat(fullPath); err == nil {
			http.ServeFile(w, r, fullPath)
			return
		}

		// Try adding .html extension (clean URLs: /bikes → bikes.html)
		if !strings.Contains(filepath.Base(path), ".") {
			htmlPath := fullPath + ".html"
			if _, err := os.Stat(htmlPath); err == nil {
				http.ServeFile(w, r, htmlPath)
				return
			}
		}

		// SPA fallback: serve index.html for unknown paths
		indexPath := filepath.Join(dir, "index.html")
		if _, err := os.Stat(indexPath); err == nil {
			http.ServeFile(w, r, indexPath)
			return
		}

		http.NotFound(w, r)
	}
}
