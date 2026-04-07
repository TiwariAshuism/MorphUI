package http

import (
	"log/slog"
	"net/http"
	"time"

	"morphui/backend/internal/cache"
	"morphui/backend/internal/clients"
	"morphui/backend/internal/composer"
	"morphui/backend/internal/config"
	"morphui/backend/internal/http/handlers"
	"morphui/backend/internal/http/middleware"
	"morphui/backend/internal/observability"
)

func NewRouter(logger *slog.Logger, cfg config.Config) http.Handler {
	userClient := clients.NewUserClient()
	contentClient := clients.NewContentClient()
	recoClient := clients.NewRecoClient()

	uiComposer := composer.NewComposer(userClient, contentClient, recoClient)
	mem := cache.NewMemory()
	metrics := observability.Noop{}

	mux := http.NewServeMux()
	mux.HandleFunc("GET /healthz", handlers.Healthz())
	mux.HandleFunc("GET /home", handlers.Home(uiComposer, mem, cfg, metrics))
	mux.HandleFunc("GET /section/{id}", handlers.Section(uiComposer, mem, cfg, metrics))
	mux.HandleFunc("POST /api/mylist", handlers.MyListStub())

	var h http.Handler = mux
	h = middleware.RequestID(h)
	h = middleware.Logging(logger, h, middleware.LoggingOptions{
		IncludeRequestHeaders: false,
		IncludeQuery:          true,
		SlowRequestThreshold: 800 * time.Millisecond,
	})

	return h
}
