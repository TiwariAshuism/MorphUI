package observability

import (
	"fmt"
	"net/http"
	"sort"
	"strings"
	"sync"
)

// InMemoryMetrics is a minimal metrics sink for Phase 8.
// It exposes Prometheus-style text at /metrics.
type InMemoryMetrics struct {
	mu        sync.Mutex
	cacheHit  map[string]uint64
	cacheMiss map[string]uint64
	// httpRequests key: path + "|" + statusClass (2xx,4xx,5xx)
	httpRequests map[string]uint64
	latencySum   map[string]uint64
	latencyCount map[string]uint64
	// latencyBucket key: path + "|" + bucket label
	latencyBucket map[string]uint64
	eventsIngested uint64
}

func NewInMemoryMetrics() *InMemoryMetrics {
	return &InMemoryMetrics{
		cacheHit:      map[string]uint64{},
		cacheMiss:     map[string]uint64{},
		httpRequests:  map[string]uint64{},
		latencySum:    map[string]uint64{},
		latencyCount:  map[string]uint64{},
		latencyBucket: map[string]uint64{},
	}
}

func (m *InMemoryMetrics) IncCacheHit(route string) {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.cacheHit[route]++
}

func (m *InMemoryMetrics) IncCacheMiss(route string) {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.cacheMiss[route]++
}

func (m *InMemoryMetrics) RecordHTTP(path string, statusCode int, latencyMs int64) {
	m.mu.Lock()
	defer m.mu.Unlock()
	class := "2xx"
	if statusCode >= 500 {
		class = "5xx"
	} else if statusCode >= 400 {
		class = "4xx"
	}
	key := path + "|" + class
	m.httpRequests[key]++
	m.latencySum[path] += uint64(latencyMs)
	m.latencyCount[path]++
	bucket := "le_200ms"
	if latencyMs >= 200 {
		bucket = "le_800ms"
	}
	if latencyMs >= 800 {
		bucket = "le_inf"
	}
	m.latencyBucket[path+"|"+bucket]++
}

func (m *InMemoryMetrics) IncEventsIngested(n int) {
	if n <= 0 {
		return
	}
	m.mu.Lock()
	defer m.mu.Unlock()
	m.eventsIngested += uint64(n)
}

func (m *InMemoryMetrics) Handler() http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "text/plain; version=0.0.4; charset=utf-8")

		m.mu.Lock()
		defer m.mu.Unlock()

		_, _ = fmt.Fprintln(w, "# HELP morphui_cache_hit_total Cache hits by route")
		_, _ = fmt.Fprintln(w, "# TYPE morphui_cache_hit_total counter")
		writeLabeledCounter(w, "morphui_cache_hit_total", m.cacheHit)

		_, _ = fmt.Fprintln(w, "# HELP morphui_cache_miss_total Cache misses by route")
		_, _ = fmt.Fprintln(w, "# TYPE morphui_cache_miss_total counter")
		writeLabeledCounter(w, "morphui_cache_miss_total", m.cacheMiss)

		_, _ = fmt.Fprintln(w, "# HELP morphui_http_requests_total HTTP requests by path and status class")
		_, _ = fmt.Fprintln(w, "# TYPE morphui_http_requests_total counter")
		keys := make([]string, 0, len(m.httpRequests))
		for k := range m.httpRequests {
			keys = append(keys, k)
		}
		sort.Strings(keys)
		for _, k := range keys {
			parts := strings.SplitN(k, "|", 2)
			path, class := parts[0], parts[1]
			if len(parts) < 2 {
				path, class = k, "unknown"
			}
			_, _ = fmt.Fprintf(w, `morphui_http_requests_total{path=%q,status_class=%q} %d`+"\n", path, class, m.httpRequests[k])
		}

		_, _ = fmt.Fprintln(w, "# HELP morphui_http_latency_ms_sum Sum of request latency in ms by path")
		_, _ = fmt.Fprintln(w, "# TYPE morphui_http_latency_ms_sum counter")
		writeLabeledCounter(w, "morphui_http_latency_ms_sum", m.latencySum)

		_, _ = fmt.Fprintln(w, "# HELP morphui_http_latency_ms_count Request count for latency average by path")
		_, _ = fmt.Fprintln(w, "# TYPE morphui_http_latency_ms_count counter")
		writeLabeledCounter(w, "morphui_http_latency_ms_count", m.latencyCount)

		_, _ = fmt.Fprintln(w, "# HELP morphui_http_latency_bucket Coarse latency buckets by path")
		_, _ = fmt.Fprintln(w, "# TYPE morphui_http_latency_bucket counter")
		bk := make([]string, 0, len(m.latencyBucket))
		for k := range m.latencyBucket {
			bk = append(bk, k)
		}
		sort.Strings(bk)
		for _, k := range bk {
			parts := strings.SplitN(k, "|", 2)
			path, bucket := parts[0], parts[1]
			if len(parts) < 2 {
				path, bucket = k, "unknown"
			}
			_, _ = fmt.Fprintf(w, `morphui_http_latency_bucket{path=%q,le=%q} %d`+"\n", path, bucket, m.latencyBucket[k])
		}

		_, _ = fmt.Fprintln(w, "# HELP morphui_events_ingested_total Client events accepted")
		_, _ = fmt.Fprintln(w, "# TYPE morphui_events_ingested_total counter")
		_, _ = fmt.Fprintf(w, "morphui_events_ingested_total %d\n", m.eventsIngested)
	}
}

func writeLabeledCounter(w http.ResponseWriter, name string, m map[string]uint64) {
	keys := make([]string, 0, len(m))
	for k := range m {
		keys = append(keys, k)
	}
	sort.Strings(keys)
	for _, k := range keys {
		_, _ = fmt.Fprintf(w, "%s{route=%q} %d\n", name, k, m[k])
	}
}

