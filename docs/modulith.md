<!-- Load when: calling code in another module, designing new public-ish API, or wondering why service not directly injected. -->

# Cross-Module API (Modulith)

## Rule

Modules (`access`, `auth`, `catalog`, `contacts`, `core`, `dashboard`, `locations`, `notifications`, `orders`, `publics`, `registration`, `staffing`, `tenant`, `user`) may only talk through `*InternalApi` interfaces. Never inject another module's service or repository directly.

## Naming and location

- Interface name: `<Concept>InternalApi` (e.g. `ContactInternalApi`, `AccessLogInternalApi`, `ProductInternalApi`, `ServicePlanInternalApi`, `CatalogTemplateInternalApi`, `LocationInternalApi`, `StaffInternalApi`, `ShiftInternalApi`, `UserInternalApi`, `TenantInternalApi`, `BillingCycleInternalApi`, `RegistrationInternalApi`, `AuthInternalApi`, `OrderInternalApi`, `InvoiceInternalApi`).
- Place at **module root** when consumed by more than one submodule. All current `*InternalApi` interfaces are at module roots.
- Companion DTOs crossing module boundaries live next to interface, also at module root. Examples: `contacts/ContactAccessInternalResponse.java`, `user/UserAuthInternalResponse.java`, `tenant/TenantInternalResponse.java`.

## Implementation

- Implementing service annotated `@Service` and lives next to peers inside owning submodule (flat for most modules, layered for `tenant/`).
- Interface itself is contract; implementation classes package-private.
- Always pass ids, not entities, across module boundaries. Receiving module hydrates id through its own repository (or view) before doing work.
- Mutable cross-submodule flows that must commit atomically orchestrated by dedicated `@Service` via direct `*InternalApi` calls **in same transaction** — not via application events. See `tenant/application/CycleRollEngine` (calls `ContactInternalApi.countContactsWithAccess` inside transaction). Application events remain only for genuinely asynchronous side effects such as email dispatch (`TenantEmailVerificationEvent` → `notifications/`).

## What NOT to do

- Do not expose `*InternalApi` methods returning JPA entities — only ids, DTOs, or primitives. This keeps each module's persistence context private.
- Do not bypass interface with `applicationContext.getBean(...)`. Interface exists so Modulith tests can verify boundary; bypassing makes boundary invisible.
- Do not add `public` to interface methods just to expose them to controllers — controllers in same module call service directly.
