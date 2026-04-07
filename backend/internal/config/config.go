package config

import (
	"log/slog"
	"os"
	"strconv"
	"strings"
	"time"
)

type Config struct {
	Addr     string
	LogLevel slog.Level
	// HomeCacheTTL is how long identical /home responses are reused (per user+locale).
	HomeCacheTTL time.Duration
	// SectionCacheTTL is TTL for GET /section/{id} responses (per user+section+cursor).
	SectionCacheTTL time.Duration
	// DisableResponseCache disables in-memory caching (useful for local debugging).
	DisableResponseCache bool
}

func FromEnv() Config {
	return Config{
		Addr:                 envString("BFF_ADDR", ":8080"),
		LogLevel:             parseLogLevel(envString("LOG_LEVEL", "info")),
		HomeCacheTTL:         envDurationSeconds("HOME_CACHE_TTL_SEC", 30*time.Second),
		SectionCacheTTL:      envDurationSeconds("SECTION_CACHE_TTL_SEC", 5*time.Minute),
		DisableResponseCache: envBool("BFF_DISABLE_CACHE", false),
	}
}

func envBool(key string, def bool) bool {
	v := strings.TrimSpace(os.Getenv(key))
	if v == "" {
		return def
	}
	switch strings.ToLower(v) {
	case "1", "true", "yes", "on":
		return true
	case "0", "false", "no", "off":
		return false
	default:
		return def
	}
}

func envDurationSeconds(key string, def time.Duration) time.Duration {
	v := strings.TrimSpace(os.Getenv(key))
	if v == "" {
		return def
	}
	sec, err := strconv.Atoi(v)
	if err != nil || sec < 0 {
		return def
	}
	return time.Duration(sec) * time.Second
}

func envString(key, def string) string {
	if v := strings.TrimSpace(os.Getenv(key)); v != "" {
		return v
	}
	return def
}

func parseLogLevel(v string) slog.Level {
	switch strings.ToLower(strings.TrimSpace(v)) {
	case "debug":
		return slog.LevelDebug
	case "warn", "warning":
		return slog.LevelWarn
	case "error":
		return slog.LevelError
	default:
		return slog.LevelInfo
	}
}

