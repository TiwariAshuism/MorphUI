package handlers

import (
	"encoding/json"
	"net/http"
	"time"

	"morphui/backend/internal/http/middleware"
	"morphui/backend/internal/schema"
)

// MyListStub accepts POST /api/mylist for SDUI api_call demos (Phase 6).
func MyListStub() http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost {
			http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
			return
		}
		traceID := middleware.RequestIDFromContext(r.Context())
		_ = json.NewDecoder(r.Body).Decode(&struct{}{})

		w.Header().Set("Content-Type", "application/json")
		_ = json.NewEncoder(w).Encode(map[string]any{
			"schema_version": schema.SDUIV1,
			"ok":             true,
			"trace_id":       traceID,
			"server_time_ms": time.Now().UnixMilli(),
		})
	}
}
