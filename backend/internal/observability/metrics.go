package observability

// Metrics is a hook for Prometheus/OpenTelemetry (Phase 3 placeholder).
type Metrics interface {
	IncCacheHit(route string)
	IncCacheMiss(route string)
	RecordHTTP(path string, statusCode int, latencyMs int64)
	IncEventsIngested(n int)
}

// Noop implements Metrics with no-op counters.
type Noop struct{}

func (Noop) IncCacheHit(string)           {}
func (Noop) IncCacheMiss(string)          {}
func (Noop) RecordHTTP(string, int, int64) {}
func (Noop) IncEventsIngested(int)        {}
