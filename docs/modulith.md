<!-- Load when: calling code in another module, designing new public-ish API, or wondering why service not directly injected. -->

# Cross-Module API (Modulith)

## Rule

Modules (`core`, `identity`, `gyms`, `notifications`) may only talk through `*InternalApi` interfaces. Never inject another module's service or repository directly.

## Naming and location

- Interface name: `<Concept>InternalApi` (e.g. `UserInternalApi`, `MemberInternalApi`, `ProductSaleInternalApi`, `TenantInternalApi`, `RegistrationInternalApi`, `AuthInternalApi`, `BillingCycleInternalApi`).
- Place at **module root** when consumed by more than one submodule. Examples at `gyms/`: `MemberInternalApi.java`, `ProductInternalApi.java`, `ProductSaleInternalApi.java`, `RoutineInternalApi.java`, `WorkerInternalApi.java`, `WorkerShiftInternalApi.java`, `EntryLogInternalApi.java`. Examples at `identity/`: `UserInternalApi.java`, `TenantInternalApi.java`, `BranchInternalApi.java`.
- Place inside owning submodule only if consumed by single peer (rare; current example: `gyms/memberships/MembershipInternalApi.java`).
- Companion DTOs crossing module boundaries live next to interface, also at module root. Examples: `gyms/MemberAccessInternalResponse.java`, `identity/UserAuthInternalResponse.java`, `identity/TenantInternalResponse.java`.

## Implementation

- Implementing service annotated `@Service` and lives next to peers inside owning submodule (flat for `gyms/*`, layered for `identity/tenant/application/`).
- Interface itself is contract; implementation classes package-private.
- Always pass ids, not entities, across module boundaries. Receiving module hydrates id through its own repository (or view) before doing work.
- Mutable cross-submodule flows that must commit atomically orchestrated by dedicated `@Service` via direct `*InternalApi` calls **in same transaction** — not via application events. See `gyms/members/MemberRegistrationService` (calls `MemberInternalApi.createMember` then `MembershipSaleInternalApi.createMembershipSale` inside `register()`). Application events remain only for genuinely asynchronous side effects such as email dispatch (`TenantEmailVerificationEvent` → `notifications/`).

## What NOT to do

- Do not expose `*InternalApi` methods returning JPA entities — only ids, DTOs, or primitives. This keeps each module's persistence context private.
- Do not bypass interface with `applicationContext.getBean(...)`. Interface exists so Modulith tests can verify boundary; bypassing makes boundary invisible.
- Do not add `public` to interface methods just to expose them to controllers — controllers in same module call service directly.
