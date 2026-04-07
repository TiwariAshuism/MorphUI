package handlers

import (
	"encoding/json"
	"log/slog"
	"net/http"
	"strings"
	"time"
	"unicode/utf8"

	"morphui/backend/internal/http/middleware"
	"morphui/backend/internal/observability"
	"morphui/backend/internal/schema"
)

type ClientEvent struct {
	EventName   string            `json:"event_name"`
	ScreenID    string            `json:"screen_id,omitempty"`
	ComponentID string            `json:"component_id,omitempty"`
	ActionType  string            `json:"action_type,omitempty"`
	Attrs       map[string]string `json:"attrs,omitempty"`
	TsMs        int64             `json:"ts_ms,omitempty"`
}

type EventsIngestRequest struct {
	Events []ClientEvent `json:"events"`
}

const maxEventsPerBatch = 500
const maxEventNameLen = 128
const maxIDLen = 256

// EventsIngest accepts POST /api/events (Phase 7/8).
func EventsIngest(logger *slog.Logger, m observability.Metrics) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost {
			http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
			return
		}

		traceID := middleware.RequestIDFromContext(r.Context())
		userID := r.Header.Get("X-User-Id")
		if userID == "" {
			userID = "guest"
		}

		var req EventsIngestRequest
		if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
			writeJSON(w, http.StatusBadRequest, map[string]any{
				"schema_version": schema.SDUIV1,
				"trace_id":       traceID,
				"error":          "invalid json",
			})
			return
		}

		if len(req.Events) > maxEventsPerBatch {
			writeJSON(w, http.StatusBadRequest, map[string]any{
				"schema_version": schema.SDUIV1,
				"trace_id":       traceID,
				"error":          "too many events",
			})
			return
		}

		now := time.Now()
		for _, ev := range req.Events {
			if !validEvent(&ev) {
				writeJSON(w, http.StatusBadRequest, map[string]any{
					"schema_version": schema.SDUIV1,
					"trace_id":       traceID,
					"error":          "invalid event fields",
				})
				return
			}
			ts := ev.TsMs
			if ts == 0 {
				ts = now.UnixMilli()
			}
			logger.Info("client_event",
				"trace_id", traceID,
				"request_id", traceID,
				"user_id", userID,
				"event_name", ev.EventName,
				"screen_id", ev.ScreenID,
				"component_id", ev.ComponentID,
				"action_type", ev.ActionType,
				"ts_ms", ts,
			)
		}

		m.IncEventsIngested(len(req.Events))

		writeJSON(w, http.StatusOK, map[string]any{
			"schema_version": schema.SDUIV1,
			"ok":             true,
			"trace_id":       traceID,
			"user_id":        userID,
			"count":          len(req.Events),
			"server_time_ms": now.UnixMilli(),
		})
	}
}

func validEvent(ev *ClientEvent) bool {
	if ev == nil {
		return false
	}
	if strings.TrimSpace(ev.EventName) == "" {
		return false
	}
	if utf8.RuneCountInString(ev.EventName) > maxEventNameLen {
		return false
	}
	if utf8.RuneCountInString(ev.ScreenID) > maxIDLen || utf8.RuneCountInString(ev.ComponentID) > maxIDLen {
		return false
	}
	if len(ev.Attrs) > 64 {
		return false
	}
	for k, v := range ev.Attrs {
		if utf8.RuneCountInString(k) > 64 || utf8.RuneCountInString(v) > 512 {
			return false
		}
	}
	return true
}
