package contract

import (
	"context"
	"encoding/json"
	"os"
	"path/filepath"
	"runtime"
	"testing"
	"time"

	"github.com/santhosh-tekuri/jsonschema/v5"

	"morphui/backend/internal/clients"
	"morphui/backend/internal/composer"
)

func TestSectionResponseConformsToSchema(t *testing.T) {
	t.Parallel()

	sch := loadSectionSchema(t)

	c := composer.NewComposer(
		clients.NewUserClient(),
		clients.NewContentClient(),
		clients.NewRecoClient(),
	)

	sec, err := c.BuildSection(context.Background(), composer.SectionRequest{
		UserID:       "anon_test_user_123",
		SectionID:  "trending",
		Cursor:       "",
		TraceID:      "trace_section",
		Now:          time.Unix(0, 0),
		SectionTTLMs: 1000,
	})
	if err != nil {
		t.Fatalf("BuildSection failed: %v", err)
	}

	b, err := json.Marshal(sec)
	if err != nil {
		t.Fatalf("marshal failed: %v", err)
	}

	var anyPayload any
	if err := json.Unmarshal(b, &anyPayload); err != nil {
		t.Fatalf("unmarshal to any failed: %v", err)
	}

	if err := sch.Validate(anyPayload); err != nil {
		t.Fatalf("schema validation failed: %v\npayload=%s", err, string(b))
	}
}

func TestHomeEnvelopeConformsToSchema(t *testing.T) {
	t.Parallel()

	sch := loadSchema(t, "")

	c := composer.NewComposer(
		clients.NewUserClient(),
		clients.NewContentClient(),
		clients.NewRecoClient(),
	)

	env, err := c.BuildHome(context.Background(), composer.HomeRequest{
		UserID:    "anon_test_user_123",
		Locale:    "en-US",
		TraceID:   "trace_test",
		Now:       time.Unix(0, 0),
		HomeTTLMs: 1000,
	})
	if err != nil {
		t.Fatalf("BuildHome failed: %v", err)
	}

	b, err := json.Marshal(env)
	if err != nil {
		t.Fatalf("marshal failed: %v", err)
	}

	var anyPayload any
	if err := json.Unmarshal(b, &anyPayload); err != nil {
		t.Fatalf("unmarshal to any failed: %v", err)
	}

	if err := sch.Validate(anyPayload); err != nil {
		t.Fatalf("schema validation failed: %v\npayload=%s", err, string(b))
	}
}

func loadSectionSchema(t *testing.T) *jsonschema.Schema {
	t.Helper()
	_, thisFile, _, ok := runtime.Caller(0)
	if !ok {
		t.Fatalf("runtime.Caller failed")
	}
	base := filepath.Dir(thisFile)
	abs := filepath.Clean(filepath.Join(base, "..", "..", "schema", "sdui.v1.section.schema.json"))
	if _, err := os.Stat(abs); err != nil {
		t.Fatalf("schema file not found at %s: %v", abs, err)
	}
	compiler := jsonschema.NewCompiler()
	if err := compiler.AddResource("sdui.v1.section.schema.json", mustOpen(t, abs)); err != nil {
		t.Fatalf("add resource: %v", err)
	}
	sch, err := compiler.Compile("sdui.v1.section.schema.json")
	if err != nil {
		t.Fatalf("compile schema: %v", err)
	}
	return sch
}

func loadSchema(t *testing.T, rel string) *jsonschema.Schema {
	t.Helper()
	_, thisFile, _, ok := runtime.Caller(0)
	if !ok {
		t.Fatalf("runtime.Caller failed")
	}
	base := filepath.Dir(thisFile)

	abs := filepath.Clean(filepath.Join(base, "..", "..", "schema", "sdui.v1.schema.json"))
	if rel != "" {
		abs = filepath.Clean(filepath.Join(base, "..", "..", rel))
	}
	if _, err := os.Stat(abs); err != nil {
		t.Fatalf("schema file not found at %s: %v", abs, err)
	}

	compiler := jsonschema.NewCompiler()
	if err := compiler.AddResource("sdui.v1.schema.json", mustOpen(t, abs)); err != nil {
		t.Fatalf("add resource: %v", err)
	}
	sch, err := compiler.Compile("sdui.v1.schema.json")
	if err != nil {
		t.Fatalf("compile schema: %v", err)
	}
	return sch
}

func mustOpen(t *testing.T, path string) *os.File {
	t.Helper()
	f, err := os.Open(path)
	if err != nil {
		t.Fatalf("open %s: %v", path, err)
	}
	t.Cleanup(func() { _ = f.Close() })
	return f
}

