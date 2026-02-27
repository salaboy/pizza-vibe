// Package main provides the entry point for the store service.
// The store service exposes REST endpoints for pizza orders and
// a WebSocket endpoint for real-time order updates.
package main

import (
	"context"
	"fmt"
	"log/slog"
	"net/http"
	"net/http/httputil"
	"net/url"
	"os"
	"os/signal"
	"strings"
	"syscall"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/go-chi/chi/v5/middleware"
	"github.com/salaboy/pizza-vibe/store"
)

func main() {
	// Get port from environment variable or default to 8080
	port := os.Getenv("PORT")
	if port == "" {
		port = "8080"
	}

	var s *store.Store
	if dbURL := os.Getenv("DATABASE_URL"); dbURL != "" {
		repo, err := store.NewPostgresRepository(dbURL)
		if err != nil {
			slog.Error("failed to connect to database", "error", err)
			os.Exit(1)
		}
		defer repo.Close()
		s = store.NewStoreWithRepo(repo)
		slog.Info("using PostgreSQL repository")
	} else {
		s = store.NewStore()
		slog.Info("using in-memory repository")
	}
	if agentURL := os.Getenv("STORE_MGMT_AGENT_URL"); agentURL != "" {
		s.SetAgentURL(agentURL)
	}
	r := chi.NewRouter()

	// Middleware
	r.Use(middleware.Logger)
	r.Use(middleware.Recoverer)

	// Backend service URLs
	ovenURL := os.Getenv("OVEN_SERVICE_URL")
	if ovenURL == "" {
		ovenURL = "http://localhost:8085"
	}
	inventoryURL := os.Getenv("INVENTORY_SERVICE_URL")
	if inventoryURL == "" {
		inventoryURL = "http://localhost:8084"
	}
	bikesURL := os.Getenv("BIKES_SERVICE_URL")
	if bikesURL == "" {
		bikesURL = "http://localhost:8088"
	}
	drinksStockURL := os.Getenv("DRINKS_STOCK_SERVICE_URL")
	if drinksStockURL == "" {
		drinksStockURL = "http://localhost:8090"
	}

	// Original store endpoints (used by agents and internal services)
	r.Post("/order", s.HandleCreateOrder)
	r.Get("/orders", s.HandleGetOrders)
	r.Post("/events", s.HandleEvent)
	r.Get("/events", s.HandleGetEvents)

	// WebSocket endpoint
	r.Get("/ws", s.HandleWebSocket)

	// Health check endpoint
	r.Get("/health", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		w.Write([]byte("OK"))
	})

	// API routes for the front-end (replaces Next.js API routes)
	r.Route("/api", func(api chi.Router) {
		// Store's own endpoints
		api.Post("/order", s.HandleCreateOrder)
		api.Get("/orders", s.HandleGetOrders)
		api.Get("/events", s.HandleGetEvents)
		api.Get("/health/store", func(w http.ResponseWriter, r *http.Request) {
			w.Header().Set("Content-Type", "application/json")
			w.WriteHeader(http.StatusOK)
			w.Write([]byte(`"OK"`))
		})

		// Inventory: GET with transform, POST pass-through
		api.Get("/inventory", store.InventoryListHandler(inventoryURL))
		api.Post("/inventory/{item}", proxyTo(inventoryURL, "/inventory"))
		api.Post("/inventory/{item}/add", proxyTo(inventoryURL, "/inventory"))

		// Oven: all pass-through
		api.Get("/oven", proxyTo(ovenURL, "/ovens/"))
		api.Post("/oven/{ovenId}", proxyToOven(ovenURL))
		api.Delete("/oven/{ovenId}", proxyToOven(ovenURL))

		// Bikes: pass-through
		api.Get("/bikes", proxyTo(bikesURL, "/bikes"))

		// Drinks stock: GET with transform, POST pass-through
		api.Get("/drinks-stock", store.DrinksStockListHandler(drinksStockURL))
		api.Post("/drinks-stock/{item}", proxyTo(drinksStockURL, "/drinks-stock"))
		api.Post("/drinks-stock/{item}/add", proxyTo(drinksStockURL, "/drinks-stock"))
	})

	// Static file serving (catch-all for exported Next.js front-end)
	r.NotFound(store.StaticFileHandler("./static"))

	addr := fmt.Sprintf(":%s", port)
	srv := &http.Server{
		Addr:    addr,
		Handler: r,
	}

	// Graceful shutdown: listen for interrupt/terminate signals
	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	go func() {
		slog.Info("store service starting", "addr", addr)
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			slog.Error("server error", "error", err)
			os.Exit(1)
		}
	}()

	<-ctx.Done()
	slog.Info("shutting down store service")

	shutdownCtx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	if err := srv.Shutdown(shutdownCtx); err != nil {
		slog.Error("shutdown error", "error", err)
		os.Exit(1)
	}
	slog.Info("store service stopped")
}

// proxyTo returns a handler that proxies the request to the given backend service.
// It rewrites the path by replacing the /api prefix with the backend basePath.
func proxyTo(targetURL, basePath string) http.HandlerFunc {
	target, _ := url.Parse(targetURL)
	proxy := httputil.NewSingleHostReverseProxy(target)
	return func(w http.ResponseWriter, r *http.Request) {
		// Rewrite path: /api/inventory/{item} → /inventory/{item}
		r.URL.Path = strings.Replace(r.URL.Path, "/api", "", 1)
		r.URL.Host = target.Host
		r.URL.Scheme = target.Scheme
		r.Host = target.Host
		proxy.ServeHTTP(w, r)
	}
}

// proxyToOven returns a handler that proxies oven requests, mapping
// /api/oven/{ovenId} → /ovens/{ovenId} on the target.
func proxyToOven(targetURL string) http.HandlerFunc {
	target, _ := url.Parse(targetURL)
	proxy := httputil.NewSingleHostReverseProxy(target)
	return func(w http.ResponseWriter, r *http.Request) {
		ovenID := chi.URLParam(r, "ovenId")
		r.URL.Path = "/ovens/" + ovenID
		r.URL.Host = target.Host
		r.URL.Scheme = target.Scheme
		r.Host = target.Host
		proxy.ServeHTTP(w, r)
	}
}
