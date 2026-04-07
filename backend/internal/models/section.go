package models

import "time"

// SectionResponse is the typed payload for GET /section/{id}?cursor=...
type SectionResponse struct {
	SchemaVersion string      `json:"schema_version"`
	UIVersion     int         `json:"ui_version"`
	SectionID     string      `json:"section_id"`
	NextCursor    string      `json:"next_cursor"`
	Items         []Component `json:"items"`
	TraceID       string      `json:"trace_id"`
	ServerTimeMs  int64       `json:"server_time_ms"`
	TTLMs         int64       `json:"ttl_ms,omitempty"`
	Errors        []string    `json:"errors,omitempty"`
}

// WithTrace returns a shallow copy with trace and server time set.
func (s *SectionResponse) WithTrace(traceID string, now time.Time) *SectionResponse {
	if s == nil {
		return nil
	}
	out := *s
	out.TraceID = traceID
	out.ServerTimeMs = now.UnixMilli()
	return &out
}

// StripTraceForCache clears trace-specific fields before storing in a shared cache.
func (s *SectionResponse) StripTraceForCache() {
	if s == nil {
		return
	}
	s.TraceID = ""
	s.ServerTimeMs = 0
}

// SectionErrorResponse is returned on GET /section/{id} failures (HTTP 5xx).
type SectionErrorResponse struct {
	SchemaVersion string `json:"schema_version"`
	TraceID       string `json:"trace_id"`
	Error         string `json:"error"`
}
