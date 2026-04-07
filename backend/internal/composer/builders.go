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
			"label": "Load more",
			// Phase 6 UX: client can show a spinner/disable during pagination.
			"loadingKey":    "loading:section:" + railID,
			"enabledKey":    "enabled:section:" + railID,
			"disabledLabel": "No more",
			"action":        models.ApiCallAction("GET", "/section/"+railID).ToMap(),
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
			"poster":         it.PosterMedium,
			"backdrop":       it.Backdrop,
			"hero":           it.Hero,
			"thumbnail_tiny": it.ThumbnailTiny,
		},
	}
}

func heroImageProps(it clients.ContentItem) map[string]any {
	return map[string]any{
		"url": it.Hero,
		"variants": map[string]string{
			"poster":         it.PosterMedium,
			"backdrop":       it.Backdrop,
			"hero":           it.Hero,
			"thumbnail_tiny": it.ThumbnailTiny,
		},
		"contentDescription": it.Title,
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
		"textColor":         "#FFFFFFFF",
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

// ── Cinema home (HTML reference: Tailwind CINEMA dark, hero-gradient, bento, nav) ──

func cinemaTopBar() models.Component {
	search := models.NavigateAction("/search", map[string]string{})
	return models.Component{
		Type: "column",
		ID:   "cinema_top_bar_wrap",
		Style: map[string]any{
			"zIndex":          50,
			"blurDp":          24,
			"backgroundColor": "#990A0A0A",
		},
		Children: []models.Component{
			{
				Type: "row",
				ID:   "cinema_top_bar",
				Style: map[string]any{
					"horizontalArrangement": "spaceBetween",
					"paddingHorizontal":     24,
					"height":                64,
				},
				Children: []models.Component{
					{
						Type: "row",
						ID:   "cinema_top_bar_left",
						Style: map[string]any{
							"horizontalArrangement": "start",
						},
						Children: []models.Component{
							textComponent("cinema_icon", "🎬", map[string]any{
								"fontSize":  24,
								"textColor": "#FFDC2626",
							}),
							spacerHorizontal(8),
							textComponent("cinema_logo", "CINEMA", map[string]any{
								"fontSize":      24,
								"fontWeight":    "black",
								"textColor":     "#FFE50914",
								"letterSpacing": 2,
							}),
						},
					},
					{
						Type: "icon_button",
						ID:   "cinema_search",
						Props: map[string]any{
							"icon":   "🔍",
							"action": search.ToMap(),
						},
						Style: map[string]any{
							"textColor": "#FFE2E2E2",
						},
					},
				},
			},
		},
	}
}

func cinemaHeroSubtitle(it clients.ContentItem) string {
	if it.Subtitle != "" {
		return it.Subtitle
	}
	return "In a world where digital consciousness is the only currency, one survivor must breach the final firewall to save humanity's legacy."
}

func cinemaHeroBox(it clients.ContentItem) models.Component {
	play := models.NavigateAction("/play", map[string]string{"id": it.ID})
	myList := models.ApiCallWithBody("POST", "/api/mylist", map[string]any{"content_id": it.ID})

	return models.Component{
		Type: "box",
		ID:   "cinema_hero",
		Style: map[string]any{
			"height": 795,
		},
		Analytics: map[string]any{
			"section":    "hero",
			"content_id": it.ID,
		},
		Children: []models.Component{
			{
				Type:  "image",
				Props: heroImageProps(it),
				Style: map[string]any{
					"layoutAlign": "fill",
				},
			},
			{
				Type: "box",
				ID:   "cinema_hero_gradient",
				Style: map[string]any{
					"layoutAlign":       "fill",
					"gradientDirection": "vertical",
					"gradientFromColor": "#00000000",
					"gradientFromAlpha": 0,
					"gradientViaColor":  "#FF131313",
					"gradientViaAlpha":  0.2,
					"gradientToColor":   "#FF131313",
					"gradientToAlpha":   1,
				},
				Children: []models.Component{},
			},
			{
				Type: "column",
				Style: map[string]any{
					"layoutAlign":       "bottomStart",
					"paddingHorizontal": 24,
					"paddingVertical":   48,
				},
				Children: []models.Component{
					textComponent("hero_kicker", "TRENDING NOW", map[string]any{
						"fontSize":          10,
						"fontWeight":        "bold",
						"textColor":         "#FFE50914",
						"backgroundColor":   "#33E50914",
						"paddingHorizontal": 12,
						"paddingVertical":   4,
						"cornerRadius":      999,
						"borderWidth":       1,
						"borderColor":       "#4DE50914",
					}),
					spacerComponent(24),
					textComponent("hero_title_primary", "THE LAST", map[string]any{
						"fontSize":   60,
						"fontWeight": "black",
						"textColor":  "#FFE2E2E2",
					}),
					textComponent("hero_title_accent", "PROTOCOL", map[string]any{
						"fontSize":   72,
						"fontWeight": "black",
						"textColor":  "#FFE50914",
					}),
					spacerComponent(24),
					textComponent("hero_subtitle", cinemaHeroSubtitle(it), map[string]any{
						"fontSize":  14,
						"textColor": "#FFE9BCB6",
						"maxWidth":  320,
					}),
					spacerComponent(12),
					{
						Type: "row",
						ID:   "hero_actions",
						Style: map[string]any{
							"horizontalArrangement": "start",
						},
						Children: []models.Component{
							{
								Type: "button",
								Props: map[string]any{
									"label":  "▶  WATCH NOW",
									"action": play.ToMap(),
								},
								Style: map[string]any{
									"weight":          1,
									"height":          56,
									"backgroundColor": "#FFE50914",
									"textColor":       "#FFFFF7F6",
									"cornerRadius":    999,
									"paddingVertical": 16,
								},
							},
							spacerHorizontal(12),
							{
								Type: "icon_button",
								Props: map[string]any{
									"icon":   "＋",
									"action": myList.ToMap(),
								},
								Style: map[string]any{
									"textColor":       "#FFE2E2E2",
									"width":           56,
									"height":          56,
									"cornerRadius":    28,
									"backgroundColor": "#994D4D4D",
									"borderWidth":     1,
									"borderColor":     "#1AFFFFFF",
								},
							},
						},
					},
				},
			},
		},
	}
}

func cinemaMovieCard(it clients.ContentItem, suffix string, metaLine string) models.Component {
	children := []models.Component{
		{
			Type:  "image",
			Props: imageVariantsProps(it),
			Style: map[string]any{
				"height":       240,
				"width":        160,
				"cornerRadius": 12,
			},
		},
		spacerComponent(8),
		textComponent("movie_title_"+it.ID+suffix, it.Title, map[string]any{
			"fontSize":   12,
			"fontWeight": "semibold",
			"textColor":  "#FFFFFEFE",
		}),
	}
	if metaLine != "" {
		children = append(children, spacerComponent(4))
		children = append(children, textComponent("movie_meta_"+it.ID+suffix, metaLine, map[string]any{
			"fontSize":   10,
			"fontWeight": "normal",
			"textColor":  "#FF737373",
		}))
	}
	return models.Component{
		Type: "column",
		ID:   "movie_card_" + it.ID + suffix,
		Style: map[string]any{
			"width": 160,
		},
		Children: children,
	}
}

func cinemaTrendingSection(ids []string, byID map[string]clients.ContentItem) models.Component {
	meta := []string{
		"Sci-Fi • 2024",
		"Thriller • 2023",
		"Drama • 2024",
	}
	children := make([]models.Component, 0, len(ids))
	for i, id := range ids {
		if it, ok := byID[id]; ok {
			children = append(children, cinemaMovieCard(it, "_trend", meta[i%len(meta)]))
		}
	}
	return models.Component{
		Type: "column",
		ID:   "section_trending",
		Style: map[string]any{
			"paddingHorizontal": 24,
			"paddingVertical":   8,
		},
		Children: []models.Component{
			{
				Type: "row",
				ID:   "trending_header_row",
				Style: map[string]any{
					"horizontalArrangement": "spaceBetween",
				},
				Children: []models.Component{
					textComponent("trending_heading", "Trending Now", map[string]any{
						"fontWeight": "bold",
						"fontSize":   20,
						"textColor":  "#FFE2E2E2",
					}),
					{
						Type: "button",
						ID:   "trending_see_all",
						Props: map[string]any{
							"label":  "See All",
							"action": models.NavigateAction("/trending", map[string]string{}).ToMap(),
						},
						Style: map[string]any{
							"backgroundColor":   "#00000000",
							"padding":           0,
							"paddingHorizontal": 0,
							"paddingVertical":   0,
							"fontSize":          10,
							"textColor":         "#FFE50914",
						},
					},
				},
			},
			spacerComponent(16),
			{
				Type: "carousel",
				Props: map[string]any{
					"itemSpacingDp":              16,
					"contentPaddingHorizontalDp": 0,
				},
				Children: children,
			},
		},
	}
}

func curatedContentIDs(userID string) (wideID, smallAID, smallBID string) {
	if userID == "" || userID == "guest" {
		return "g_t1", "g_t2", "g_r2"
	}
	return userID + "_t1", userID + "_t2", userID + "_r2"
}

func cinemaCuratedSection(byID map[string]clients.ContentItem, userID string) models.Component {
	wideID, _, _ := curatedContentIDs(userID)
	itW := byID[wideID]

	return models.Component{
		Type: "column",
		ID:   "section_curated",
		Style: map[string]any{
			"paddingHorizontal": 24,
			"paddingVertical":   8,
		},
		Children: []models.Component{
			textComponent("curated_heading", "Curated For You", map[string]any{
				"fontWeight": "bold",
				"fontSize":   20,
				"textColor":  "#FFE2E2E2",
			}),
			spacerComponent(16),
			{
				Type: "column",
				Children: []models.Component{
					{
						Type: "box",
						ID:   "curated_wide",
						Style: map[string]any{
							"height": 176,
						},
						Children: []models.Component{
							{
								Type:  "image",
								Props: heroImageProps(itW),
								Style: map[string]any{
									"layoutAlign": "fill",
									"opacity":     0.6,
								},
							},
							{
								Type: "box",
								Style: map[string]any{
									"layoutAlign":       "fill",
									"gradientDirection": "horizontal",
									"gradientFromColor": "#FF131313",
									"gradientFromAlpha": 0.9,
									"gradientViaColor":  "#FF131313",
									"gradientViaAlpha":  0.4,
									"gradientToColor":   "#00000000",
									"gradientToAlpha":   0,
								},
								Children: []models.Component{},
							},
							{
								Type: "column",
								Style: map[string]any{
									"layoutAlign":       "centerStart",
									"paddingHorizontal": 24,
									"paddingVertical":   24,
								},
								Children: []models.Component{
									textComponent("curated_special", "Special Edition", map[string]any{
										"fontSize":   10,
										"fontWeight": "bold",
										"textColor":  "#FFE50914",
									}),
									textComponent("curated_wide_label", "THE DIRECTOR'S\nCUT", map[string]any{
										"fontSize":   24,
										"fontWeight": "black",
										"textColor":  "#FFE2E2E2",
									}),
								},
							},
						},
					},
					spacerComponent(12),
					{
						Type: "row",
						ID:   "curated_small_row",
						Style: map[string]any{
							"horizontalArrangement": "spacedBy",
						},
						Children: []models.Component{
							cinemaBentoGrayCard(),
							cinemaBentoRedCard(),
						},
					},
				},
			},
		},
	}
}

func cinemaBentoGrayCard() models.Component {
	return models.Component{
		Type: "box",
		ID:   "curated_bento_gray",
		Style: map[string]any{
			"weight":          1,
			"height":          176,
			"backgroundColor": "#FF2A2A2A",
			"cornerRadius":    12,
			"borderWidth":     1,
			"borderColor":     "#335E3F3B",
		},
		Children: []models.Component{
			{
				Type: "column",
				Style: map[string]any{
					"layoutAlign":               "fill",
					"padding":                   20,
					"columnHorizontalAlignment": "start",
					"verticalArrangement":       "spaceBetween",
				},
				Children: []models.Component{
					{
						Type: "box",
						ID:   "bento_gray_icon_wrap",
						Style: map[string]any{
							"width":           40,
							"height":          40,
							"cornerRadius":    8,
							"backgroundColor": "#FF131313",
						},
						Children: []models.Component{
							textComponent("bento_gray_icon", "★", map[string]any{
								"fontSize":    18,
								"textColor":   "#FFE50914",
								"layoutAlign": "center",
							}),
						},
					},
					textComponent("bento_gray_title", "Award Winning Originals", map[string]any{
						"fontSize":   14,
						"fontWeight": "bold",
						"textColor":  "#FFE2E2E2",
					}),
				},
			},
		},
	}
}

func cinemaBentoRedCard() models.Component {
	return models.Component{
		Type: "box",
		ID:   "curated_bento_red",
		Style: map[string]any{
			"weight":          1,
			"height":          176,
			"backgroundColor": "#FFE50914",
			"cornerRadius":    12,
		},
		Children: []models.Component{
			{
				Type: "column",
				Style: map[string]any{
					"layoutAlign":               "fill",
					"padding":                   20,
					"columnHorizontalAlignment": "start",
					"verticalArrangement":       "spaceBetween",
				},
				Children: []models.Component{
					{
						Type: "box",
						ID:   "bento_red_icon_wrap",
						Style: map[string]any{
							"width":           40,
							"height":          40,
							"cornerRadius":    8,
							"backgroundColor": "#1AFFFFFF",
						},
						Children: []models.Component{
							textComponent("bento_red_icon", "✨", map[string]any{
								"fontSize":    18,
								"textColor":   "#FFFFF7F6",
								"layoutAlign": "center",
							}),
						},
					},
					textComponent("bento_red_title", "TOP 10 TODAY", map[string]any{
						"fontSize":   14,
						"fontWeight": "black",
						"textColor":  "#FFFFF7F6",
					}),
				},
			},
		},
	}
}

func cinemaNewReleasesSection(ids []string, byID map[string]clients.ContentItem) models.Component {
	children := make([]models.Component, 0, len(ids))
	for i, id := range ids {
		if it, ok := byID[id]; ok {
			children = append(children, cinemaReleaseCard(it, i))
		}
	}
	return models.Component{
		Type: "column",
		ID:   "section_new_releases",
		Style: map[string]any{
			"paddingHorizontal": 24,
			"paddingVertical":   8,
		},
		Children: []models.Component{
			{
				Type: "row",
				ID:   "new_releases_header_row",
				Style: map[string]any{
					"horizontalArrangement": "spaceBetween",
				},
				Children: []models.Component{
					textComponent("new_releases_heading", "New Releases", map[string]any{
						"fontWeight": "bold",
						"fontSize":   20,
						"textColor":  "#FFE2E2E2",
					}),
					{
						Type: "button",
						ID:   "new_releases_explore",
						Props: map[string]any{
							"label":  "Explore",
							"action": models.NavigateAction("/new", map[string]string{}).ToMap(),
						},
						Style: map[string]any{
							"backgroundColor":   "#00000000",
							"padding":           0,
							"paddingHorizontal": 0,
							"paddingVertical":   0,
							"fontSize":          10,
							"textColor":         "#FFE50914",
						},
					},
				},
			},
			spacerComponent(16),
			{
				Type: "carousel",
				Props: map[string]any{
					"itemSpacingDp":              16,
					"contentPaddingHorizontalDp": 0,
				},
				Children: children,
			},
		},
	}
}

func cinemaReleaseCard(it clients.ContentItem, index int) models.Component {
	title := it.Title
	if title == "" {
		title = "Movie"
	}
	return models.Component{
		Type: "box",
		ID:   "new_rel_" + it.ID,
		Style: map[string]any{
			"width":  256,
			"height": 144,
		},
		Children: []models.Component{
			{
				Type:  "image",
				Props: imageVariantsProps(it),
				Style: map[string]any{
					"layoutAlign": "fill",
				},
			},
			{
				Type: "box",
				Style: map[string]any{
					"layoutAlign":       "fill",
					"gradientDirection": "vertical",
					"gradientFromColor": "#00000000",
					"gradientFromAlpha": 0,
					"gradientViaColor":  "#00000000",
					"gradientViaAlpha":  0,
					"gradientToColor":   "#CC000000",
					"gradientToAlpha":   1,
				},
				Children: []models.Component{},
			},
			textComponent("new_rel_lbl_"+it.ID, title, map[string]any{
				"layoutAlign": "bottomStart",
				"textColor":   "#FFFFFFFF",
				"padding":     16,
				"fontSize":    14,
				"fontWeight":  "bold",
			}),
		},
	}
}

func navItem(id, label, icon string, selected bool, action *models.Action) models.Component {
	return models.Component{
		Type: "nav_item",
		ID:   id,
		Props: map[string]any{
			"label":    label,
			"icon":     icon,
			"selected": selected,
			"action":   action.ToMap(),
		},
	}
}

func cinemaBottomNav() models.Component {
	return models.Component{
		Type: "bottom_nav",
		ID:   "cinema_bottom_nav",
		Style: map[string]any{
			"backgroundColor": "#CC0A0A0A",
			"blurDp":          24,
			"elevation":       8,
		},
		Children: []models.Component{
			navItem("nav_home", "Home", "🏠", true, models.NavigateAction("/home", map[string]string{})),
			navItem("nav_new", "New", "✨", false, models.NavigateAction("/new", map[string]string{})),
			navItem("nav_search", "Search", "🔍", false, models.NavigateAction("/search", map[string]string{})),
			navItem("nav_downloads", "Downloads", "⬇", false, models.NavigateAction("/downloads", map[string]string{})),
		},
	}
}

func cinemaScrollList(children []models.Component) models.Component {
	return models.Component{
		Type: "list",
		ID:   "cinema_scroll",
		Props: map[string]any{
			"contentPaddingBottomDp": 80,
		},
		Style: map[string]any{
			"weight":          1,
			"backgroundColor": "#FF131313",
		},
		Children: children,
	}
}

func cinemaRootColumn(scrollList models.Component, bottomNav models.Component) *models.Component {
	return &models.Component{
		Type: "column",
		ID:   "cinema_root",
		Style: map[string]any{
			"backgroundColor": "#FF131313",
		},
		Children: []models.Component{scrollList, bottomNav},
	}
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
		"enable_carousel":    true,
	}
}
