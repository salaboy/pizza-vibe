// Package main is the entry point for the Drinks Stock service.
// It sets up the HTTP server with drinks stock management endpoints.
package main

import (
	"context"
	"fmt"
	"log/slog"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/go-chi/chi/v5/middleware"
	drinksstock "github.com/salaboy/pizza-vibe/drinks-stock"
)

func main() {
	// Get port from environment variable or default to 8090
	port := os.Getenv("PORT")
	if port == "" {
		port = "8090"
	}

	// Create drinks stock instance
	ds := drinksstock.NewDrinksStock()

	// Set up router with middleware
	r := chi.NewRouter()
	r.Use(middleware.Logger)
	r.Use(middleware.Recoverer)

	// Register routes
	r.Get("/drinks-stock", ds.HandleGetAll)
	r.Get("/drinks-stock/{item}", ds.HandleGetItem)
	r.Post("/drinks-stock/{item}", ds.HandleAcquireItem)
	r.Post("/drinks-stock/{item}/add", ds.HandleAddQuantity)

	// Health check endpoint
	r.Get("/health", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		w.Write([]byte("OK"))
	})

	addr := fmt.Sprintf(":%s", port)
	srv := &http.Server{
		Addr:    addr,
		Handler: r,
	}

	// Graceful shutdown: listen for interrupt/terminate signals
	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	go func() {
		slog.Info("drinks-stock service starting", "addr", addr)
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			slog.Error("server error", "error", err)
			os.Exit(1)
		}
	}()

	<-ctx.Done()
	slog.Info("shutting down drinks-stock service")

	shutdownCtx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	if err := srv.Shutdown(shutdownCtx); err != nil {
		slog.Error("shutdown error", "error", err)
		os.Exit(1)
	}
	slog.Info("drinks-stock service stopped")
}
