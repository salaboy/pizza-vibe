// Package main is the entry point for the Oven service.
// It sets up the HTTP server with oven management endpoints.
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
	"github.com/salaboy/pizza-vibe/internal/telemetry"
	"github.com/salaboy/pizza-vibe/oven"
	"go.opentelemetry.io/contrib/instrumentation/net/http/otelhttp"
)

func main() {
	port := os.Getenv("PORT")
	if port == "" {
		port = "8085"
	}

	otelShutdown, err := telemetry.Setup(context.Background(), "oven")
	if err != nil {
		slog.Error("failed to set up telemetry", "error", err)
		os.Exit(1)
	}

	oven.InitFeatureFlags()

	svc := oven.NewOvenService()

	r := chi.NewRouter()
	r.Use(middleware.Logger)
	r.Use(middleware.Recoverer)

	r.Get("/ovens/", svc.HandleGetAll)
	r.Get("/ovens/{ovenId}", svc.HandleGetByID)
	r.Post("/ovens/{ovenId}", svc.HandleReserve)

	r.Get("/health", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		w.Write([]byte("OK"))
	})

	addr := fmt.Sprintf(":%s", port)
	srv := &http.Server{
		Addr:    addr,
		Handler: otelhttp.NewHandler(r, "oven"),
	}

	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	go func() {
		slog.Info("oven service starting", "addr", addr)
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			slog.Error("server error", "error", err)
			os.Exit(1)
		}
	}()

	<-ctx.Done()
	slog.Info("shutting down oven service")

	shutdownCtx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	if err := srv.Shutdown(shutdownCtx); err != nil {
		slog.Error("shutdown error", "error", err)
	}
	if err := otelShutdown(shutdownCtx); err != nil {
		slog.Error("telemetry shutdown error", "error", err)
	}
	slog.Info("oven service stopped")
}
