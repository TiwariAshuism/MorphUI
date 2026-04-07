package handlers

import (
	"encoding/json"
	"net/http"
	"time"

	"morphui/backend/internal/cache"
	"morphui/backend/internal/composer"
	"morphui/backend/internal/config"
	"morphui/backend/internal/http/middleware"
	"morphui/backend/internal/models"
	"morphui/backend/internal/observability"
	"morphui/backend/internal/schema"
)

func Home(c *composer.Composer, mem *cache.Memory, cfg config.Config, m observability.Metrics) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		userID := r.Header.Get("X-User-Id")
		if userID == "" {
			userID = "guest"
		}

		traceID := middleware.RequestIDFromContext(r.Context())
		locale := r.Header.Get("Accept-Language")

		cacheKey := "home:v1:" + userID + ":" + locale

		if !cfg.DisableResponseCache {
			if v, ok := mem.Get(cacheKey); ok {
				if env, ok := v.(*models.SduiEnvelope); ok {
					m.IncCacheHit("/home")
					writeJSON(w, http.StatusOK, env.WithTrace(traceID, time.Now()))
					return
				}
			}
			m.IncCacheMiss("/home")
		}

		payload, err := c.BuildHome(r.Context(), composer.HomeRequest{
			UserID:    userID,
			Locale:    locale,
			TraceID:   traceID,
			Now:       time.Now(),
			HomeTTLMs: cfg.HomeCacheTTL.Milliseconds(),
		})
		if err != nil {
			writeJSON(w, http.StatusInternalServerError, models.ErrorEnvelope{
				SchemaVersion: schema.SDUIV1,
				UIVersion:     schema.UIVersion,
				PageID:        "home",
				TraceID:       traceID,
				Errors:        []string{err.Error()},
				Screen: &models.Component{
					Type: "column",
					ID:   "error_screen",
					Children: []models.Component{
						{
							Type:  "text",
							ID:    "error_text",
							Props: map[string]any{"value": "Failed to load home"},
						},
					},
				},
			})
			return
		}

		if !cfg.DisableResponseCache {
			toCache := *payload
			toCache.StripTraceForCache()
			mem.Set(cacheKey, &toCache, cfg.HomeCacheTTL)
		}

		writeJSON(w, http.StatusOK, payload)
	}
}

func writeJSON(w http.ResponseWriter, status int, v any) {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.WriteHeader(status)
	enc := json.NewEncoder(w)
	enc.SetEscapeHTML(false)
	_ = enc.Encode(v)
}
