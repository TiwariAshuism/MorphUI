package experiments

import (
	"hash/fnv"
	"sort"
)

// Catalog defines available experiments and their variants.
// Keep this small and explicit; Phase 8 can load from config.
var Catalog = map[string][]string{
	"home_rail_order": {"variant_a", "variant_b"},
	"hero_variant":    {"control", "alt_copy"},
}

// Assign returns a stable assignment map for a given userID.
// For guest/empty users, it returns deterministic defaults.
func Assign(userID string) map[string]string {
	out := make(map[string]string, len(Catalog)+1)
	out["schema"] = "sdui.v1"

	// Stable iteration for reproducible output.
	names := make([]string, 0, len(Catalog))
	for k := range Catalog {
		names = append(names, k)
	}
	sort.Strings(names)

	for _, name := range names {
		vars := Catalog[name]
		if len(vars) == 0 {
			continue
		}
		out[name] = pickVariant(userID, name, vars)
	}
	return out
}

func pickVariant(userID, experiment string, variants []string) string {
	// Empty/guest still needs to be stable.
	key := userID
	if key == "" || key == "guest" {
		key = "guest"
	}

	h := fnv.New32a()
	_, _ = h.Write([]byte(key))
	_, _ = h.Write([]byte{0})
	_, _ = h.Write([]byte(experiment))

	idx := int(h.Sum32()) % len(variants)
	return variants[idx]
}

