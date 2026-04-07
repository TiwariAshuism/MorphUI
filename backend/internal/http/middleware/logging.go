package middleware

import (
	"log/slog"
	"net/http"
	"time"

	"morphui/backend/internal/observability"
)

type LoggingOptions struct {
	IncludeQuery          bool
	IncludeRequestHeaders bool
	SlowRequestThreshold  time.Duration
}

func Logging(logger *slog.Logger, m observability.Metrics, next http.Handler, opt LoggingOptions) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		start := time.Now()
		sw := &statusWriter{ResponseWriter: w, status: http.StatusOK}

		next.ServeHTTP(sw, r)

		latency := time.Since(start)
		ms := latency.Milliseconds()
		m.RecordHTTP(r.URL.Path, sw.status, ms)

		attrs := []any{
			"method", r.Method,
			"path", r.URL.Path,
			"status", sw.status,
			"latency_ms", ms,
			"request_id", RequestIDFromContext(r.Context()),
		}

		if opt.IncludeQuery && r.URL.RawQuery != "" {
			attrs = append(attrs, "query", r.URL.RawQuery)
		}
		if opt.IncludeRequestHeaders {
			attrs = append(attrs, "user_agent", r.UserAgent())
		}

		level := slog.LevelInfo
		if sw.status >= 500 {
			level = slog.LevelError
		} else if sw.status >= 400 {
			level = slog.LevelWarn
		} else if opt.SlowRequestThreshold > 0 && latency >= opt.SlowRequestThreshold {
			level = slog.LevelWarn
			attrs = append(attrs, "slow", true)
		}

		logger.Log(r.Context(), level, "http_request", attrs...)
	})
}

type statusWriter struct {
	http.ResponseWriter
	status int
}

func (w *statusWriter) WriteHeader(status int) {
	w.status = status
	w.ResponseWriter.WriteHeader(status)
}

