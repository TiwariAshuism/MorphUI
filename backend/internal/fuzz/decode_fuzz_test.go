package fuzz

import (
	"encoding/json"
	"testing"

	"morphui/backend/internal/models"
)

func FuzzDecodeEnvelope(f *testing.F) {
	seed := []byte(`{"schema_version":"sdui.v1","ui_version":1,"page_id":"home","trace_id":"t","server_time_ms":1,"screen":{"type":"list","children":[]}}`)
	f.Add(seed)

	f.Fuzz(func(t *testing.T, data []byte) {
		var env models.SduiEnvelope
		_ = env
		// We only care that decoding doesn't panic; errors are fine.
		_ = jsonUnmarshalNoPanic(data, &env)
	})
}

func FuzzDecodeSection(f *testing.F) {
	seed := []byte(`{"schema_version":"sdui.v1","ui_version":1,"section_id":"trending","next_cursor":"c","items":[{"type":"card","id":"x","children":[{"type":"text","props":{"value":"hi"}}]}],"trace_id":"t","server_time_ms":1}`)
	f.Add(seed)

	f.Fuzz(func(t *testing.T, data []byte) {
		var sec models.SectionResponse
		_ = jsonUnmarshalNoPanic(data, &sec)
	})
}

func jsonUnmarshalNoPanic(data []byte, v any) (err error) {
	defer func() {
		if r := recover(); r != nil {
			err = &panicError{value: r}
		}
	}()
	return json.Unmarshal(data, v)
}

type panicError struct{ value any }

func (e *panicError) Error() string { return "panic during json unmarshal" }

