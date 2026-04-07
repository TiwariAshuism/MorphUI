package clients

import (
	"context"
	"fmt"
)

type ContentClient struct{}

func NewContentClient() *ContentClient { return &ContentClient{} }

// ContentItem includes multiple image roles for response shaping (Phase 3).
// Clients use `url` on image props; optional `variants` carries hints for Coil sizing / future clients.
type ContentItem struct {
	ID            string
	Title         string
	Subtitle      string
	PosterMedium  string
	Backdrop      string
	Hero          string
	ThumbnailTiny string
}

func (c *ContentClient) GetByIDs(ctx context.Context, ids []string) ([]ContentItem, error) {
	_ = ctx

	out := make([]ContentItem, 0, len(ids))
	for _, id := range ids {
		out = append(out, ContentItem{
			ID:            id,
			Title:         fmt.Sprintf("Title %s", id),
			Subtitle:      "Because you watched similar titles",
			PosterMedium:  fmt.Sprintf("https://picsum.photos/seed/%s_poster/300/450", id),
			Backdrop:      fmt.Sprintf("https://picsum.photos/seed/%s_backdrop/1280/720", id),
			Hero:          fmt.Sprintf("https://picsum.photos/seed/%s_hero/1920/640", id),
			ThumbnailTiny: fmt.Sprintf("https://picsum.photos/seed/%s_thumb/120/180", id),
		})
	}
	return out, nil
}
