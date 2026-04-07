package composer

import "morphui/backend/internal/clients"

// orderRailsPersonalized puts Continue Watching first for signed-in users (Phase 3 personalization).
func orderRailsPersonalized(rails []clients.Rail, userID string) []clients.Rail {
	if userID == "" || userID == "guest" {
		return rails
	}
	byID := make(map[string]clients.Rail, len(rails))
	for _, r := range rails {
		byID[r.ID] = r
	}
	order := []string{"continue_watching", "trending", "recommended"}
	var out []clients.Rail
	used := make(map[string]bool)
	for _, id := range order {
		if r, ok := byID[id]; ok {
			out = append(out, r)
			used[id] = true
		}
	}
	for _, r := range rails {
		if !used[r.ID] {
			out = append(out, r)
		}
	}
	return out
}

// dedupeRailsGlobally removes duplicate content IDs across rails, preserving first-rail wins order.
func dedupeRailsGlobally(rails []clients.Rail) []clients.Rail {
	seen := make(map[string]struct{})
	out := make([]clients.Rail, len(rails))
	for i, rail := range rails {
		out[i] = clients.Rail{ID: rail.ID, Title: rail.Title}
		for _, id := range rail.IDs {
			if _, ok := seen[id]; ok {
				continue
			}
			seen[id] = struct{}{}
			out[i].IDs = append(out[i].IDs, id)
		}
	}
	return out
}

func pickHeroItem(rails []clients.Rail, byID map[string]clients.ContentItem) *clients.ContentItem {
	for _, rail := range rails {
		if rail.ID != "trending" {
			continue
		}
		for _, id := range rail.IDs {
			if it, ok := byID[id]; ok {
				cp := it
				return &cp
			}
		}
	}
	for _, rail := range rails {
		for _, id := range rail.IDs {
			if it, ok := byID[id]; ok {
				cp := it
				return &cp
			}
		}
	}
	return nil
}
