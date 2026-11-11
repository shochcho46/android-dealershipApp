# Auth & Permissions

## Token lifecycle

1. `LoginScreen` submits `email_or_phone` + `password` via `LoginViewModel` → `AuthRepository.login()`.
2. `AuthRepositoryImpl.login()` calls `POST v1/admin/login`. The response **confirmed** inlines
   the full admin object (`data.admin`) alongside `data.token` — there is no separate profile
   call needed on login. `sessionManager.saveToken()` is still called first (before
   `saveProfile()`), since `AuthInterceptor` needs the token for any call made in between.
3. `SessionManager.sessionState: Flow<SessionState>` (`Loading / LoggedOut / LoggedIn(profile)`)
   is what the UI observes. `SplashScreen` collects it once to decide whether to start at
   `Routes.LOGIN` or the bottom-nav shell; `BottomNavScaffold`/`ProfileScreen` collect it
   continuously.
4. Logout: `AuthRepository.logout()` calls `POST v1/admin/logout` best-effort (failure ignored —
   a dead network shouldn't trap the user in a logged-in-looking state), then always calls
   `SessionManager.clearSession()`; the caller navigates back to `Routes.LOGIN` clearing the
   back stack. The logout action lives in `ProfileScreen` (moved there in Phase 1 — there's no
   drawer anymore).
5. Any 401 from any authenticated call → same as logout: clear session, navigate to Login. When
   adding a new repository method, check `NetworkResult.Error.isUnauthorized` and call
   `sessionManager.clearSession()` if true (see `coding-conventions.md` recipe).

## `SessionManager` (`core/session/SessionManager.kt`)

```kotlin
interface SessionManager {
    val sessionState: Flow<SessionState>
    fun currentTokenBlocking(): String?      // used only by AuthInterceptor
    suspend fun saveToken(token: String)
    suspend fun saveProfile(profile: AdminProfile)
    suspend fun clearSession()
}
```

Backed by one `DataStore<Preferences>` with keys for the raw token and a JSON-serialized
`AdminProfile`. One manager for both: the bottom-nav/profile UI needs the profile and the
network layer needs the token — keeping them together avoids two sources of truth going out of
sync (e.g. token present but no profile after a crash).

## Role model (`domain/model/RoleLevel.kt`)

The SRS's access rules are role-based, not permission-slug-based — replaces the Phase 0
speculative slug system now that the real API confirms `AdminObject.roles: [{id, name,
guard_name}]` (see `api-contract.md`).

```kotlin
enum class RoleLevel { NORMAL, ADMIN, SUPER_ADMIN }

fun List<String>.toRoleLevel(): RoleLevel = when {
    any { it.equals("superadmin", ignoreCase = true) } -> RoleLevel.SUPER_ADMIN
    any { it.equals("admin", ignoreCase = true) } -> RoleLevel.ADMIN
    else -> RoleLevel.NORMAL
}
```

`AdminProfile.roleLevel` is derived from `roles` this way. **The exact role name strings
("SuperAdmin" vs "super_admin" vs "super-admin" etc.) are not confirmed** — the SRS just says
"SuperAdmin"/"Admin"/normal user, the API doc doesn't show example role names. Confirm the exact
strings against a real login response before Phase 2/3 rely on this for Edit/Delete gating, and
update `toRoleLevel()`'s matching if they differ (currently case-insensitive substring-free exact
match on "admin"/"superadmin" - loosen or tighten once confirmed).

**How each phase uses it** (per the SRS):
- **Vendor** (Phase 1): no role gating — any authenticated user can view/create/edit vendors.
- **DSR Collection** (Phase 2): Normal = no edit/delete; Admin/SuperAdmin = both. List should
  also filter to "own" collections for Normal users vs "all" for Admin/SuperAdmin — but the
  List endpoint's query params (`vendor_id`, `payment_method_id`, `date_from`, `date_to`) have
  **no `created_by`/`deposite_by` filter** in the API docs, so "own only" for Normal users may
  need client-side filtering against `collection.created_by.id == currentUser.id`, or a backend
  change — resolve when Phase 2 starts.
- **Order** (Phase 3): same "own vs all" caveat — `GET /v1/orders/by-placed-by` already takes a
  `place_by` param, so "own" is natural (pass the current user's id); "all" for Admin/SuperAdmin
  needs a different endpoint or `place_by` omitted — the current docs only show the
  by-placed-by endpoint, confirm an "all orders" endpoint exists before Phase 3 relies on it.
- **Sales Performance** (Phase 4 follow-up): a separate, narrower role check —
  `List<String>.canSeeAllSalesPerformance()` in `RoleLevel.kt` — decides whether `admin_id` is
  sent to `GET v1/admin/sales-performance`. True (Director/Admin/SuperAdmin/Subadmin, matched
  directly against the role strings, case/separator-insensitive) → **omit** `admin_id`, see
  everyone. False (any other role) → **send** the caller's own id as `admin_id`, see only their
  own details. Independent of the 3-value `RoleLevel` enum above, since Director/Subadmin aren't
  values that enum has. See `api-contract.md` "Sales Performance" for the full request/response
  rule.

## Profile fields gap

The SRS's Profile screen spec lists Employee ID, Address, Joining Date, Last Login. The real
`AdminObject` (confirmed in `api-contract.md`) does not return any of these — only `id, name,
email, phone, status, picture, roles, permissions, created_at, updated_at`. `ProfileScreen`
shows exactly these confirmed fields (using `created_at` as a "member since" line, not a
fabricated "joining date" field) and does not display Employee ID/Address/Last Login. If the
backend adds these fields later, extend `AdminProfileDto`/`AdminProfile` and the screen then —
don't invent placeholder values now.

## Bottom navigation (`presentation/navigation/`, replaces the Phase 0 drawer + `AppMenu`)

Fixed 5 tabs per the SRS — **not** permission/role-gated (every logged-in user sees all 5 tabs;
what's role-gated is actions *inside* a module, per the role model above):

```kotlin
enum class BottomNavTab(val route: String, val label: String, val icon: ImageVector) {
    Dashboard(Routes.DASHBOARD, "Dashboard", Icons.Filled.Dashboard),
    Order(Routes.ORDERS, "Order", Icons.Filled.ShoppingCart),
    Collection(Routes.COLLECTIONS, "Collection", Icons.Filled.Payments),
    Vendor(Routes.VENDORS, "Vendor", Icons.Filled.Store),
    Profile(Routes.PROFILE, "Profile", Icons.Filled.Person),
}
```

`BottomNavScaffold` renders a Material 3 `NavigationBar` with these 5 entries; `MainScaffold.kt`/
`AppMenu.kt`/`MenuItem.kt` from Phase 0 are deleted, not deprecated in place.

### Adding a new destination inside an existing tab

Add a route constant in `Routes.kt`, add the composable to `DealerNestNavHost.kt` nested under
that tab's graph. The 5 top-level tabs themselves don't change — only the SRS or the user
changes those.
