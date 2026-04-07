package composer

import (
	"context"
	"fmt"
	"time"

	"morphui/backend/internal/clients"
	"morphui/backend/internal/models"
	"morphui/backend/internal/schema"
)

type Composer struct {
	user    *clients.UserClient
	content *clients.ContentClient
	reco    *clients.RecoClient
}

func NewComposer(user *clients.UserClient, content *clients.ContentClient, reco *clients.RecoClient) *Composer {
	return &Composer{
		user:    user,
		content: content,
		reco:    reco,
	}
}

type HomeRequest struct {
	UserID    string
	Locale    string
	TraceID   string
	Now       time.Time
	HomeTTLMs int64
}

type SectionRequest struct {
	UserID       string
	SectionID    string
	Cursor       string
	TraceID      string
	Now          time.Time
	SectionTTLMs int64
}

// BuildHome composes a realistic home: hero + ordered rails + global dedupe + image variants (Phase 3).
func (c *Composer) BuildHome(ctx context.Context, req HomeRequest) (*models.SduiEnvelope, error) {
	profile, err := c.user.GetProfile(ctx, req.UserID, req.Locale)
	if err != nil {
		return nil, err
	}

	rails, err := c.reco.HomeRails(ctx, req.UserID)
	if err != nil {
		return nil, err
	}

	rails = orderRailsPersonalized(rails, req.UserID)
	rails = dedupeRailsGlobally(rails)

	var allIDs []string
	for _, rail := range rails {
		allIDs = append(allIDs, rail.IDs...)
	}

	items, err := c.content.GetByIDs(ctx, allIDs)
	if err != nil {
		return nil, err
	}
	byID := map[string]clients.ContentItem{}
	for _, it := range items {
		byID[it.ID] = it
	}

	flags := defaultFeatureFlags()
	var hero *models.Component
	if flags["enable_hero_banner"] {
		if hi := pickHeroItem(rails, byID); hi != nil {
			h := heroBanner(*hi, profile.Name)
			hero = &h
		}
	}

	railComponents := make([]models.Component, 0, len(rails))
	for _, rail := range rails {
		var gating *models.Gating
		if rail.ID == "recommended" {
			gating = &models.Gating{
				Experiment: "reco_rail_v2",
				Variant:    "variant_b",
				Required:   false,
			}
		}
		railComponents = append(railComponents, buildRailAsColumn(rail, byID, gating))
	}

	greeting := fmt.Sprintf("Welcome, %s", profile.Name)
	screen := homeRootList(greeting, hero, railComponents)

	ttl := req.HomeTTLMs
	if ttl <= 0 {
		ttl = 30000
	}

	return &models.SduiEnvelope{
		SchemaVersion: schema.SDUIV1,
		UIVersion:     schema.UIVersion,
		PageID:        "home",
		TTLMs:         ttl,
		TraceID:       req.TraceID,
		ServerTimeMs:  req.Now.UnixMilli(),
		Experiments:   assignExperiments(req.UserID),
		FeatureFlags:  flags,
		Screen:        screen,
		FallbackPage:  FallbackPageMinimal(),
	}, nil
}

// BuildSection returns paginated section items with client cache hint (Phase 3).
func (c *Composer) BuildSection(ctx context.Context, req SectionRequest) (*models.SectionResponse, error) {
	ids, nextCursor, err := c.reco.Section(ctx, req.UserID, req.SectionID, req.Cursor)
	if err != nil {
		return nil, err
	}

	items, err := c.content.GetByIDs(ctx, ids)
	if err != nil {
		return nil, err
	}

	out := make([]models.Component, 0, len(items))
	for _, it := range items {
		out = append(out, contentCard(it, req.SectionID))
	}

	ttl := req.SectionTTLMs
	if ttl <= 0 {
		ttl = 300000
	}

	return &models.SectionResponse{
		SchemaVersion: schema.SDUIV1,
		UIVersion:     schema.UIVersion,
		SectionID:     req.SectionID,
		NextCursor:    nextCursor,
		Items:         out,
		TraceID:       req.TraceID,
		ServerTimeMs:  req.Now.UnixMilli(),
		TTLMs:         ttl,
	}, nil
}
