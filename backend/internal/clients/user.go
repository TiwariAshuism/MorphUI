package clients

import (
	"context"
)

type UserClient struct{}

func NewUserClient() *UserClient { return &UserClient{} }

type UserProfile struct {
	UserID   string
	Name     string
	Maturity string
	Locale   string
}

func (c *UserClient) GetProfile(ctx context.Context, userID string, locale string) (UserProfile, error) {
	_ = ctx

	name := "Guest"
	if userID != "" && userID != "guest" {
		name = "User " + userID
	}

	if locale == "" {
		locale = "en-US"
	}

	return UserProfile{
		UserID:   userID,
		Name:     name,
		Maturity: "adult",
		Locale:   locale,
	}, nil
}

