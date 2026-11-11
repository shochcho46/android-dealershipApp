# Coding Conventions

## Recipe: adding a new feature (e.g. DSR Collections in Phase 2)

The Vendor module (`data/vendor/`, `domain/model/Vendor.kt`, `domain/repository/VendorRepository.kt`,
`presentation/vendor/`) is the reference implementation of this recipe — copy its shape rather
than re-deriving one from scratch:

1. **DTOs** — `data/<feature>/dto/<Feature>Dtos.kt`: `@Serializable` data classes matching the
   exact JSON confirmed in `api-contract.md`. Field names use `@SerialName` where the API uses
   snake_case. Don't invent fields the API doc doesn't confirm.
2. **Domain model** — `domain/model/<Feature>.kt`: plain Kotlin data class, camelCase, no
   serialization annotations. Add a `fun <Feature>Dto.toDomain(): <Feature>` mapper next to the DTO.
3. **Repository interface** — `domain/repository/<Feature>Repository.kt`: suspend functions
   returning `NetworkResult<T>`.
4. **Repository impl** — `data/<feature>/<Feature>RepositoryImpl.kt`: calls `ApiService` inside
   `safeApiCall { }` for JSON endpoints; for multipart (Vendor create/update is the example),
   call the `@Multipart` Retrofit method directly and map the result manually since
   `safeApiCall` expects an `ApiEnvelope<T>` return type.
5. **Bind it** — add `@Binds abstract fun bind<Feature>Repository(impl: ...): <Feature>Repository`
   to `data/di/RepositoryModule.kt`.
6. **ViewModel(s)** — `presentation/<feature>/<Feature>XViewModel.kt`: `@HiltViewModel`, exposes
   `StateFlow<XUiState>` (sealed: `Loading / Success(...) / Error(message) / Empty`). One
   ViewModel per screen-concern (list / details / form), not one god ViewModel per feature.
7. **Screen(s)** — `presentation/<feature>/<Feature>XScreen.kt`: `hiltViewModel()`,
   `collectAsStateWithLifecycle()`, render per `UiState` branch using the shared components in
   `presentation/components/` (see `architecture.md`) rather than one-off styling.
8. **Wire it in** — route(s) in `Routes.kt`, `composable(...)` entries in `DealerNestNavHost.kt`
   nested under the right bottom-nav tab (see `auth-and-permissions.md`).
9. **Role-gate where the SRS says to** — if the feature has Edit/Delete rules, check
   `profile.roleLevel` (see `auth-and-permissions.md`) in the ViewModel/screen, not by hiding
   the destination.

## `NetworkResult<T>` (core/network/NetworkResult.kt)

```kotlin
sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(val message: String, val errors: Map<String, List<String>>? = null, val isUnauthorized: Boolean = false) : NetworkResult<Nothing>()
    data class Exception(val throwable: Throwable) : NetworkResult<Nothing>()
}
```

Always exhaustively `when` over this in ViewModels — don't unwrap with `!!` or ignore branches.

## UI state pattern

One sealed `XxxUiState` per screen, not a single shared "app state" god object. Keep it in the
same file as its ViewModel unless it grows large. Loading/Error/Success are the minimum
branches; add screen-specific ones (e.g. `Empty` for a search with no results) as needed.

## Style

- No comments explaining *what* code does — names should already say that. Only comment a
  non-obvious *why* (e.g. the blocking DataStore read in `AuthInterceptor`).
- Prefer `internal` visibility for anything not referenced outside its own package.
- Hilt modules are one per layer/concern (`NetworkModule`, `SessionModule`,
  `RepositoryModule`), not one giant module — keeps each file's purpose scannable.
- Don't add pagination, caching, retry, or offline support ahead of the phase that needs it —
  see `roadmap.md`. Speculative infrastructure is exactly what this doc set exists to prevent.
