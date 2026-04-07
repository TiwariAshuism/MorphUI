package models

import "time"

// SduiEnvelope is the top-level BFF response for full screens (e.g. GET /home).
// Clients must ignore unknown fields. ui_version + screen (root component) are required for MorphUI Android.
type SduiEnvelope struct {
	SchemaVersion string            `json:"schema_version"` // e.g. "sdui.v1"
	UIVersion     int               `json:"ui_version"`
	PageID        string            `json:"page_id"`
	TTLMs         int64             `json:"ttl_ms"`
	TraceID       string            `json:"trace_id"`
	ServerTimeMs  int64             `json:"server_time_ms"`
	Experiments   map[string]string `json:"experiments,omitempty"`
	FeatureFlags  map[string]bool   `json:"feature_flags,omitempty"`
	Errors        []string          `json:"errors,omitempty"`
	// Screen is the root component tree (MorphUIEngine reads "screen" key).
	Screen *Component `json:"screen,omitempty"`
	// Page is an optional alias for the same tree when using "page" terminology; omit if identical to Screen.
	Page *Component `json:"page,omitempty"`
	// FallbackPage renders when the primary screen fails validation or critical components are unsupported.
	FallbackPage *Component `json:"fallback_page,omitempty"`
	// ClientHints echoes negotiated capabilities (optional).
	ClientHints map[string]any `json:"client_hints,omitempty"`
}

// WithTrace returns a shallow copy with trace and server time set (for cache hits).
func (e *SduiEnvelope) WithTrace(traceID string, now time.Time) *SduiEnvelope {
	if e == nil {
		return nil
	}
	out := *e
	out.TraceID = traceID
	out.ServerTimeMs = now.UnixMilli()
	return &out
}

// StripTraceForCache clears fields that must not be stored across requests.
func (e *SduiEnvelope) StripTraceForCache() {
	if e == nil {
		return
	}
	e.TraceID = ""
	e.ServerTimeMs = 0
}

// ErrorEnvelope is returned on handler failures when a full SduiEnvelope cannot be built.
type ErrorEnvelope struct {
	SchemaVersion string   `json:"schema_version"`
	UIVersion     int      `json:"ui_version"`
	PageID        string   `json:"page_id,omitempty"`
	TraceID       string   `json:"trace_id"`
	Errors        []string `json:"errors"`
	Screen        *Component `json:"screen,omitempty"`
}
