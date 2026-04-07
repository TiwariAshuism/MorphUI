package models

// Component is the recursive SDUI tree node (v1).
// Unknown JSON fields should be ignored by clients (additive evolution).
type Component struct {
	Type       string           `json:"type"`
	ID         string           `json:"id,omitempty"`
	Props      map[string]any   `json:"props,omitempty"`
	Style      map[string]any   `json:"style,omitempty"`
	Children   []Component      `json:"children,omitempty"`
	Fallback   *Component       `json:"fallback,omitempty"`
	Analytics  map[string]any   `json:"analytics,omitempty"`
	Gating     *Gating          `json:"gating,omitempty"`
}

// Gating ties a subtree to feature flags or experiment variants (server-driven).
type Gating struct {
	FeatureFlag string `json:"feature_flag,omitempty"`
	Experiment  string `json:"experiment,omitempty"`
	Variant     string `json:"variant,omitempty"`
	Required    bool   `json:"required,omitempty"` // if true, hide entire branch when not satisfied (client policy)
}
