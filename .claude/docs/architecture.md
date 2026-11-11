# Architecture

## Stack

| Concern | Choice | Why |
|---|---|---|
| Language/UI | Kotlin 2.2.10, Jetpack Compose, Material 3 | already the project template |
| DI | Hilt 2.57.1 (KSP, not kapt) | standard for Android, best tooling; KSP is faster and required-in-spirit under the K2 compiler |
| Networking | Retrofit 3.0.0 + OkHttp 5.4.0 + kotlinx.serialization | no Gson/Moshi reflection; DTOs are plain `@Serializable` data classes |
| Async | Kotlin Coroutines + `Flow` | repository → ViewModel → Compose, all suspend/Flow based |
| Local storage | DataStore Preferences 1.2.1 | actively maintained (EncryptedSharedPreferences is effectively frozen in alpha); app-private storage is already OS-sandboxed. If stronger at-rest encryption is ever required, wrap the same `SessionManager` interface around a Tink-encrypted DataStore later — nothing above it needs to change. |
| Navigation | Navigation-Compose 2.9.7, single Activity | standard, works with Hilt via `hilt-navigation-compose` |
| State | MVVM: `StateFlow<UiState>` per screen | predictable, testable, no framework magic |
| Images | Coil 3 (`coil-compose` + `coil-network-okhttp` 3.5.0) | Coil 3 split networking into a separate artifact - reuses the app's existing OkHttp engine rather than bundling its own |
| Camera/QR | CameraX 1.5.2 + ML Kit `barcode-scanning` 17.3.0 (bundled model, no Play Services dependency) | reusable scanner for Vendor/Collection/Order vendor-lookup, per the SRS |
| Location | Play Services `play-services-location` 21.4.0 (Fused Location Provider) | one-shot GPS capture for Vendor create/edit - the SRS forbids manual lat/long entry |

## Module structure

**Single Gradle module** (`:app`), organized by layer then feature. Four features (auth,
products, vendors, orders) don't justify multi-module Gradle overhead — it would only slow
down navigation for both humans and AI agents. Revisit only if the app grows substantially.

```
com.example.dealernest/
  DealerNestApp.kt              # @HiltAndroidApp
  MainActivity.kt                # hosts the single NavHost

  core/
    network/                     # Retrofit/OkHttp, envelope, error handling
    session/                     # DataStore-backed token + profile cache
    location/                    # LocationPermissionManager, LocationProvider (Fused Location)
    qr/                          # CameraX + ML Kit QR scanner composable

  domain/
    model/                       # plain Kotlin data classes, no serialization annotations
    repository/                  # interfaces only — data/ provides the impls

  data/
    auth/                        # DTOs + repository impl for auth
    vendor/                      # DTOs + repository impl for vendors
    di/                          # Hilt modules binding repository interfaces to impls
    (data/products/, data/orders/, data/collections/ arrive in their own phases)

  presentation/
    components/                  # shared design-system: cards, badges, states, buttons, search
    navigation/                  # routes, NavHost, BottomNavScaffold (fixed 5 tabs)
    locationgate/                # blocking location-permission gate, shown before Splash
    splash/
    auth/                        # login screen + viewmodel
    vendor/                      # list/details/form screens + viewmodels
    profile/                     # read-only profile screen + viewmodel
    home/                        # Dashboard/Order/Collection placeholders (their phases haven't arrived)

  ui/theme/                       # existing Color/Theme/Type, lightly extended
```

**Dependency direction**: `presentation → domain ← data`, and both depend on `core/`. `domain`
has zero Android/Retrofit/DataStore imports — it's plain Kotlin, which keeps ViewModels and
repositories independently testable. `core/` never depends on `presentation` or `data`. The
Phase 0 `MenuItem`/`AppMenu` permission-slug model is gone — Phase 1 replaced it with a fixed
5-tab bottom nav (`presentation/navigation/BottomNavScaffold.kt`) plus a role model
(`domain/model/RoleLevel.kt`) that gates in-module actions, not tab visibility — see
`auth-and-permissions.md`.

## Networking core

- `ApiEnvelope<T>` — generic wrapper for `{ success, message, data, errors }` (see
  `api-contract.md` for the exact shape).
- `NetworkResult<T>` — sealed `Success(data) / Error(message, errors?) / Exception(throwable)`,
  returned by every repository method. ViewModels map this directly to their `UiState`.
- `safeApiCall { }` — inline helper that runs a suspend Retrofit call, catches `HttpException`/
  `IOException`, and produces the right `NetworkResult` branch, including parsing the 422
  `errors` map and detecting 401s.
- `AuthInterceptor` — reads the current token from `SessionManager.currentTokenBlocking()` (an
  in-memory cache kept in sync with DataStore by a background collector, since OkHttp
  interceptors run synchronously and can't suspend) and adds `Authorization: Bearer <token>`.
  Skips this for paths in a small no-auth allowlist (currently just `v1/admin/login`) — checked
  by comparing `request.url.encodedPath`, since Retrofit suspend functions don't have a
  convenient way to attach OkHttp request tags without extra boilerplate.
- 401 handling: **not** done via `Authenticator` (no refresh-token endpoint exists in the API).
  `safeApiCall` detects a 401 and returns `NetworkResult.Error(..., isUnauthorized = true)`;
  the caller (currently only reachable post-login, since login itself can't 401 the same way)
  is expected to call `SessionManager.clearSession()` when it sees that flag. See
  `auth-and-permissions.md`.

## Design system (`presentation/components/`)

Shared Compose building blocks so every module gets the "cutting-edge, colorful, modern" look
from one place instead of re-styling per screen: `GradientBackground`, `SectionCard` (rounded,
soft-shadow container), `StatCard`, `StatusBadge` (color-coded: green/orange/red for
paid/partial/unpaid-style states), `AppSearchField`, `SkeletonLoader` (shimmer placeholder),
`EmptyStateView`, `ErrorStateView` (with a retry callback), `PrimaryButton`. New screens should
compose these rather than writing raw `Card`/`OutlinedTextField` styling inline - if a new
pattern doesn't fit an existing component, add a new one here rather than one-off styling in a
feature screen.

## Location gate (`core/location/`, `presentation/locationgate/`)

Per the SRS, the app is unusable without location permission. `LocationGateScreen` sits before
`Routes.SPLASH` in `DealerNestNavHost` and blocks all navigation until granted:
rationale → `ActivityResultContracts.RequestPermission` → denied (retry) → permanently denied
(button opens `Settings.ACTION_APPLICATION_DETAILS_SETTINGS` for this app). Every screen after
this gate can assume the permission is granted. `LocationProvider.getCurrentLocation()` (a
one-shot suspend fun over `FusedLocationProviderClient`) is what Vendor create/edit calls
immediately before submitting - lat/long are never manual text fields anywhere in the app.

## QR scanner (`core/qr/`)

`QrScannerScreen` is a full-screen Composable (CameraX `PreviewView` + ML Kit barcode analysis)
that returns the scanned text via a callback. It's generic - not vendor-specific - so Phase 2/3
(Collection/Order vendor lookup) reuse it the same way Vendor's list search does: the scanned
value feeds into the same search state the text box uses, per the SRS ("both should update the
same screen").

It requests `CAMERA` at runtime itself (rationale → request → denied-with-retry →
permanently-denied → Settings, same shape as the location gate) before rendering the preview -
the manifest `<uses-permission>` alone isn't enough on API 23+. Missing this originally caused a
silent black screen (`bindToLifecycle` throws without it, swallowed by a `runCatching`) with no
error shown - found via live testing, not initially built defensively. Any *new* screen that
uses a dangerous permission should request it explicitly the same way; don't assume a manifest
entry is sufficient.

## Vendor module pattern (`data/vendor/`, `domain/model/Vendor.kt`, `presentation/vendor/`)

The template every later CRUD module (Collection, Order) should follow: DTOs confirmed against
`api-contract.md` → domain model + repository interface → repository impl (plain GET for
search/details, `@Multipart` for create/update matching the exact form fields, including the
`_method=PUT` override for update) → one ViewModel+screen per concern (list/details/form) using
the design-system components above. Vendor search has no page cursor (see `api-contract.md`),
so its list uses a "load more by raising `limit`" pattern, not Paging 3 - don't copy this
pattern for Orders/DSR Collections, which do have real pagination (Phase 5 note in
`roadmap.md`).

## DI graph (Hilt)

- `NetworkModule` (`@InstallIn(SingletonComponent::class)`) → `Json`, `OkHttpClient`,
  `Retrofit`, `ApiService`.
- `SessionModule` → the single `DataStore<Preferences>` instance (via `preferencesDataStore`
  delegate scoped to the Application context), `SessionManager`.
- `RepositoryModule` → `@Binds` interfaces (`AuthRepository` → `AuthRepositoryImpl`,
  `VendorRepository` → `VendorRepositoryImpl`).

**Gotcha - the `com.google.dagger.hilt.android` Gradle plugin is deliberately NOT applied.**
It's incompatible with AGP 9.3.0 as of Hilt 2.57.1 (`Android BaseExtension not found` -
[dagger#4944](https://github.com/google/dagger/issues/4944)). Instead, the two Hilt entry
points are wired manually per [Hilt's own documented fallback](https://dagger.dev/hilt/gradle-setup.html):
`DealerNestApp` extends the generated `Hilt_DealerNestApp` and is annotated
`@HiltAndroidApp(Application::class)`; `MainActivity` extends `Hilt_MainActivity` and is
annotated `@AndroidEntryPoint(ComponentActivity::class)` - the explicit class argument on both
annotations replaces what the plugin's bytecode transform would otherwise infer. **If you ever
add another `@AndroidEntryPoint` class** (there's currently only the one Activity, since this
is a single-Activity app), give it the same explicit-superclass treatment - see the comments in
`DealerNestApp.kt`/`MainActivity.kt`. `@HiltViewModel` ViewModels are unaffected by any of this
(no plugin dependency either way) and need no special handling.

Two related Gradle workarounds, both commented in `gradle.properties`, exist for the same
AGP 9 bleeding-edge friction: `android.disallowKotlinSourceSets=false` (KSP's source-set
registration predates AGP 9's mandatory "built-in Kotlin" mode) and `ksp.incremental=false`
(works around a Windows-only crash when the project dir and Gradle's cache are on different
drive letters). Revisit both if upgrading KSP/Hilt or changing machines resolves their root
causes.

## Why DataStore over EncryptedSharedPreferences (explicit tradeoff)

`androidx.security.crypto` (EncryptedSharedPreferences) has been stuck at `1.1.0-alpha06` for
years with no stable release in sight, while DataStore Preferences is Google's actively
maintained, currently-recommended local storage API. Since app-private storage is already
inaccessible to other apps on a non-rooted device, the marginal security benefit of an extra
encryption layer is small relative to the maintenance risk of depending on a permanently-alpha
library. If the user later wants at-rest encryption anyway (e.g. compliance requirement), swap
the `DataStore<Preferences>` inside `SessionManager` for a Tink-encrypted one — the public
`SessionManager` API doesn't need to change.
