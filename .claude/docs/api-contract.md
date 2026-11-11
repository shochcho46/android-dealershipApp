# API Contract

Sources of truth: `.claude/postman/inventory-api-collection.json` (request shapes) and the
Laravel API markdown doc the user provided alongside the SRS (response shapes - not stored as a
separate file, but every field below is transcribed from it, not guessed). This doc normalizes
both so you don't have to re-derive them per feature — read this instead.

Base URL comes from `.env` → `BuildConfig.BASE_URL`. All paths below are **relative** to that
base URL and already include the `v1/` prefix (`base_url` in Postman = `.../api`, then
`v1/...` is appended — our `BASE_URL` should include the trailing `/api/`).

## Null-safety audit (2026-07-21, live testing)

Two real crashes surfaced fields where the docs' JSON examples didn't match the live backend:
`admin.status` decoded as `Int` when the real API sends a JSON **boolean** (see the Authentication
section below), and `payment_method.name` decoded as a non-null `String` when the real API sent
`null`. Neither was guessable from the docs alone - the examples always showed a populated
value, and admin's `status` looked identical to Vendor/Product's int `status` fields.

Rather than patch fields one crash at a time, every DTO in `data/*/dto/` was audited and every
**leaf string field that isn't a primary key** was made nullable with a fallback in its
`toDomain()` mapper (e.g. `shopName ?: "Unnamed Vendor"`, money fields `?: "0.00"`) - Laravel
nullable `varchar` columns can send `null` regardless of what a doc's example happened to show.
**Left required** (no fallback, will still throw if actually missing): `id` fields (real DB
primary keys, `NOT NULL` by definition) and structural relations that represent a `NOT NULL`
foreign key with no sensible placeholder (`DsrCollectionDto.vendor`, `OrderDto.vendor`,
`OrderItemDto.product`, `LoginResponseDto.admin`/`token`) - if one of *those* is ever missing,
that's a deeper data-integrity problem a UI fallback would only mask.

This does **not** mean every numeric/boolean field is safe by the same logic - `admin.status`
being a bool instead of an int was a genuine type mismatch, not a missing-value one, and nothing
else has been confirmed to have the same problem. If another field throws a similar
`JsonDecodingException`, check the real response's JSON type first rather than assuming the same
fix applies everywhere.

**Update (same day, third crash):** `created_by: {"id": null, "name": null, "email": null}` -
this backend null-shells entire nested reference objects (every field null, including `id`)
instead of returning `null` for the relation itself. So `id` fields on nested/related objects
aren't safe to assume non-null either, only the *top-level* row's own `id` (the primary key of
the thing actually being fetched/listed) is. Made nullable as a result: `RoleDto.id`,
`PermissionDto.id`, `VendorCountryDto.id`, `ProductLookupDto.id`, `OrderAdminDto.id`,
`OrderStatusDto.id`, `PaymentMethodDto.id`, `CollectionAdminDto.id` (the confirmed crash),
`CollectionVendorDto.id`, `OrderVendorDto.id` - all cascaded to nullable `vendorId` on the
`DsrCollection`/`Order` domain models (safe: grepped first, confirmed neither field is read
anywhere in the app besides an unrelated filter param of the same name).

**Deliberately left required** despite that pattern: `OrderItemProductDto.id` and, downstream,
`OrderVendorSelection.id` (both actively used to resubmit `product_id`/`vendor_id` when
editing/updating an order - see `OrderRepositoryImpl`/`OrderFormViewModel.submit()`). A crash on
missing data is safer than silently sending a fabricated id to a write endpoint. If either ever
null-shells in practice: `OrderFormViewModel.submit()` already refuses to submit with a clear
error when the vendor id is missing; the product-id case doesn't have that guard yet (no
evidence it's needed) - add the same pattern there if it ever crashes.

**Update (same day, fourth crash + a structural fix):** `VendorDto.status` is *also* a JSON
boolean, not an int - confirmed 2-for-2 now (Admin, Vendor), so `ProductDto.status` was fixed to
`Boolean` too pre-emptively even without its own confirmed crash, since it's the same field name
on the same backend. This is genuinely a different bug class than the null-shelling above - a
type mismatch, not a missing value - and there's no way to guess which fields it applies to next
without hitting them.

That's exactly why `core/network/SafeApiCall.kt` now also catches
`kotlinx.serialization.SerializationException` (the parent of `JsonDecodingException`) and
converts it to a normal `NetworkResult.Error` instead of rethrowing it to crash the whole app.
Every one of these four crashes was a `SerializationException` reaching all the way to the
Android runtime uncaught - fixing each field stops that *specific* crash, but this stops the
*next unknown one* from taking the whole app down too. It still won't fix the underlying data
problem (the screen will show "unexpected format" and a retry), but that's a recoverable error
state instead of `FATAL EXCEPTION`.

Also fixed alongside this: `OrderFormViewModel`'s and `CollectionCreateViewModel`'s vendor/
product search functions were swallowing *all* errors (network failures, and previously these
crashes) into a bare empty list with no message - which is why failed searches looked like
"no results" with zero feedback rather than an visible error. They now surface
`NetworkResult.Error`/`Exception` messages the same way every other screen in the app already
did; this was a real, independent bug, not a consequence of the DTO issues above.

## Envelope

```json
{ "success": true, "message": "...", "data": { /* T */ } }
```

422s: `{ "success": false, "message": "...", "errors": { "field": ["reason"] } }`. Paginated
list endpoints (Orders-by-placed-by, DSR Collections) add a `meta` object alongside `data`
(current_page/last_page/per_page/total, plus DSR Collections adds
`filtered_total_amount`/`total_all_time`). Auth failures (401) have no guaranteed body — treat
any 401 as "session invalid" regardless of body content.

## Authentication (Phase 0 — implemented)

| Endpoint | Method | Path | Auth | Request | Response `data` |
|---|---|---|---|---|---|
| Admin Login | POST | `v1/admin/login` | none | `{ email_or_phone, password }` | `{ admin: AdminObject, token, token_type: "Bearer" }` — **confirmed**, login inlines the full admin object, so `AuthRepositoryImpl` never needs a separate profile call after login. |
| Admin Logout | POST | `v1/admin/logout` | Bearer | — | — (best-effort; local session clears regardless of this call's outcome) |
| Get Admin Profile | GET | `v1/admin/profile` | Bearer | — | Same `AdminObject` shape as login's `admin` field. |

**AdminObject (confirmed fields — this is the complete set the API returns):**
`id, name, email, phone, status (bool), status_text, picture: {url, thumb_url}, roles: [{id, name, guard_name}], permissions: [{id, name, guard_name}], created_at, updated_at`.

**Correction (found via live testing, 2026-07-21):** `status` on the admin object is a JSON
**boolean** (`true`/`false`), not an int like Vendor/Product's `status` fields — the markdown
doc's examples showed `"status": 1` for everything, but the real response for admin sends
`true`/`false`. `AdminProfileDto.status`/`domain.model.AdminProfile.status` are `Boolean`. This
was a real crash (`JsonDecodingException` decoding `Int`) before being fixed — if any other
`status`-shaped field throws the same error, check its real JSON type before assuming it matches
this doc's examples; don't assume Vendor/Product are also secretly booleans without evidence.

**Profile fields gap**: the SRS's Profile screen asks for Employee ID, Address, Joining Date,
Last Login — **none of these exist in the API response above**. The Profile screen shows only
the confirmed fields; `created_at` is used as a "member since" stand-in, not a real joining
date field. Don't add fake fields to `AdminProfileDto` to fill this gap.

**Role model**: `roles[].name` is the only source for `RoleLevel` (Normal/Admin/SuperAdmin) —
see `auth-and-permissions.md`. `permissions[]` exists in the payload but the SRS's actual access
rules are role-based ("Admin can edit/delete", "SuperAdmin can do everything"), not
permission-slug-based — `permissions[]` is carried through the DTO for completeness but nothing
currently gates on it.

## Products (Phase 3 — not yet implemented)

| Endpoint | Method | Path | Query/Body | Response `data` |
|---|---|---|---|---|
| Search Products | GET | `v1/products/search` | `search`, `limit` (default 10, max 50) | array of Product objects + top-level `count` |
| Get Product Details | GET | `v1/products/{id}` | — | one Product object |

**Product object (confirmed):** `id, name, description, color: {id,name}, company: {id,name},
unit: {id,name}, measurement_unit_name, measurement_unit_number, package_unit_name,
package_unit_quantity, discount_type, discount_amount, status, status_text,
image: {url, thumb_url}, quantity_available (int), sell_price (string decimal),
stocks: [{stock_id, warehouse_id, batch_id, purchase_price, sell_price, available_quantity,
manufacture_date, expire_date}], created_at, updated_at`. No pagination — `limit` only.

## Vendors (Phase 1 — implemented)

| Endpoint | Method | Path | Body | Response `data` |
|---|---|---|---|---|
| Search Vendors | GET | `v1/vendors/search` | `search`, `limit` (default 10, **max 50**) | array of Vendor objects + top-level `count` |
| Get Vendor Details | GET | `v1/vendors/{id}` | — | one Vendor object |
| Create Vendor | POST | `v1/vendors` | multipart: `shop_name*, contact_person*, email, mobile*, country_id*, full_address*, lat, long, status (default 1), image(file, jpeg/png/jpg/gif/webp, max 2MB)` | created Vendor object (201) |
| Update Vendor | POST | `v1/vendors/{id}` | multipart, same fields + `_method=PUT` override (Laravel convention — multipart can't be sent as a true PUT from most clients) | updated Vendor object (200) |

`*` = required per the markdown doc's field descriptions.

**Vendor object (confirmed):** `id, uuid, shop_name, contact_person, email, mobile,
country: {id, name, iso}, address: {full_address, lat, long, location_status, location_status_color},
status, status_text, image: {url, thumb_url}, due_balance, old_due, total_credit, total_debit
(all string decimals), created_at, updated_at`.

**`address.location_status`/`location_status_color`** are server-computed - `"yes"`/`"no"`
(lowercase) for whether this vendor has GPS coordinates on file, plus a ready-to-use hex color
(e.g. `#ff0000`). Mapped straight through to `Vendor.locationStatus`/`locationStatusColor`
(`Vendor.hasLocationSet` derives the boolean) and shown as a colored Yes/No chip on the Vendor
list card, so vendors still needing a location update are visible at a glance. Cached in Room
(`VendorEntity`, DB version 2) alongside the rest of the vendor row.

**No true pagination** — `search`+`limit` only, capped at 50. `VendorListViewModel`'s
"load more" bumps `limit` and re-fetches; there is no page cursor. Do not build Paging 3 for
this endpoint — see `roadmap.md` Phase 5 note.

**GPS**: `lat`/`long` are accepted by Create/Update but the SRS forbids manual entry — the app
captures them via `LocationProvider.getCurrentLocation()` immediately before submit (see
`architecture.md`).

**`country_id` has no picker data source** — the API docs don't include a countries list/search
endpoint, only that Create/Update Vendor requires a numeric `country_id`. Resolved the same way
as `payment_method_id` below: `VendorFormViewModel` hardcodes `18` (the Postman example's value,
presumably Bangladesh), never shown in the UI. Replace with a real picker once a countries
endpoint exists.

## Orders (Phase 3 — not yet implemented)

| Endpoint | Method | Path | Body/Query | Response `data` |
|---|---|---|---|---|
| Create Order | POST | `v1/orders` | `{ vendor_id, place_by, items: [{ product_id, quantity, sell_price, discount_price }], latitude?, longitude? }` | full Order object (201) |
| Update Order | PUT | `v1/orders/{id}` | same shape as Create | full Order object (200) — 403 if the order's status doesn't allow updates (`can_be_cancelled == false`) |
| Get Order Details | GET | `v1/orders/{id}` | — | full Order object |
| Get Orders by Placed-By User | GET | `v1/orders/by-placed-by` | `place_by*`, optional `status_filter`, `vendor_filter`, `date_from`, `date_to`, `page`, `per_page` (default 15, max 100) | array of Order objects + `meta` (current_page/last_page/per_page/total) |
| Cancel Order | POST | `v1/orders/{id}/cancel` | — | — ; 403 if not cancellable |

**Order object (confirmed):** `id, invoice_id, admin: {id,name,email}, vendor: {id, shop_name,
contact_person, mobile, email, full_address, due_balance}, location: {order_latitude,
order_longitude, vendor_latitude, vendor_longitude, has_location_data, distance_from_vendor,
distance_in_meters, google_maps_url}, placed_by: {id,name,email}, order_status: {id, name, color},
total_amount, paid_amount, due_amount (string decimals), total_quantity, total_discount_amount,
total_return_quantity, total_damage_quantity, total_lost_quantity, payment_status (int),
payment_status_text, total_profit, can_be_cancelled (bool), items: [{id, product: {id,name,image},
quantity, purchase_price, sell_price, total_price, discount_price, profit}], paid_at, created_at,
updated_at`.

**`location` (new) is entirely server-computed** — the app never calculates distance or builds
the maps URL itself. Only `has_location_data`, `distance_from_vendor`, and `google_maps_url` are
parsed (`OrderLocationDto` in `data/order/dto/`, mapped to `Order.hasLocationData`/
`distanceFromVendor`/`googleMapsUrl`); `order_latitude`/`order_longitude`/`vendor_latitude`/
`vendor_longitude`/`distance_in_meters` are deliberately left unparsed since their JSON type isn't
confirmed (unlike Vendor's `address.lat`/`long`, which are confirmed strings) and the UI doesn't
need raw coordinates — only a distance string and a tap-to-open maps link, shown as a pill on the
Order list card when `google_maps_url` is present.

**Create/Update Order's `latitude`/`longitude`** are new, GPS-captured the same way as Vendor
(`LocationProvider.getCurrentLocation()` immediately before submit — see `OrderFormViewModel.
submit()`), sent as strings, omitted when capture fails or permission isn't granted. Field naming
mirrors the response's `order_latitude`/`order_longitude` rather than Vendor's `lat`/`long` — not
independently confirmed against the backend for this endpoint.

**Order status IDs**: the SRS names 2=Pending, 3=Confirmed (editable), 4=Shipped, 5=Delivered
(read-only) — the API doc doesn't enumerate the full `order_statuses` table, so confirm these
IDs/names against the real backend before hardcoding the edit-lock logic in Phase 3.

**No `paid_amount` input on Create/Update Order** — the request body is only
`{ vendor_id, place_by, items }`; `paid_amount`/`due_amount` only appear in the *response*.
There's no field in the documented API to record a payment at order-creation time, so the
Order form's summary shows Subtotal/Discount/Grand Total only while creating — Paid/Due amounts
are display-only, read from an existing order's response (Order details, Invoice). If the SRS's
"Paid Amount" input is truly required at creation, that needs a new backend field first.

## DSR Collections (Phase 2 — not yet implemented)

Vendor payments recorded **without an invoice** (distinct from Order payments). Note the vendor
search here is a **different, smaller-field endpoint** than `v1/vendors/search` — don't reuse
`VendorDto` for it, use a lite DTO.

| Endpoint | Method | Path | Body/Query | Response `data` |
|---|---|---|---|---|
| List DSR Collections | GET | `v1/dsr-collections` | optional `vendor_id`, `payment_method_id`, `date_from`, `date_to`, `page`, `per_page` (default 15, max 100) | array of Collection objects + `meta` (current_page/last_page/per_page/total/**filtered_total_amount**/**total_all_time**) |
| Create DSR Collection | POST | `v1/dsr-collections` | `{ vendor_id*, payment_method_id*, amount* (min 0.01), collection_date* (YYYY-MM-DD), note? (max 1000 chars) }` | created Collection object (201) |
| Get DSR Collection Details | GET | `v1/dsr-collections/{id}` | — | one Collection object |
| Delete DSR Collection | DELETE | `v1/dsr-collections/{id}` | — | — ; 403 if not SuperAdmin/Admin |
| Search Vendors (collection-scoped) | GET | `v1/dsr-collections/vendors/search` | `search`, `limit` (default 10, max 50) | array of lite Vendor objects + top-level `count` |

**Collection object (confirmed):** `id, vendor: {id, shop_name, contact_person, mobile, email,
full_address, due_balance}, payment_method: {id, name, account_name}, amount,
collection_date, note, created_by: {id,name,email}, deposite_by: {id,name,email}, created_at,
updated_at`.

**Lite Vendor object (collection-scoped search only):** `id, shop_name, mobile, full_address,
contact_person, due_balance` — smaller than the general Vendor object above, no image/country/
credit-debit fields.

**No update endpoint** in the API docs — only Create/Delete. If Phase 2 needs "Edit", confirm
with the backend first; don't assume PUT `/v1/dsr-collections/{id}` exists.

**`payment_method_id` source unresolved**: the SRS says don't show it in the UI and use "a
default value internally", but there's no payment-methods list endpoint in the docs to resolve
what that default *is*. Resolve this before building Create Collection in Phase 2 — don't
hardcode a guessed id.

## Sales Performance (Phase 4 follow-up — implemented)

Cross-user metrics - not the client-side-derived personal stats the rest of the Dashboard uses.
Called for **every** logged-in role now (`DashboardViewModel`), scoped by an `admin_id` query
param:

- Roles matching Director/Admin/SuperAdmin/Subadmin (`RoleLevel.canSeeAllSalesPerformance()` in
  `RoleLevel.kt` - a role-string check separate from the 3-value `RoleLevel` enum used elsewhere,
  since Director/Subadmin aren't modeled there) **omit `admin_id` entirely** and see everyone's
  details.
- Every other role passes **their own `admin_id`** and the backend scopes the response to just
  their own details.

This replaces the old behavior (gated to `RoleLevel.ADMIN`/`SUPER_ADMIN`, skipped entirely for
Normal users to avoid a guaranteed 403) - the endpoint no longer 403s for any role.

| Endpoint | Method | Path | Body/Query | Response `data`/`meta` |
|---|---|---|---|---|
| Get Sales Performance | GET | `v1/admin/sales-performance` | optional `date_from`, `date_to` (YYYY-MM-DD, defaults to current month, max 1 year range), optional `admin_id` (int - see role rule above) | array of per-user entries + system-wide `meta` |

**Entry object (confirmed):** `id, name, role, image, sales_target, sales: {amount,
order_count, collections_received, due_amount, collection_percentage},
individual_collections: {from_current_period_orders, from_previous_period_orders},
dsr_collections, target_metrics: {completion_percentage, amount_remaining, status} | null`.
`target_metrics` is null for DSR users (no target); `status` is one of "Achieved", "In
Progress", "No Target Set".

**`meta` shape here is not `PageMeta`** — no pagination cursors, just system-wide totals
(`date_from, date_to, total_users, total_orders_amount, total_collections_current_period,
total_collections_previous_period, total_due_amount, collection_percentage_current_period,
due_percentage`). Modeled as its own `SalesPerformanceResponseDto`
(`data/salesperformance/dto/`), not `ApiEnvelope`/`PaginatedEnvelope`.

## Adding an endpoint

When a feature's phase comes up: add the method to `ApiService.kt`, add request/response DTOs
next to the feature's repository impl, map to a domain model — see the recipe in
`coding-conventions.md`.
