package contract

import (
	"context"
	"encoding/json"
	"os"
	"path/filepath"
	"runtime"
	"testing"
	"time"

	"morphui/backend/internal/clients"
	"morphui/backend/internal/composer"
)

// TestGenerateHomeGolden writes testdata/golden/home_envelope.golden.json when GENERATE_GOLDEN=1.
// Run once after intentional composer changes:
//
//	GENERATE_GOLDEN=1 go test ./internal/contract -run TestGenerateHomeGolden -v
func TestGenerateHomeGolden(t *testing.T) {
	if os.Getenv("GENERATE_GOLDEN") != "1" {
		t.Skip("set GENERATE_GOLDEN=1 to regenerate golden file")
	}

	c := composer.NewComposer(
		clients.NewUserClient(),
		clients.NewContentClient(),
		clients.NewRecoClient(),
	)

	env, err := c.BuildHome(context.Background(), composer.HomeRequest{
		UserID:    "golden_user_1",
		Locale:    "en-US",
		TraceID:   "trace_golden",
		Now:       time.Unix(1700000000, 0),
		HomeTTLMs: 30000,
	})
	if err != nil {
		t.Fatalf("BuildHome: %v", err)
	}

	env.TraceID = ""
	env.ServerTimeMs = 0
	env.TTLMs = 0

	got, err := json.MarshalIndent(env, "", "  ")
	if err != nil {
		t.Fatalf("marshal: %v", err)
	}

	path := goldenPath(t, "home_envelope.golden.json")
	if err := os.MkdirAll(filepath.Dir(path), 0o755); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(path, got, 0o644); err != nil {
		t.Fatal(err)
	}
	t.Logf("wrote %s", path)
}

// TestHomeEnvelopeGoldenSnapshot compares a normalized /home envelope to a committed golden file.
// Volatile fields (trace, server time) are zeroed before comparison.
func TestHomeEnvelopeGoldenSnapshot(t *testing.T) {
	t.Parallel()

	c := composer.NewComposer(
		clients.NewUserClient(),
		clients.NewContentClient(),
		clients.NewRecoClient(),
	)

	env, err := c.BuildHome(context.Background(), composer.HomeRequest{
		UserID:    "golden_user_1",
		Locale:    "en-US",
		TraceID:   "trace_golden",
		Now:       time.Unix(1700000000, 0),
		HomeTTLMs: 30000,
	})
	if err != nil {
		t.Fatalf("BuildHome: %v", err)
	}

	env.TraceID = ""
	env.ServerTimeMs = 0
	env.TTLMs = 0

	got, err := json.MarshalIndent(env, "", "  ")
	if err != nil {
		t.Fatalf("marshal: %v", err)
	}

	path := goldenPath(t, "home_envelope.golden.json")
	want, err := os.ReadFile(path)
	if err != nil {
		t.Fatalf("read golden %s: %v (run with GENERATE_GOLDEN=1 go test -run TestGenerateHomeGolden)", path, err)
	}

	var gotObj, wantObj any
	_ = json.Unmarshal(got, &gotObj)
	_ = json.Unmarshal(want, &wantObj)
	gotNorm, _ := json.Marshal(gotObj)
	wantNorm, _ := json.Marshal(wantObj)
	if string(gotNorm) != string(wantNorm) {
		t.Fatalf("golden mismatch for /home envelope.\nUpdate %s if composer output changed intentionally.\n--- got ---\n%s\n--- want ---\n%s", path, string(got), string(want))
	}
}

func goldenPath(t *testing.T, name string) string {
	t.Helper()
	_, thisFile, _, ok := runtime.Caller(0)
	if !ok {
		t.Fatal("runtime.Caller")
	}
	return filepath.Clean(filepath.Join(filepath.Dir(thisFile), "testdata", "golden", name))
}
