package oven

import (
	"log/slog"
	"os"
	"strconv"

	flagd "github.com/open-feature/go-sdk-contrib/providers/flagd/pkg"
	"github.com/open-feature/go-sdk/openfeature"
	hooks "github.com/open-feature/go-sdk-contrib/hooks/open-telemetry/pkg"
)

// InitFeatureFlags sets up the OpenFeature provider backed by flagd.
// Reads FLAGD_HOST (default: localhost) and FLAGD_PORT (default: 8013).
func InitFeatureFlags() {
	host := os.Getenv("FLAGD_HOST")
	if host == "" {
		host = "localhost"
	}

	port := uint16(8013)
	if raw := os.Getenv("FLAGD_PORT"); raw != "" {
		if p, err := strconv.ParseUint(raw, 10, 16); err == nil && p > 0 {
			port = uint16(p)
		}
	}

	provider, err := flagd.NewProvider(
		flagd.WithHost(host),
		flagd.WithPort(port),
	)
	if err != nil {
		slog.Error("failed to create OpenFeature flagd provider", "error", err)
		return
	}

	openfeature.AddHooks(hooks.NewTracesHook())

	if err := openfeature.SetProviderAndWait(provider); err != nil {
		slog.Error("failed to initialize OpenFeature provider", "error", err)
		return
	}

	slog.Info("OpenFeature initialized with flagd provider", "host", host, "port", port)
}
