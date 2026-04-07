package models

import (
	"encoding/json"
	"testing"
	"time"

	"morphui/backend/internal/schema"
)

func TestSduiEnvelope_JSONRoundTrip(t *testing.T) {
	env := SduiEnvelope{
		SchemaVersion: schema.SDUIV1,
		UIVersion:     schema.UIVersion,
		PageID:        "home",
		TTLMs:         30000,
		TraceID:       "trace-1",
		ServerTimeMs:  1234567890,
		Experiments:   map[string]string{"a": "b"},
		FeatureFlags:  map[string]bool{"f": true},
		Screen: &Component{
			Type: "column",
			ID:   "root",
			Children: []Component{
				{Type: "text", Props: map[string]any{"value": "hi"}},
			},
		},
		FallbackPage: &Component{Type: "text", Props: map[string]any{"value": "fb"}},
	}
	b, err := json.Marshal(env)
	if err != nil {
		t.Fatal(err)
	}
	var out SduiEnvelope
	if err := json.Unmarshal(b, &out); err != nil {
		t.Fatal(err)
	}
	if out.SchemaVersion != env.SchemaVersion || out.Screen.Type != "column" {
		t.Fatalf("roundtrip mismatch: %+v", out)
	}
}

func TestAction_ToMap(t *testing.T) {
	a := NavigateAction("/x", map[string]string{"id": "1"})
	m := a.ToMap()
	if m["type"] != "Navigate" || m["route"] != "/x" {
		t.Fatalf("unexpected map: %v", m)
	}
}

func TestSduiEnvelope_WithTraceAndStrip(t *testing.T) {
	now := int64(1000)
	env := &SduiEnvelope{
		SchemaVersion: schema.SDUIV1,
		UIVersion:     1,
		PageID:        "home",
		TTLMs:         30000,
		TraceID:       "old",
		ServerTimeMs:  now,
	}
	env.StripTraceForCache()
	if env.TraceID != "" || env.ServerTimeMs != 0 {
		t.Fatalf("strip: %+v", env)
	}
	env.TraceID = "x"
	env.ServerTimeMs = 1
	out := env.WithTrace("new", time.UnixMilli(2000))
	if out.TraceID != "new" || out.ServerTimeMs != 2000 {
		t.Fatalf("with trace: %+v", out)
	}
}
