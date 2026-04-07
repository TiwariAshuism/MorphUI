package clients

import (
	"context"
	"strconv"
)

type RecoClient struct{}

func NewRecoClient() *RecoClient { return &RecoClient{} }

type Rail struct {
	ID    string
	Title string
	IDs   []string
}

func (c *RecoClient) HomeRails(ctx context.Context, userID string) ([]Rail, error) {
	_ = ctx

	base := "g"
	if userID != "" && userID != "guest" {
		base = userID
	}

	// Intentional overlap: t1 appears in trending and recommended to exercise global dedupe in composer.
	return []Rail{
		{
			ID:    "trending",
			Title: "Trending Now",
			IDs: []string{
				base + "_t1", base + "_t2", base + "_t3", base + "_t4", base + "_t5",
			},
		},
		{
			ID:    "continue_watching",
			Title: "Continue Watching",
			IDs: []string{
				base + "_c1", base + "_c2", base + "_c3",
			},
		},
		{
			ID:    "recommended",
			Title: "Recommended For You",
			IDs: []string{
				base + "_t1", base + "_r2", base + "_r3", base + "_r4",
			},
		},
	}, nil
}

// Section paginates with opaque string cursors: "" → "1" → "2" → "" (done).
func (c *RecoClient) Section(ctx context.Context, userID string, sectionID string, cursor string) (ids []string, nextCursor string, err error) {
	_ = ctx

	base := "g"
	if userID != "" && userID != "guest" {
		base = userID
	}

	page := 0
	if cursor != "" {
		p, err := strconv.Atoi(cursor)
		if err != nil || p < 0 {
			return nil, "", nil
		}
		page = p
	}

	switch page {
	case 0:
		return []string{
				base + "_" + sectionID + "_p0_1",
				base + "_" + sectionID + "_p0_2",
				base + "_" + sectionID + "_p0_3",
			},
			"1",
			nil
	case 1:
		return []string{
				base + "_" + sectionID + "_p1_1",
				base + "_" + sectionID + "_p1_2",
			},
			"2",
			nil
	case 2:
		return []string{
				base + "_" + sectionID + "_p2_1",
			},
			"",
			nil
	default:
		return nil, "", nil
	}
}
