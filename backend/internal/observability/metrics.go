package observability

// Metrics is a hook for Prometheus/OpenTelemetry (Phase 3 placeholder).
type Metrics interface {
	IncCacheHit(route string)
	IncCacheMiss(route string)
}

// Noop implements Metrics with no-op counters.
type Noop struct{}

func (Noop) IncCacheHit(string)  {}
func (Noop) IncCacheMiss(string) {}
