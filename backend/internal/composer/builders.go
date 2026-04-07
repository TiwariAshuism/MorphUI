package composer

import (
	"morphui/backend/internal/clients"
	"morphui/backend/internal/experiments"
	"morphui/backend/internal/models"
)

func textComponent(id, value string, style map[string]any) models.Component {
	return models.Component{
		Type:  "text",
		ID:    id,
		Props: map[string]any{"value": value},
		Style: style,
	}
}

func spacerComponent(height float64) models.Component {
	return models.Component{
		Type:  "spacer",
		Props: map[string]any{"height": height},
	}
}

func spacerHorizontal(width float64) models.Component {
	return models.Component{
		Type:  "spacer",
		Props: map[string]any{"width": width},
	}
}

// buttonLoadMore uses a BFF-relative path; clients append ?cursor=<next_cursor> from the last section response.
func buttonLoadMore(railID string) models.Component {
	return models.Component{
		Type: "button",
		ID:   railID + "_load_more",
		Props: map[string]any{
			"label":  "Load more",
			// Phase 6 UX: client can show a spinner/disable during pagination.
			"loadingKey":  "loading:section:" + railID,
			"enabledKey":  "enabled:section:" + railID,
			"disabledLabel": "No more",
			"action": models.ApiCallAction("GET", "/section/"+railID).ToMap(),
		},
		Style: map[string]any{
			"margin":            16,
			"backgroundColor":   "#FF1DB954",
			"cornerRadius":      10,
			"paddingVertical":   6,
			"paddingHorizontal": 8,
		},
		Analytics: map[string]any{
			"event":      "load_more_click",
			"section_id": railID,
		},
	}
}

func imageVariantsProps(it clients.ContentItem) map[string]any {
	return map[string]any{
		"url": it.PosterMedium,
		"variants": map[string]string{
			"poster":        it.PosterMedium,
			"backdrop":      it.Backdrop,
			"hero":          it.Hero,
			"thumbnail_tiny": it.ThumbnailTiny,
		},
	}
}

func contentCard(it clients.ContentItem, sectionID string) models.Component {
	nav := models.NavigateAction("/details", map[string]string{"id": it.ID})
	return models.Component{
		Type: "card",
		ID:   sectionID + "_card_" + it.ID,
		Style: map[string]any{
			"margin":       12,
			"cornerRadius": 14,
			"elevation":    4,
		},
		Analytics: map[string]any{
			"impression": "content_card",
			"content_id": it.ID,
			"section_id": sectionID,
		},
		Children: []models.Component{
			{
				Type: "column",
				Style: map[string]any{
					"padding": 12,
				},
				Children: []models.Component{
					{
						Type:  "image",
						Props: imageVariantsProps(it),
						Style: map[string]any{
							"height":       180,
							"cornerRadius": 12,
						},
					},
					spacerComponent(8),
					textComponent(sectionID+"_text_"+it.ID, it.Title, map[string]any{
						"fontSize":   16,
						"fontWeight": "medium",
					}),
					spacerComponent(8),
					{
						Type: "button",
						Props: map[string]any{
							"label":  "Open details",
							"action": nav.ToMap(),
						},
					},
				},
			},
		},
	}
}

func buildRailAsColumn(rail clients.Rail, byID map[string]clients.ContentItem, gating *models.Gating) models.Component {
	children := make([]models.Component, 0, len(rail.IDs)+3)
	children = append(children, textComponent(rail.ID+"_title", rail.Title, map[string]any{
		"paddingHorizontal": 16,
		"paddingVertical":   8,
		"fontSize":          18,
		"fontWeight":        "semibold",
	}))
	for _, id := range rail.IDs {
		if it, ok := byID[id]; ok {
			children = append(children, contentCard(it, rail.ID))
		}
	}
	children = append(children, buttonLoadMore(rail.ID))

	col := models.Component{
		Type:     "column",
		ID:       "rail_" + rail.ID,
		Children: children,
		Gating:   gating,
		Analytics: map[string]any{
			"rail_id": rail.ID,
		},
	}
	return col
}

// FallbackPageMinimal is shown when the client cannot render the primary tree (Phase 2 contract).
func FallbackPageMinimal() *models.Component {
	return &models.Component{
		Type: "column",
		ID:   "fallback_page",
		Children: []models.Component{
			textComponent("fallback_title", "Limited experience", map[string]any{
				"padding":    24,
				"fontSize":   18,
				"fontWeight": "bold",
			}),
			textComponent("fallback_body", "We could not load the full home screen. Pull to refresh or try again later.", map[string]any{
				"paddingHorizontal": 24,
				"fontSize":          14,
			}),
		},
	}
}

// heroBanner is a Netflix-style hero built from primitives the Android renderer already supports.
func heroBanner(it clients.ContentItem, displayName string) models.Component {
	play := models.NavigateAction("/play", map[string]string{"id": it.ID})
	myList := models.ApiCallWithBody("POST", "/api/mylist", map[string]any{"content_id": it.ID})

	return models.Component{
		Type: "column",
		ID:   "home_hero",
		Analytics: map[string]any{
			"section":    "hero",
			"content_id": it.ID,
		},
		Children: []models.Component{
			{
				Type: "card",
				ID:   "hero_card",
				Style: map[string]any{
					"margin":       12,
					"cornerRadius": 16,
					"elevation":    6,
				},
				Children: []models.Component{
					{
						Type: "column",
						Children: []models.Component{
							{
								Type: "image",
								Props: map[string]any{
									"url": it.Hero,
									"variants": map[string]string{
										"poster":   it.PosterMedium,
										"backdrop": it.Backdrop,
										"hero":     it.Hero,
									},
									"contentDescription": it.Title,
								},
								Style: map[string]any{
									"height":       220,
									"cornerRadius": 12,
								},
							},
							spacerComponent(12),
							textComponent("hero_kicker", "Featured for "+displayName, map[string]any{
								"paddingHorizontal": 12,
								"fontSize":          14,
								"fontWeight":        "medium",
							}),
							textComponent("hero_title", it.Title, map[string]any{
								"paddingHorizontal": 12,
								"paddingVertical":   6,
								"fontSize":          22,
								"fontWeight":        "bold",
							}),
							textComponent("hero_subtitle", it.Subtitle, map[string]any{
								"paddingHorizontal": 12,
								"fontSize":          14,
							}),
							spacerComponent(12),
							{
								Type: "row",
								ID:   "hero_actions",
								Style: map[string]any{
									"paddingHorizontal": 12,
									"paddingVertical":   8,
								},
								Children: []models.Component{
									{
										Type: "button",
										Props: map[string]any{
											"label":  "Play",
											"action": play.ToMap(),
										},
										Style: map[string]any{
											"backgroundColor": "#FFE50914",
											"cornerRadius":  8,
										},
									},
									spacerHorizontal(12),
									{
										Type: "button",
										Props: map[string]any{
											"label":  "My List",
											"action": myList.ToMap(),
										},
										Style: map[string]any{
											"backgroundColor": "#33FFFFFF",
											"cornerRadius":  8,
										},
									},
								},
							},
							spacerComponent(8),
						},
					},
				},
			},
		},
	}
}

func homeRootList(greeting string, hero *models.Component, rails []models.Component) *models.Component {
	n := len(rails) + 1
	if hero != nil {
		n++
	}
	children := make([]models.Component, 0, n)
	if hero != nil {
		children = append(children, *hero)
	}
	children = append(children, textComponent("home_greeting", greeting, map[string]any{
		"padding":    16,
		"fontSize":   22,
		"fontWeight": "bold",
	}))
	children = append(children, rails...)
	return &models.Component{
		Type:     "list",
		ID:       "home_root_list",
		Children: children,
	}
}

func assignExperiments(userID string) map[string]string {
	m := experiments.Assign(userID)
	if userID != "" && userID != "guest" {
		m["reco_personalization"] = "on"
	}
	return m
}

func defaultFeatureFlags() map[string]bool {
	return map[string]bool{
		"enable_hero_banner": true,
		"enable_carousel":    false,
	}
}
