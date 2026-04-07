# MorphUI BFF

## Run

From repo root:

```bash
cd backend
go run ./cmd/bff
```

Env:

- `BFF_ADDR` (default `:8080`)
- `LOG_LEVEL` (`debug|info|warn|error`, default `info`)
- `HOME_CACHE_TTL_SEC` — in-memory cache TTL for `GET /home` per `user + Accept-Language` (default **30**)
- `SECTION_CACHE_TTL_SEC` — cache TTL for `GET /section/{id}` per `user + section + cursor` (default **300**)
- `BFF_DISABLE_CACHE` — set `true` to bypass response cache (local debugging)

## Endpoints

- `GET /healthz`
- `GET /home`
  - dev identity: header `X-User-Id` or query `?user_id=...`
- `GET /section/{id}?cursor=...`
  - cursors are opaque page tokens: `""` → `"1"` → `"2"` → `""` (end)

## Phase 2 — SDUI schema v1 (typed contract)

Responses use typed structs in `internal/models` and constants in `internal/schema`.

### `GET /home` envelope (`SduiEnvelope`)

| Field | Meaning |
|-------|---------|
| `schema_version` | e.g. `sdui.v1` |
| `ui_version` | integer wire version (see `internal/schema/version.go`) |
| `page_id` | logical screen id (e.g. `home`) |
| `ttl_ms` | suggested client cache TTL (aligned with `HOME_CACHE_TTL_SEC`) |
| `trace_id` | correlates with `X-Request-Id` |
| `server_time_ms` | server clock (Unix ms) |
| `experiments` | string map of experiment → variant (A/B) |
| `feature_flags` | bool map for server-driven toggles |
| `screen` | root **component tree** (Android `MorphUIEngine` reads `screen`) |
| `fallback_page` | optional degraded UI if primary tree fails client-side |

### Component node (`Component`)

| Field | Meaning |
|-------|---------|
| `type` | `text`, `list`, `column`, `row`, `card`, `image`, … |
| `id` | stable id for analytics / diffing |
| `props` / `style` | maps to existing Android DTOs |
| `children` | nested components |
| `fallback` | subtree if this node fails |
| `analytics` | impression/click metadata |
| `gating` | experiment / feature-flag metadata |

### `GET /section/{id}` (`SectionResponse`)

Includes `schema_version`, `ui_version`, `section_id`, `next_cursor`, `items`, `trace_id`, `server_time_ms`, **`ttl_ms`** (section cache hint).

### JSON Schema (documentation)

See `schema/sdui.v1.schema.json` for a draft JSON Schema of the envelope.

## Phase 3 — Realistic home + caching + response shaping

- **Hero**: personalized banner (`column` → `card` → image + text + `row` with Play / My List) using `Hero` image URL; gated by `feature_flags.enable_hero_banner` (default **true**).
- **Rails**: Trending, Continue Watching, Recommended; **signed-in users** get **Continue Watching → Trending → Recommended**.
- **Deduping**: same `content_id` appears only in the first rail (e.g. `t1` overlap between Trending and Recommended is removed from Recommended).
- **Images**: cards use **poster** URLs; hero uses **wide hero** URL; image `props` may include `variants` (`poster`, `backdrop`, `hero`, `thumbnail_tiny`) for richer clients.
- **Pagination**: `next_cursor` strings for section pages; **Load more** uses `ApiCall` `GET /section/{railId}` (client appends `?cursor=` from the last response).
- **Caching**: in-process TTL cache (`internal/cache`) with **cache hit/miss** hooks (`internal/observability`); cached payloads **strip** `trace_id` / `server_time_ms` and refresh them on each response.

## Notes

- The composer still emits **only** primitives the current Android renderer supports (`column`, `list`, `row`, `card`, `image`, `text`, `button`, `spacer`). Dedicated `carousel` / `grid` / `page` types land in Phase 5.
- Actions remain **PascalCase** (`Navigate`, `ApiCall`) to match `ActionParser` on Android.
