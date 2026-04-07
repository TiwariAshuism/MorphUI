# MorphUI — Netflix-style SDUI + Go BFF (Execution Tasks)

This document is the **single source of truth** for implementing a
production-grade **Server-Driven UI (SDUI)** system (Netflix/Airbnb style)
using:

- **Android client**: Kotlin + Jetpack Compose (dynamic renderer)
- **Backend-for-Frontend (BFF)**: Go
- **Architecture**: microservices + BFF + UI Composer
- **Data format**: JSON-based UI schema with versioning, fallbacks, actions

It is written so **another AI** (or engineer) can continue the work with
minimal context.

---

## 1) Current repo state (what exists today)

### Android SDUI runtime (`sdui/`)

- **Engine / registry / renderer / validation / binding / cache** — as before; built-ins now include **`page`**, **`carousel`**, **`grid`**, **`hero`**.
- **`ScreenViewModel`** — loads `home` from the **Go BFF** (`BffScreenRepository`); **`handleApiCall()`** runs **`ApiActionExecutor`** (allowlisted HTTP + section pagination merge).
- **`DynamicScreen`** — collects **`uiEvents`** for async **`onSuccess` / `onError`** (e.g. navigation after API completion).
- **Identity** — **`UserIdentity`** (device-scoped anonymous id) → **`X-User-Id`** on BFF calls.
- **Analytics** — **`BffActionAnalytics`** → **`POST /api/events`** (batch, persistence + retry, impression dedupe).

### Go BFF (`backend/`)

- **`GET /healthz`**, **`GET /home`**, **`GET /section/{id}`**, **`POST /api/events`**, **`POST /api/mylist`**, **`GET /metrics`**
- **Composer** — hero + rails, caching, **experiments** (stable bucketing), **variant-driven** rail order / hero copy.
- **Phase 8** — contract tests, fuzz tests, CORS, rate limit on events, per-route body limits, **`slog`** on events, in-memory **Prometheus-style** `/metrics`.

### Still optional / future

- Replace anonymous id with **real auth** and hardened **TLS / pinning** for production BFF URLs.
- **Viewport-based** impressions (vs current tree-walk / session dedupe).
- **Durable** event store (DB/stream) instead of logs-only on the BFF.

---

## 2) Target system architecture

Text diagram (end-to-end):

```
┌─────────────────────────────┐
│ Android App (Kotlin/Compose) │
│ - SDUI Engine               │
│ - Component Registry        │
│ - Compose Renderer          │
│ - Action Dispatcher         │
│ - Cache (UICache)           │
└──────────────┬──────────────┘
               │ HTTPS
               │  GET /home
               │  GET /section/{id}?cursor=...
               │  POST /actions/... (optional)
               v
┌─────────────────────────────┐
│ Go BFF                       │
│ - Auth/session               │
│ - Aggregation                │
│ - UI Composer                │
│ - Feature flags/experiments  │
│ - Observability              │
└───────┬───────────┬─────────┘
        │           │
        │           │
        v           v
┌──────────────┐  ┌────────────────┐  ┌─────────────────────┐
│ User Service  │  │ Content Service │  │ Recommendation Svc  │
│ (profile etc) │  │ (metadata/art)  │  │ (ranked rails)      │
└──────────────┘  └────────────────┘  └─────────────────────┘
```

### Request flow

1. Android calls `GET /home` with:
   - auth/session identifiers
   - locale/device capabilities (supported UI versions, component capabilities)
2. Go BFF:
   - fetches user profile
   - fetches recommendation rails + content metadata
   - applies experiments/flags
   - composes a **Page SDUI JSON** response
3. Android:
   - validates schema (lenient/strict depending on config)
   - resolves bindings (optional)
   - parses into `UIComponent` tree
   - renders via Compose
4. User interacts:
   - action dispatch occurs (navigate, deep link, API call, load more)
   - for pagination rails: Android calls `GET /section/{id}?cursor=...`
   - merges section content into UI state

---

## 3) SDUI schema contract (v1)

### Design goals

- **Extensible**: new components without breaking old clients
- **Versioned**: client/server negotiate supported versions/capabilities
- **Backwards compatible**: additive changes preferred; unknown fields ignored
- **Safe**: fallbacks and error handling are first-class
- **Actionable**: actions include enough context for navigation/API calls
- **Observable**: analytics hooks and trace propagation

### Envelope (top-level response)

Required in v1:

- `ui_version` (int)
- `page` (component tree; typically type=`page`)

Recommended:

- `page_id` (string) e.g. `"home"`
- `ttl_ms` (int)
- `trace_id` (string)
- `experiments` (object) e.g. assignments
- `server_time_ms` (int)
- `fallback_page` (component) optional
- `errors` (array) optional (non-fatal schema warnings)

Example envelope shape:

```json
{
  "ui_version": 1,
  "page_id": "home",
  "ttl_ms": 30000,
  "trace_id": "abc-123",
  "experiments": {
    "home_rail_order": "variant_b"
  },
  "page": {
    "type": "page",
    "id": "home_page",
    "props": { "title": "Home" },
    "children": []
  }
}
```

### Component shape (matches existing engine conventions)

All components:

- `type`: string (required)
- `id`: string (optional; stable IDs improve diffing, analytics, caching)
- `props`: object/map (optional; component specific)
- `style`: object (optional; maps to `UIStyle`)
- `children`: array of components (optional; supported by container types)
- `fallback`: component (optional; render if this component fails/unsupported)
- `analytics`: object (optional; impression/click metadata)
- `gating`: object (optional; feature flag / experiment gating)

### Built-in components required by the final system

Must support (as per requirements):

- `page` (container/page)
- `text`
- `image` (and/or `banner`/`hero`)
- `button`
- `list` (vertical list)
- `carousel` (horizontal rail)
- `grid` (grid layout)
- `container` (generic, or reuse `column`/`row`)

Current repo already supports:

- `text`, `image`, `button`, `list`, `column`, `row`, etc.

Implemented in registry/renderer:

- `page`, `carousel`, `grid`, `hero` (see `ComponentRegistry`, `ComposeRenderer`)

### Actions (v1)

Canonical action JSON:

- `type`: one of
  - `navigate`
  - `deeplink`
  - `api_call`
  - `load_more`
  - `refresh`
  - `back`
  - `none`

Recommended common fields:

- `analytics` (object)
- `requires_auth` (bool)

Examples:

Navigate:
```json
{ "type": "navigate", "route": "/details", "params": { "id": "tt123" } }
```

Deep link:
```json
{ "type": "deeplink", "url": "morphui://details/tt123" }
```

API call (for POST/GET; can return UI patches later):
```json
{
  "type": "api_call",
  "method": "POST",
  "endpoint": "/like",
  "body": { "content_id": "tt123" },
  "on_success": { "type": "show_toast", "message": "Liked" },
  "on_error": { "type": "show_toast", "message": "Failed" }
}
```

Pagination:
```json
{
  "type": "load_more",
  "section_id": "trending",
  "endpoint": "/section/trending",
  "cursor": "next_cursor_token"
}
```

NOTE: The current Android `UIAction` uses PascalCase action types
(`Navigate`, `ApiCall`, etc). Either:

1) Update Android `ActionParser` to accept both `navigate` and `Navigate`
   (recommended for compatibility), or
2) Make backend emit PascalCase action types (short-term).

### Versioning & backwards compatibility rules

Client contract:

- Client declares:
  - supported `ui_version` range
  - supported component/action capabilities (optional)
- Client must:
  - ignore unknown fields
  - render `UnknownComponent` for unknown `type`
  - use `fallback` when provided

Server contract:

- Prefer **additive** changes:
  - add new optional fields
  - add new component types behind gating/experiments
- Bump `ui_version` only on breaking changes.
- Support at least N-1 versions for a deprecation window.

---

## 4) Realistic `/home` page response (target)

Home should include:

- Hero/banner at top (personalized)
- Rails:
  - Trending (carousel)
  - Continue Watching (carousel)
  - Recommended For You (grid or carousel)
- Per-item actions:
  - navigate to details
  - deep link (optional)
  - API calls (like/add-to-list)
- Pagination for at least one rail via `GET /section/{id}?cursor=...`

---

## 5) Backend implementation tasks (Go BFF + UI Composer)

### Backend module structure (recommended)

Create `backend/` with:

```
backend/
  go.mod
  cmd/
    bff/
      main.go
  internal/
    http/
      router.go
      middleware/
        requestid.go
        logging.go
      handlers/
        home.go
        section.go
        health.go
    composer/
      home.go
      components.go
      schema.go
    clients/
      user.go
      content.go
      reco.go
    models/
      ui.go
      domain.go
    config/
      config.go
```

### Endpoints to implement (v1)

- `GET /healthz`
  - returns 200 + basic info
- `GET /home`
  - inputs:
    - auth token (optional in dev)
    - user_id (header or query in dev)
    - locale
    - capabilities headers (ui version range)
  - output: SDUI envelope JSON
- `GET /section/{id}?cursor=...`
  - output: section payload (either a partial page or a component subtree)

### Downstream services (mock first, real later)

Implement mock clients returning deterministic data:

- `user-service`: profile, maturity, locale
- `content-service`: metadata + artwork URLs
- `reco-service`: ranked content IDs per rail

### UI Composer (core)

Responsibilities:

- Choose layout based on:
  - user profile
  - experiments/flags
  - device capabilities (e.g. grid only if supported)
- Compose:
  - `page` root
  - `hero` (optional)
  - rails as `carousel`/`grid`
- Provide:
  - `ttl_ms`
  - stable IDs for sections and items
  - paging tokens for rails

### Observability (minimum viable)

- Request ID:
  - accept `X-Request-Id` if present, else generate
  - return `X-Request-Id` and embed `trace_id` in response envelope
- Structured logs:
  - method/path/status/latency/request_id/user_id
- Metrics hook:
  - placeholder interface for later Prometheus/OpenTelemetry

---

## 6) Android implementation tasks

### 6.1 Transport migration: Firebase → HTTP

Replace or add an alternative to `FirebaseService`:

- Create a network client layer (Retrofit+OkHttp or Ktor).
- Add repository: `HomeRepository` / `SectionRepository`.
- Update `ScreenViewModel` (or create `HomeViewModel`) to call BFF endpoints.

Acceptance criteria:

- App loads `/home` over HTTP and renders via existing engine pipeline.
- Cache is used for instant render where possible (optional in first pass).

### 6.2 Typed decoding (critical for production)

Current parsing relies on `Map<String, Any>`; replace with typed DTOs:

- Use `kotlinx.serialization` (recommended) or Moshi.
- Define:
  - `SduiEnvelopeDto`
  - `ComponentDto`
  - `ActionDto`
  - `StyleDto`
- Convert DTO → existing `Map<String, Any>` only as a transitional step, or
  parse DTO → `UIComponent` directly (preferred).

Acceptance criteria:

- Can decode the `/home` payload without numeric type bugs.
- Unknown fields do not fail decoding (use defaults / `Json { ignoreUnknownKeys = true }`).

### 6.3 Add Netflix primitives: `page`, `carousel`, `grid`, `hero`

Implement new components:

- `PageComponent`
  - props: title, background, optional app bar, safe-area padding hints
- `CarouselComponent`
  - props: section title, item template, items list, paging action
  - renderer: Compose `LazyRow`
- `GridComponent`
  - props: columns/adaptive sizing, spacing
  - renderer: Compose `LazyVerticalGrid`
- `HeroComponent` (or `banner`)
  - props: image, title, subtitle, primary action

Update:

- `UIComponent` sealed hierarchy
- `ComponentRegistry` built-in parsers
- `SchemaValidator` required props / child support
- `ComposeRenderer` `when` + render functions

### 6.4 Action handling system v2

Current:

- Actions parse, but `handleApiCall()` is TODO.

Implement:

- Action dispatcher that receives `ActionContext`:
  - page/screen id
  - component id
  - form state
  - trace id/request id
  - analytics context
- Support:
  - Navigation (Jetpack Navigation)
  - Deep links (allowlist schemes/hosts)
  - API calls (OkHttp/Retrofit)
  - Pagination (`load_more` → call `/section/{id}` and merge)

Acceptance criteria:

- Clicking a content card navigates to details route with params.
- `load_more` appends items to a rail without full page reload.

---

## 7) Personalization + experiments (backend-led)

### Backend approach

- BFF receives user identity (token / user id).
- BFF queries reco-service for personalized rail ordering and items.
- Experiments:
  - assign user → variant (server-side)
  - composer chooses component layouts based on variant
  - include `experiments` in response envelope

### Android approach

- Log experiment exposure events (impression) with request/trace correlation.
- Use server-provided analytics payloads (do not hardcode event schemas in app).

---

## 8) Performance & payload strategy

Backend:

- Cache:
  - per-user `/home` response cache (short TTL, e.g. 10–60s)
  - per-rail `/section/{id}` cache (longer TTL, e.g. 1–5m)
- Payload:
  - dedupe repeated items
  - include image variants (small/medium/large) + aspect ratio hints
  - compress responses (gzip/br)

Android:

- Lazy rendering:
  - `LazyRow`/`LazyVerticalGrid`
  - stable keys per item
  - Coil caching with correct sizing
- Background prefetch:
  - optional: prefetch first N images per rail

---

## 9) Production hardening checklist

Schema:

- unknown component types:
  - render `UnknownComponent` or `fallback`
- invalid payload:
  - show fallback page or cached page
  - report error (analytics/logging)

Security:

- allowlist action endpoints (prevent arbitrary network calls)
- allowlist deep link schemes/hosts
- sanitize URLs

Observability:

- request_id everywhere
- structured logging in backend
- client logs for parse/validation failures (sampled)

Testing:

- **Contract tests**:
  - golden `/home` JSON fixtures render without crash
  - backend composer snapshot tests to prevent drift

---

## 10) Phase plan (execution order)

### Phase 0 — Baseline & alignment (1–2 days) ✅

- Define target Home UX and acceptance criteria
- Decide action naming compatibility (`navigate` vs `Navigate`)
- Decide transport (HTTP is required; Firebase can remain for preview only)

### Phase 1 — Go backend foundation (2–4 days) ✅

- Create `backend/` module and server
- Implement `GET /home`, `GET /section/{id}`, `GET /healthz`
- Add request_id + structured logs
- Mock downstream service clients

### Phase 2 — SDUI schema v1 (2–3 days) ✅

- Finalize envelope + component shape + action shape
- Implement composer types/models in Go
- Add fallbacks + gating fields

### Phase 3 — UI Composer + realistic `/home` (3–6 days) ✅

- Compose hero + rails (trending/continue/recommended)
- Add paging tokens
- Add caching and response shaping

### Phase 4 — Android HTTP + typed parsing (3–6 days) ✅

- Replace Firebase loading path with HTTP repository
- Add typed DTO decoding (kotlinx.serialization or Moshi)
- Validate + render `/home`

### Phase 5 — Android primitives (4–8 days) ✅

- Implement `page`, `carousel`, `grid`, `hero`
- Update registry/renderer/validator
- Performance tuning (keys, layout, Coil sizing) — ongoing as needed

### Phase 6 — Action system v2 (3–6 days) ✅

- [x] Implement real `api_call` execution (OkHttp + allowlisted endpoints)
- [x] Add deep link allowlist and navigation integration (async `onSuccess`/`onError` via `SharedFlow`)
- [x] Implement `load_more` rail pagination (fetch `/section/{id}` + append into `rail_{id}`)

Phase 6 follow-ups (implemented):

- [x] Tighten allowlist (host/scheme + per-path method allowlist) — `ApiAllowlist` / `ApiActionExecutor`
- [x] UX polish for pagination (loading keys, disable/hide when no `next_cursor`, remove load-more button)
- [x] Standardize API error UX (default toast when `onError` absent; optional `onRetry` on `ApiCall`)
- [x] Action analytics hooks — `ActionAnalytics` + `BffActionAnalytics` (`api_call_*`, taps, impressions)

### Phase 7 — Personalization + A/B ✅ (core shipped; polish ongoing)

- [x] Server-side assignments (stable bucketing) — `backend/internal/experiments`, `experiments` map on envelope
  - [x] Experiment catalog (`home_rail_order`, `hero_variant`, …) in code (config file is a follow-up)
  - [x] Stable bucketing (hash user id + experiment name → variant)
  - [x] Return assignments in envelope (`experiments`)
  - [ ] “Force variant” dev override (query/header) for QA
- [x] Variant-driven composer choices
  - [x] Rail order varies with `home_rail_order`
  - [x] Hero copy varies with `hero_variant` (`applyHeroCopyVariant`)
  - [ ] Explicit client-side gating UI for `gating` blocks (currently informational / safe defaults)
- [x] Exposure/click logging (baseline)
  - [x] Event payload: `event_name`, `screen_id`, `component_id`, `action_type`, `attrs`, `ts_ms`
  - [x] `POST /api/events` (batch) + structured `slog` + validation + rate limit
  - [x] Android: best-effort impressions + taps + API lifecycle events
  - [ ] Backend: durable warehouse + join with assignments (analytics pipeline / Phase 8+)
- [x] Propagate user identity to BFF
  - [x] Android: `UserIdentity` (device-scoped anonymous id) → `X-User-Id`
  - [ ] Android: replace with **real auth** session when available
  - [x] BFF: `X-User-Id` only (no `?user_id=`), format validation in middleware
  - [x] Cache keys include user id (`/home`, `/section`)

### Phase 8 — Production readiness (1–2 weeks) ✅

- [x] Contract tests (schema + golden payloads)
  - [x] `/home` envelope vs `schema/sdui.v1.schema.json`
  - [x] `/section/{id}` response vs `schema/sdui.v1.section.schema.json`
  - [x] Golden snapshot `internal/contract/testdata/golden/home_envelope.golden.json` (regenerate with `GENERATE_GOLDEN=1`)
- [x] Fuzz tests (decoder safety for envelope + section models)
- [x] Security hardening (request body limits per route, `X-User-Id` validation, CORS for dev, **rate limit** on `/api/events`)
- [x] Observability upgrades (`/metrics` with HTTP + latency + cache + events ingested counters; structured `slog` per client event in `POST /api/events`)


### Phase 8 follow-ups (Android + ops)

- [x] Analytics queue persistence + retry backoff (`AnalyticsEventQueue` + `BffActionAnalytics`)
- [x] Session-level impression dedupe (`BffActionAnalytics`)
- [x] JVM unit tests (`ApiAllowlistTest`, `ComponentTreeAppendTest`) — Gradle `test` task may fail on some environments; use `assembleDebug` or `./gradlew :sdui:test` when supported
- [ ] TLS / pinning for production BFF endpoints
- [ ] True viewport-based impressions (Lazy list visibility)

---

## 11) Definition of Done (DoD) for “Netflix-style Home”

Backend:

- `/home` returns personalized SDUI JSON with:
  - hero + ≥3 rails
  - per-item actions
  - rail pagination via `/section/{id}`
  - request_id/trace_id
- `/section/{id}` returns next items and cursor

Android:

- App renders `/home` dynamically using registry pattern
- Supports nested rendering and new primitives (carousel/grid/page/hero)
- Handles:
  - navigation
  - deep links
  - api calls
  - pagination actions
- Invalid/unknown components do not crash (fallback or UnknownComponent)

DoD status (as implemented):

- [x] Pagination via `/section/{id}` + cursor
- [x] Android `api_call` execution + nested `onSuccess`/`onError`
- [x] Navigation integration for async actions
- [x] Personalized **rails order + hero experiments** (server-driven assignments + composer)
- [ ] **Auth-backed** identity and **production-grade** reco/content services (beyond mocks + anonymous id)

