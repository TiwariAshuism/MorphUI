package models

import "encoding/json"

// Action matches the Android ActionParser contract (PascalCase type names).
// See task.md for canonical lowercase names; clients should accept both in a later phase.
type Action struct {
	Type     string `json:"type"`
	Route    string `json:"route,omitempty"`
	URL      string `json:"url,omitempty"`
	Params   map[string]string `json:"params,omitempty"`
	Endpoint string `json:"endpoint,omitempty"`
	Method   string `json:"method,omitempty"`
	Body     map[string]any `json:"body,omitempty"`
	OnSuccess *Action `json:"onSuccess,omitempty"`
	OnError   *Action `json:"onError,omitempty"`
	Message  string `json:"message,omitempty"`
	Name     string `json:"name,omitempty"`
	// Pagination / load_more (Phase 6)
	SectionID string `json:"section_id,omitempty"`
	Cursor    string `json:"cursor,omitempty"`
	RequiresAuth bool `json:"requires_auth,omitempty"`
}

// NavigateAction returns a Navigate action (Jetpack Navigation route + string params).
func NavigateAction(route string, params map[string]string) *Action {
	if params == nil {
		params = map[string]string{}
	}
	return &Action{Type: "Navigate", Route: route, Params: params}
}

// ApiCallAction returns an ApiCall action (GET/POST to BFF-relative path).
func ApiCallAction(method, endpoint string) *Action {
	return &Action{Type: "ApiCall", Method: method, Endpoint: endpoint}
}

// ApiCallWithBody returns an ApiCall with a JSON body (e.g. like / my-list).
func ApiCallWithBody(method, endpoint string, body map[string]any) *Action {
	return &Action{Type: "ApiCall", Method: method, Endpoint: endpoint, Body: body}
}

// ToMap encodes the action for embedding in component props (map[string]any).
func (a *Action) ToMap() map[string]any {
	if a == nil {
		return nil
	}
	b, err := json.Marshal(a)
	if err != nil {
		return nil
	}
	var m map[string]any
	if err := json.Unmarshal(b, &m); err != nil {
		return nil
	}
	return m
}
