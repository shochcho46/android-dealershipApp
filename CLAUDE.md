# DealerNest

Android "Sales & Collection Management" app for a Laravel backend (auth, Vendors, Products,
Orders, DSR Collections). Kotlin + Jetpack Compose, Material 3, single Gradle module. Built
against a full SRS — bottom-nav (Dashboard/Order/Collection/Vendor/Profile), GPS-gated, QR-driven,
role-based, offline-capable — executed in phases, see `roadmap.md`.

**Read only the doc you need — don't re-read this whole tree for every task:**

- `.claude/docs/architecture.md` — stack, layers, DI graph, design system, why each choice
- `.claude/docs/api-contract.md` — every endpoint, confirmed request/response shapes (source of
  truth over `.claude/postman/inventory-api-collection.json`, which is reference-only) — flags
  what's confirmed vs. what still needs backend confirmation before a later phase builds on it
- `.claude/docs/auth-and-permissions.md` — token lifecycle, `SessionManager`, role model
  (Normal/Admin/SuperAdmin), bottom-nav shell, the Profile-fields-not-in-API gap
- `.claude/docs/coding-conventions.md` — recipe for adding a feature (Vendor module is the
  reference implementation), `NetworkResult`/`UiState` patterns
- `.claude/docs/roadmap.md` — the 6-phase plan, what's done, what's next, open questions each
  phase needs resolved before it starts

## Current phase

**All 6 phases done.** Foundation, Vendor, Profile (1); DSR Collections (2); Products, Orders,
Invoice (3); Dashboard (4); Room read-through cache for Vendors/Products + sync-on-reconnect
(5); notification channel/permission scaffold + nav transition polish (6). Two things were
deliberately *not* built, documented rather than silently skipped: **Paging 3** for
Orders/Collections (existing manual pagination already correctly hits the real API, so
migrating would only swap mechanism, not add capability) and **Lottie animations** (SRS asked
for them, no asset files were ever provided — adding the dependency with nothing to render
would be a half-finished feature). See `roadmap.md` for the full per-phase detail.

Known, documented (not silently wrong) limitations from missing backend endpoints: no "own vs
all" split for Collections/Orders/Dashboard stats, no DSR Collection edit, no "vendors visited"
stat, `country_id`/`payment_method_id` hardcoded and hidden from the UI, order status IDs
unconfirmed, no `paid_amount` input on order creation, **push notifications blocked** pending a
Firebase project/`google-services.json` from the user.

## Setup

Copy `.env.example` to `.env` and set `BASE_URL` to a real backend before running. Plain
`http://` only works for `localhost`/`127.0.0.1`/`10.0.2.2` (see `network_security_config.xml`)
— any other host needs `https://`. Location permission must be granted on first launch (SRS
requirement) — the app blocks past the gate screen until it is.

**Build environment gotchas** (all commented where they're configured, see `architecture.md`
DI section and `gradle.properties`): the Hilt Gradle plugin is intentionally not applied (AGP 9
incompatibility), and two AGP-9/KSP interim workarounds are set in `gradle.properties`. Don't
"clean up" any of these without reading the linked comment first.

## Key files

| What | Where |
|---|---|
| API endpoints | `app/src/main/java/com/example/dealernest/core/network/ApiService.kt` |
| Bearer token injection | `app/src/main/java/com/example/dealernest/core/network/AuthInterceptor.kt` |
| Session/token storage | `app/src/main/java/com/example/dealernest/core/session/SessionManager.kt` |
| Role model | `app/src/main/java/com/example/dealernest/domain/model/RoleLevel.kt` |
| Location gate | `app/src/main/java/com/example/dealernest/presentation/locationgate/` |
| QR scanner | `app/src/main/java/com/example/dealernest/core/qr/QrScannerScreen.kt` |
| Bottom nav shell | `app/src/main/java/com/example/dealernest/presentation/navigation/BottomNavScaffold.kt` |
| Design-system components | `app/src/main/java/com/example/dealernest/presentation/components/` |
| Vendor module (reference pattern) | `app/src/main/java/com/example/dealernest/presentation/vendor/`, `data/vendor/` |
| DSR Collection module | `app/src/main/java/com/example/dealernest/presentation/collection/`, `data/collection/` |
| Order module + cart calculator | `app/src/main/java/com/example/dealernest/presentation/order/`, `data/order/` |
| Invoice screen | `app/src/main/java/com/example/dealernest/presentation/order/InvoiceScreen.kt` |
| Dashboard | `app/src/main/java/com/example/dealernest/presentation/home/DashboardScreen.kt`, `DashboardViewModel.kt` |
| Bar chart component | `app/src/main/java/com/example/dealernest/presentation/components/SimpleBarChart.kt` |
| Offline cache (Room) | `app/src/main/java/com/example/dealernest/core/database/` |
| Connectivity/sync-on-reconnect | `app/src/main/java/com/example/dealernest/core/network/ConnectivityObserver.kt` |
| Notification channel/permission | `app/src/main/java/com/example/dealernest/core/notifications/NotificationHelper.kt`, `presentation/home/DashboardScreen.kt` |
| Nav routes/graph | `app/src/main/java/com/example/dealernest/presentation/navigation/` |
| Login screen/VM | `app/src/main/java/com/example/dealernest/presentation/auth/` |
