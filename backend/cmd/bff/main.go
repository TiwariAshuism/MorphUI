package main

import (
	"context"
	"log/slog"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"morphui/backend/internal/config"
	httpx "morphui/backend/internal/http"
)

func main() {
	cfg := config.FromEnv()

	logger := slog.New(slog.NewJSONHandler(os.Stdout, &slog.HandlerOptions{
		Level: cfg.LogLevel,
	}))

	srv := &http.Server{
		Addr:              cfg.Addr,
		Handler:           httpx.NewRouter(logger, cfg),
		ReadHeaderTimeout: 5 * time.Second,
	}

	go func() {
		logger.Info("bff starting", "addr", cfg.Addr)
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			logger.Error("listen failed", "err", err)
			os.Exit(1)
		}
	}()

	stop := make(chan os.Signal, 1)
	signal.Notify(stop, syscall.SIGINT, syscall.SIGTERM)
	<-stop

	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	logger.Info("bff shutting down")
	if err := srv.Shutdown(ctx); err != nil {
		logger.Error("shutdown failed", "err", err)
		os.Exit(1)
	}
	logger.Info("bff stopped")
}

