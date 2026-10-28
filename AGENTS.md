# AGENTS.md — B2BSystem

Multi-tenant B2B management SaaS. Java 25, Spring Boot 4.1.1, Spring Modulith 2.1.1, PostgreSQL + Flyway, Hibernate (multi-tenancy via `@TenantId`), MapStruct 1.6.3, Caffeine cache, Spring Security (OAuth2 resource server), Tomcat. Memory-constrained runtime (~512MB) — every read path matters.

Modules: `core/`, `identity/`, `gyms/`, `notifications/`. The `identity/tenant` submodule is the only one currently using the layered package layout; everywhere else flat. See `docs/architecture.md`.

## Hard invariants (do not violate)

- Multi-tenancy enforced via `@TenantId` + `TenantIdentifierResolver`. Never query across tenants — cross-tenant reads fetch scalars/IDs only, reload entities per-tenant after setting context. See `docs/reads-hydration.md`.
- Entities extend one of: `ImmutableBaseEntity` (ledgers, append-only), `TenantMutableEntity` (CRUD, tenant-scoped), `TenantSoftDeletableEntity` (adds `deleted_at`), `TenantTogglableEntity` (adds `is_active`), `SystemMutableEntity` (system-level, no tenant). See `docs/entities.md`.
- Migrations: `V{num}__{description}.sql`, UUID PKs, `tenant_id` indexed. See `docs/migrations.md`.
- Caching uses Caffeine. Tenant-scoped cache values must include tenant in key. See `docs/caching.md`.
- Heavy-table reads follow sanctioned patterns in `docs/reads-hydration.md` — never ad-hoc.
- Errors thrown from domain code are subclasses of `DomainRuleException`; global handler maps them to RFC-7807 `ProblemDetail` responses. See `docs/errors.md`.
- Cross-module calls go through `*InternalApi` interfaces, not direct service injection. See `docs/modulith.md`.

## Task → doc router

| Task                                         | Read first                              |
|----------------------------------------------|-----------------------------------------|
| Adding/editing an entity, view, or projection | `docs/entities.md`                      |
| Adding/editing a DTO or MapStruct mapper      | `docs/dtos-mappers.md`                  |
| Any read/query endpoint (controllers, repos)  | `docs/reads-hydration.md`               |
| Writing or editing any test                   | `docs/testing.md`                       |
| Anything touching `@Cacheable` / Caffeine    | `docs/caching.md`                       |
| New Flyway migration                          | `docs/migrations.md`                    |
| Module/package layout questions              | `docs/architecture.md`                  |
| Errors, validation, global handler           | `docs/errors.md`                        |
| Cross-module API surface (`*InternalApi`)    | `docs/modulith.md`                      |
| Recording/reviewing settled decisions        | `docs/decisions.md`                     |
| Understanding current schema / table shapes / views | `docs/database.md` |

## Build & verify

- Maven wrapper: `./mvnw` (or `mvnw.cmd` on Windows).
- Tests use Testcontainers (Postgres) — `src/test/java`. No in-memory DB.
- Structural invariants enforced by tests, not one ArchUnit layer rule: `TenancyEnforcementTest` (ArchUnit: `@TenantId` on entities, native-query tenant filters, tenant-scoped `UserRepository` reads), `SoftDeleteRestrictionTest` (`@SQLRestriction` on soft-deletable entities), Modulith `ApplicationModules.of(...)` boundary checks. See `docs/testing.md`.
