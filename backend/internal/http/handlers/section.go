package handlers

import (
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

func Section(c *composer.Composer, mem *cache.Memory, cfg config.Config, m observability.Metrics) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		id := r.PathValue("id")
		if id == "" {
			writeJSON(w, http.StatusBadRequest, map[string]any{
				"schema_version": schema.SDUIV1,
				"error":          "missing section id",
			})
			return
		}

		userID := r.Header.Get("X-User-Id")
		if userID == "" {
			userID = r.URL.Query().Get("user_id")
		}
		if userID == "" {
			userID = "guest"
		}

		cursor := r.URL.Query().Get("cursor")
		traceID := middleware.RequestIDFromContext(r.Context())

		cacheKey := "section:v1:" + userID + ":" + id + ":" + cursor

		if !cfg.DisableResponseCache {
			if v, ok := mem.Get(cacheKey); ok {
				if sec, ok := v.(*models.SectionResponse); ok {
					m.IncCacheHit("/section")
					writeJSON(w, http.StatusOK, sec.WithTrace(traceID, time.Now()))
					return
				}
			}
			m.IncCacheMiss("/section")
		}

		payload, err := c.BuildSection(r.Context(), composer.SectionRequest{
			UserID:       userID,
			SectionID:    id,
			Cursor:       cursor,
			TraceID:      traceID,
			Now:          time.Now(),
			SectionTTLMs: cfg.SectionCacheTTL.Milliseconds(),
		})
		if err != nil {
			writeJSON(w, http.StatusInternalServerError, models.SectionErrorResponse{
				SchemaVersion: schema.SDUIV1,
				TraceID:       traceID,
				Error:         err.Error(),
			})
			return
		}

		if !cfg.DisableResponseCache {
			toCache := *payload
			toCache.StripTraceForCache()
			mem.Set(cacheKey, &toCache, cfg.SectionCacheTTL)
		}

		writeJSON(w, http.StatusOK, payload)
	}
}
