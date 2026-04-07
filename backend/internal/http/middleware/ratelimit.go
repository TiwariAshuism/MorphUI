package middleware

import (
	"net/http"
	"strings"
	"sync"
	"time"
)

// RateLimitPerIP limits requests per client IP (best-effort; behind proxies use X-Forwarded-For in Phase 8+).
func RateLimitPerIP(maxPerMinute int, next http.Handler) http.Handler {
	if maxPerMinute <= 0 {
		maxPerMinute = 120
	}
	var mu sync.Mutex
	hits := map[string][]time.Time{}

	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if !allow(&mu, &hits, r, maxPerMinute) {
			http.Error(w, "rate limit exceeded", http.StatusTooManyRequests)
			return
		}
		next.ServeHTTP(w, r)
	})
}

// RateLimitPathPrefix applies a per-IP limit only when the request path has the given prefix.
func RateLimitPathPrefix(pathPrefix string, maxPerMinute int, next http.Handler) http.Handler {
	if maxPerMinute <= 0 {
		maxPerMinute = 300
	}
	var mu sync.Mutex
	hits := map[string][]time.Time{}

	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if !strings.HasPrefix(r.URL.Path, pathPrefix) {
			next.ServeHTTP(w, r)
			return
		}
		if !allow(&mu, &hits, r, maxPerMinute) {
			http.Error(w, "rate limit exceeded", http.StatusTooManyRequests)
			return
		}
		next.ServeHTTP(w, r)
	})
}

func allow(mu *sync.Mutex, hits *map[string][]time.Time, r *http.Request, maxPerMinute int) bool {
	ip := r.RemoteAddr
	if xf := r.Header.Get("X-Forwarded-For"); xf != "" {
		ip = xf
	}
	now := time.Now()
	cutoff := now.Add(-time.Minute)

	mu.Lock()
	defer mu.Unlock()
	list := (*hits)[ip]
	var kept []time.Time
	for _, t := range list {
		if t.After(cutoff) {
			kept = append(kept, t)
		}
	}
	if len(kept) >= maxPerMinute {
		return false
	}
	kept = append(kept, now)
	(*hits)[ip] = kept
	return true
}
