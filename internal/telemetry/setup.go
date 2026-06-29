package telemetry

import (
	"context"
	"fmt"
	"log/slog"

	"go.opentelemetry.io/contrib/bridges/otelslog"
	"go.opentelemetry.io/otel"
	"go.opentelemetry.io/otel/exporters/otlp/otlplog/otlploggrpc"
	"go.opentelemetry.io/otel/exporters/otlp/otlptrace/otlptracegrpc"
	"go.opentelemetry.io/otel/propagation"
	sdklog "go.opentelemetry.io/otel/sdk/log"
	sdkresource "go.opentelemetry.io/otel/sdk/resource"
	sdktrace "go.opentelemetry.io/otel/sdk/trace"
	semconv "go.opentelemetry.io/otel/semconv/v1.26.0"
)

// Setup initialises OTLP trace and log exporters, bridges slog to the OTel
// log pipeline so that every slog.InfoContext/WarnContext/ErrorContext call
// carries the trace_id and span_id from the active span in the supplied
// context, and returns a shutdown function that must be called before exit.
//
// The OTLP endpoint is read from OTEL_EXPORTER_OTLP_ENDPOINT (default
// localhost:4317). The service name can be overridden at runtime with the
// standard OTEL_SERVICE_NAME environment variable.
func Setup(ctx context.Context, serviceName string) (func(context.Context) error, error) {
	res, err := sdkresource.Merge(
		sdkresource.Default(),
		sdkresource.NewWithAttributes(
			semconv.SchemaURL,
			semconv.ServiceName(serviceName),
		),
	)
	if err != nil {
		return nil, fmt.Errorf("resource: %w", err)
	}

	traceExp, err := otlptracegrpc.New(ctx)
	if err != nil {
		return nil, fmt.Errorf("trace exporter: %w", err)
	}
	tp := sdktrace.NewTracerProvider(
		sdktrace.WithBatcher(traceExp),
		sdktrace.WithResource(res),
	)
	otel.SetTracerProvider(tp)
	otel.SetTextMapPropagator(propagation.NewCompositeTextMapPropagator(
		propagation.TraceContext{},
		propagation.Baggage{},
	))

	logExp, err := otlploggrpc.New(ctx)
	if err != nil {
		return nil, fmt.Errorf("log exporter: %w", err)
	}
	lp := sdklog.NewLoggerProvider(
		sdklog.WithProcessor(sdklog.NewBatchProcessor(logExp)),
		sdklog.WithResource(res),
	)

	// Bridge slog → OTel: calling slog.InfoContext(ctx, ...) with a context that
	// holds an active span automatically injects trace_id and span_id into the
	// exported log record, enabling backend correlation between logs and traces.
	slog.SetDefault(slog.New(otelslog.NewHandler(serviceName, otelslog.WithLoggerProvider(lp))))

	return func(ctx context.Context) error {
		if err := tp.Shutdown(ctx); err != nil {
			return err
		}
		return lp.Shutdown(ctx)
	}, nil
}
