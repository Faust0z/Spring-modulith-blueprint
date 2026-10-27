<!-- Load when: deciding where to place a new file inside a module, or asking which submodule a concept belongs to. -->

# Architecture and Package Layout

## Top-level modules

```
com.faust0z.B2BSystem
├── core/             cross-cutting: tenancy, caching, error handling, audit, base entities
├── identity/         auth, users, tenants, branches, registration, public tenant lookup
├── gyms/             members, memberships, entry logs, store, workers, routines, dashboard, access
└── notifications/    event listeners (email verification, etc.)
```

Modulith boundaries enforced by package ownership. Only allowed cross-module call through `*InternalApi` interface (see `docs/modulith.md`).

## Target layered layout inside a submodule

```
<submodule>/
├── domain/           entities, enums, exceptions
├── application/      services, mappers, internal orchestration
├── infrastructure/   repositories (Spring Data interfaces)
├── web/              controllers, request DTOs
└── <InternalApi>.java, <SharedResponse>.java   (module-root files)
```

## Reality today

- `identity/tenant/` — **fully conforms** to layered layout. New code in other modules mirror this. See `identity/tenant/domain/TenantEntity.java`, `application/TenantService.java`, `infrastructure/TenantRepository.java`, `web/TenantController.java`.
- `identity/registration/` — partial: has `domain/` and `infrastructure/`; services, DTOs, `*InternalApi` live at submodule root.
- All other submodules — **flat**: every file at submodule root. `gyms/members/`, `gyms/entrylogs/`, `gyms/store/`, `gyms/workers/`, `gyms/memberships/`, `gyms/routines/`, `gyms/dashboard/`, `gyms/gymaccess/`, `gyms/publics/`, `identity/auth/`, `identity/user/`, `identity/branches/`, `identity/publics/`, `notifications/`.

## Rule of thumb for new files in flat submodule

If adding new concept inside `gyms/members/` (flat), keep file alongside peers at package root. Do not create `gyms/members/domain/...` paths — breaks layout + ArchUnit checks.

Module-wide refactor to layered layout: single PR, not file-by-file.

## Cross-module shared files

Place shared enums, exceptions, `*InternalApi` interfaces imported by more than one submodule at **module root** (e.g. `gyms/PaymentMethod.java`, `gyms/MemberInternalApi.java`, `identity/Role.java`, `identity/UserInternalApi.java`).

One current exception: `gyms/memberships/MembershipInternalApi.java` lives inside submodule rather than at `gyms/` root. New `*InternalApi` interfaces should be at module root for consistency.

## `publics/` submodules

`identity/publics/` and `gyms/publics/` intentionally tiny: just public/unauthenticated controller + response DTO. They delegate to `*InternalApi` interfaces owned by other submodules. Do not add service or repository here.
