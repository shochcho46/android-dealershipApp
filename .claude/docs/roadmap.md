# Roadmap

Supersedes the original Phase 0-4 plan — the user handed over a full SRS ("Sales & Collection
Management" app: bottom-nav Dashboard/Order/Collection/Vendor/Profile, GPS-gated, QR-driven,
role-based, offline-capable) plus an expanded Postman collection (adds a "DSR Collections"
module) and a Laravel API doc with exact response shapes. This is the new authoritative,
multi-phase plan.

## Phase 0 — Foundation + Auth (done)

Gradle/env setup, networking core, session storage, Login/Logout. Superseded pieces (drawer nav,
permission-slug menu model) were replaced in Phase 1 once the SRS gave a concrete role model and
a fixed bottom-nav requirement — see `auth-and-permissions.md` and `architecture.md`.

## Phase 1 — Foundation-for-the-SRS + Vendor module (done)

- Design-system component library (`presentation/components/`): gradients, cards, badges,
  skeleton/empty/error states, search field, buttons - every later phase reuses these instead of
  hand-rolling styling per screen.
- Location permission gate (`core/location/`, `presentation/locationgate/`) - the app is unusable
  until location is granted, per the SRS. Sits before `Routes.SPLASH` in the nav graph.
- QR scanner (`core/qr/`) - CameraX + ML Kit, reusable across Vendor/Collection/Order.
- Role model (`domain/model/RoleLevel.kt`) - Normal/Admin/SuperAdmin resolved from
  `AdminProfile.roles`, replacing the old speculative permission-slug system.
- Bottom-nav shell (`presentation/navigation/BottomNavScaffold.kt`) - fixed 5 tabs
  (Dashboard/Order/Collection/Vendor/Profile), always visible; in-module actions are what's
  role-gated, not the tabs.
- Vendor module, full vertical slice: search (text + QR, shared state), pull-to-refresh,
  "load more" (the API has no true pagination for vendor search - see `api-contract.md`),
  details, create/edit with silent GPS capture (no manual lat/long fields) and image upload.
- Profile module, real and read-only, showing only the fields the API actually returns (see the
  "Profile fields gap" note in `auth-and-permissions.md`).

## Phase 2 — DSR Collection module (done)

- List with date-range filters, the API's real pagination (`page`/`per_page`) plus
  `meta.filtered_total_amount`. `CollectionListScreen`/`CollectionListViewModel`.
- Create: step 1 select vendor (search box or QR, via the collection-scoped
  `/v1/dsr-collections/vendors/search` endpoint - `VendorLiteDto`, not `VendorDto`), step 2
  amount + optional note; `collection_date` defaults to today (`todayApiDateString()`).
  `payment_method_id` resolved by hardcoding `1` (no list endpoint exists - same treatment as
  Vendor's `country_id`), never shown in the UI. `CollectionCreateScreen`/`ViewModel`.
- Delete gated by `RoleLevel != NORMAL` (confirmation dialog first). **No Edit** - the API docs
  only expose Create/Delete for DSR collections, no update endpoint.
- **Known limitation, not fixed**: no "own vs all" split - the list endpoint has no
  `created_by` filter, so every role currently sees the same list; only Delete visibility is
  role-gated. The list screen shows an inline note saying so. Revisit if the backend adds a
  filter.

## Phase 3 — Order module + Invoice (done)

- Product search (`domain/model/Product.kt`, `ProductRepository`, `/v1/products/search`) -
  `Product.minSellPrice` is the lowest `stocks[].sell_price`, enforced as a floor on cart rows.
- `OrderCartLine` (text-backed fields, not Double, so typing never gets reformatted mid-edit;
  clamping happens in computed properties only) + `OrderFormViewModel`/`OrderFormScreen` -
  vendor picker (search/QR, reuses the general Vendor search, not Collection's lite one) →
  product autocomplete → editable rows ("add more" supported) → live Subtotal/Discount/Grand
  Total summary → submit.
- Status-based edit lock uses the SRS's stated IDs (2/3 editable, 4/5 read-only via
  `Order.isEditable`) - **not confirmed** against the real `order_statuses` table (API doc
  doesn't enumerate it), flagged in `api-contract.md`. Cancel button instead gates on the
  response's own `can_be_cancelled` boolean where possible.
- **No `paid_amount` input exists on Create/Update Order** (request body is only
  `vendor_id`/`place_by`/`items`) - the Create/Edit form's summary shows Subtotal/Discount/Grand
  Total only; Paid/Due amounts are read-only, shown on already-created orders (Invoice, list).
- `OrderListScreen` - same "own records" limitation as Collections (`place_by` requires a
  specific user id, no "all" mode), date-range filter, cancel action gated on
  `can_be_cancelled`, Edit icon gated on `isEditable`.
- `InvoiceScreen` - gradient header, vendor block, line-item table, summary, Paid/Partial/Unpaid
  badge derived from `paid_amount`/`due_amount` (the API's `payment_status` int isn't documented
  beyond `0`="Unpaid", so amounts are used instead of trusting unknown int codes).

## Phase 4 — Dashboard + polish

## Phase 4 — Dashboard (done)

No aggregate/stats endpoint exists in the API - `DashboardViewModel` derives everything
client-side from the same list endpoints Phases 2/3 already use (`OrderRepository`,
`DsrCollectionRepository`, `VendorRepository`), fetched concurrently via `coroutineScope`/`async`:

- **Today's Statistics**: Orders/Sales/Products Sold are **the current user's own** (the
  Orders API requires a specific `place_by`, same "own vs all" limitation as Phase 3);
  Collections total is **company-wide** (the Collections API has no per-user filter at all -
  same limitation as Phase 2). The two stat rows are labeled "(yours)" vs "(all)" on-screen so
  this asymmetry reads as intentional, not a bug.
- **Total Vendors Visited is omitted entirely** - no visit-tracking endpoint exists anywhere in
  the API docs to derive it from. Not faked with a zero or placeholder.
- **Weekly Sales/Collection Trend charts**: resolved the deferred chart-library decision by not
  needing one - `presentation/components/SimpleBarChart.kt` is plain Compose layout (a
  fixed-height `Box` per bar with `fillMaxHeight(fraction)`), not a charting library or raw
  Canvas math. Data comes from one 7-day-range API call each (not 7 separate calls), bucketed by
  day client-side.
- **Quick Actions** (Create Order/Collection/Vendor, Scan Vendor QR) and **Recent Activity**
  (latest 5 orders/collections) work exactly as planned, no gaps.
- **Scan Vendor QR** searches by the scanned value (limit 1) and navigates straight to the
  vendor's details if found; otherwise shows a snackbar - there's no dedicated
  "resolve QR to vendor" endpoint, so this reuses the existing vendor search.

## Phase 5 — Offline cache + list polish (done)

- **Room cache for Vendors/Products** (`core/database/`) - not Profile, which was already
  durably cached via `SessionManager`/DataStore since Phase 0, so Room would only duplicate it.
- The API has no "get all vendors"/"get all products" endpoint, only `search`+`getById`, so this
  is a **read-through cache**, not a full offline-first dataset: every successful fetch upserts
  by `id`; a network failure (`NetworkResult.Exception` - no connectivity) falls back to a
  `LIKE`-matching query against the cached rows. `NetworkResult.Success.isFromCache` (new field,
  defaults `false` so every existing call site kept compiling unchanged) flags this so
  `VendorListScreen` and the Order form's product search can show "Showing cached results —
  you're offline."
- **Sync-on-reconnect**: `ConnectivityObserver` (`core/network/`) wraps
  `ConnectivityManager.NetworkCallback` in a `Flow<Boolean>`; `VendorListViewModel` refreshes on
  a false→true transition - the concrete "sync automatically when internet returns."
- **Paging 3 - deliberately not adopted.** `OrderListViewModel`/`CollectionListViewModel`
  already implement correct manual pagination against the real `page`/`per_page` API. Migrating
  to Paging 3 would only swap the mechanism for equivalent capability, risking two
  already-verified screens for no functional gain. Revisit only if infinite-scroll/prefetch UX
  is specifically requested.

## Phase 6 — Push notifications (partial) + final polish (done)

- Push notifications **stay blocked** on a Firebase project - not attempted without
  `google-services.json`. What's buildable without it: `NotificationHelper` creates the
  notification channel on app start (`DealerNestApp.onCreate()`), and `DashboardScreen` requests
  the Android 13+ `POST_NOTIFICATIONS` runtime permission once. Wiring in
  `FirebaseMessagingService` is the only remaining step once credentials exist.
- **Lottie - deliberately not added.** The SRS asks for it but no animation asset files were
  provided; adding the dependency with nothing to render would be a half-finished
  implementation. Final polish uses Compose's own animation APIs instead:
  `DealerNestNavHost`'s `NavHost` got a fade+slide `enterTransition`/`exitTransition` pass - one
  change point, consistent across every screen, rather than piecemeal per-screen work.
- Printable/exportable invoice - not built; no export mechanism (PDF/share) was specifically
  requested beyond the SRS's original "should look professional enough to print or export
  later" aside, and `InvoiceScreen`'s layout is already a `SectionCard`-based, print-friendly
  design if that's picked up later.

## Out of scope until asked

Multi-module Gradle split, full offline-first write queueing (beyond simple cache+refetch),
anything not traceable to the SRS + the provided API docs.
