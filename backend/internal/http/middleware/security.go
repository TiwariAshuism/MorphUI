package middleware

import (
	"net/http"
	"regexp"
)

var userIDRe = regexp.MustCompile(`^[a-zA-Z0-9_\-]{1,64}$`)

const defaultMaxBody = 1 << 20       // 1 MiB
const eventsMaxBody = 8 << 20        // 8 MiB for batched analytics
const mylistMaxBody = 256 << 10        // 256 KiB

// Security applies minimal hardening:
// - validates X-User-Id format (drops invalid values)
// - caps request body size per route
func Security(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		uid := r.Header.Get("X-User-Id")
		if uid != "" && !userIDRe.MatchString(uid) {
			r.Header.Del("X-User-Id")
		}

		limit := defaultMaxBody
		switch {
		case r.URL.Path == "/api/events" && r.Method == http.MethodPost:
			limit = eventsMaxBody
		case r.URL.Path == "/api/mylist" && r.Method == http.MethodPost:
			limit = mylistMaxBody
		}
		r.Body = http.MaxBytesReader(w, r.Body, int64(limit))

		next.ServeHTTP(w, r)
	})
}

