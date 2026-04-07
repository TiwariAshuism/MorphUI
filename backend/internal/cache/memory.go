package cache

import (
	"sync"
	"time"
)

// Memory is a small in-process TTL cache for BFF responses (Phase 3).
// Not suitable for multi-instance production without shared store; swap for Redis later.
type Memory struct {
	mu      sync.Mutex
	entries map[string]entry
}

type entry struct {
	value     any
	expiresAt time.Time
}

func NewMemory() *Memory {
	return &Memory{entries: make(map[string]entry)}
}

// Get returns the cached value and true if present and unexpired.
func (m *Memory) Get(key string) (any, bool) {
	m.mu.Lock()
	defer m.mu.Unlock()
	e, ok := m.entries[key]
	if !ok || time.Now().After(e.expiresAt) {
		if ok {
			delete(m.entries, key)
		}
		return nil, false
	}
	return e.value, true
}

// Set stores value with TTL. Overwrites existing key.
func (m *Memory) Set(key string, value any, ttl time.Duration) {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.entries[key] = entry{
		value:     value,
		expiresAt: time.Now().Add(ttl),
	}
}
