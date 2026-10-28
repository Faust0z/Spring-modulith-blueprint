# Spring Modulith Production Blueprint

> A production-grade, multi-tenant B2B SaaS backend blueprint built with **Spring Boot 4.1.1**, **Spring Modulith 2.1.1**, and **Java 25**. Designed as a reference architecture demonstrating enterprise patterns for modular monoliths with strict tenant isolation, memory-constrained runtime optimization, and comprehensive structural enforcement via tests.

---

## What This Is

This repository is a **blueprint**, not a product. It exists to demonstrate how to build a maintainable, testable, and scalable B2B SaaS backend using modern Spring ecosystem patterns. Every architectural decision is intentional and enforced by automated tests — not documentation alone.

The codebase models a generic **catalog & access management** domain (contacts, products, service plans, locations, staffing, orders, billing) across **14 modules** with strict boundaries. It is designed to be cloned, studied, and adapted.

---

## Architecture Highlights

### Modular Monolith (Spring Modulith)

14 domain modules with compile-time boundary enforcement via `ApplicationModules.of(...)`. Cross-module communication goes exclusively through `*InternalApi` interfaces placed at module roots. No service or repository is ever injected across module boundaries.

```
com.faust0z.B2BSystem
├── access/        Entry logs, scan processing, occupancy
├── auth/          Security, JWT, OAuth2, tenant context filter
├── catalog/       Products, service plans, catalog templates
├── contacts/      Contact/member CRUD, import, quick stats
├── core/          Tenancy, caching, errors, base entities, scheduling
├── dashboard/     Dashboard aggregation
├── locations/     Branch/location CRUD, active validation
├── notifications/ Event listeners (email verification)
├── orders/        Orders, order items, invoices
├── publics/       Public/unauthenticated controllers
├── registration/  Tenant registration, email verification
├── staffing/      Staff, shifts, shift sweeper job
├── tenant/        Tenants, billing cycles, subscription plans
└── user/          Users, auth internal API
```

The `tenant/` module is the only one using the **layered package layout** (`domain/`, `application/`, `infrastructure/`, `web/`). All other modules are flat — a deliberate, documented choice for incremental adoption.

### Multi-Tenancy (Hibernate `@TenantId`)

Tenant isolation is enforced at the ORM level via `@TenantId` on entity fields and a `CurrentTenantIdentifierResolver` backed by a `ThreadLocal` `TenantContext`. No query can accidentally cross tenant boundaries — Hibernate filters every read automatically.

- **System scope**: Cross-tenant operations (scheduled jobs, sweeps) use `TenantContext.SYSTEM_SCOPE` explicitly — an intentional escape hatch, never a fallback.
- **Pre-context reads**: Identity lookups before tenant is known (login, registration) go through `AuthLookupRepository` (JDBC, no Hibernate session), structurally guaranteeing zero tenant-resolver involvement.
- **Per-tenant reload**: Cross-tenant aggregations fetch scalar IDs only, then reload managed entities inside per-tenant loops via `TenantJobRunner.forEach()`.

### Entity Base Class Hierarchy

Five base classes, each chosen by entity lifecycle:

```
ImmutableBaseEntity  (id, createdAt, createdBy, @Version) — append-only ledgers
├── TenantMutableEntity       (+ tenantId, lastModifiedAt/By, isNew) — CRUD
│   └── TenantSoftDeletableEntity  (+ deletedAt, softDelete(), @SQLRestriction) — soft-delete
│       └── TenantTogglableEntity  (+ isActive) — legacy toggles
└── SystemMutableEntity       (no tenantId; hybrid tables like users)
```

### Two-Stage Reads (Memory Optimization)

Heavy tables (`contacts`, `access_logs`) use a two-stage JDBC pattern:

1. `RowMapper<[Domain]Summary>` — pure column extraction into a package-private `Summary` record
2. `[Domain]Mapper.toDetails(Summary)` — MapStruct computes derived values (`fullName`, `canAccess`, etc.)

Returns `Slice` (no `COUNT(*)` roundtrip). View entities (`@Immutable`) are join-layer only — JPA never hydrates them for list endpoints.

### Caching (Caffeine)

- Per-cache TTLs and max sizes defined in `CacheConfig`
- **Tenant-scoped keys mandatory**: every `@Cacheable` key includes tenant (`tenantId + ':' + id`)
- `@Caching` groups evictions/puts on mutations
- `CacheErrorHandler` logs failures instead of throwing
- Programmatic `CacheManager` reserved for revocation records

### Error Handling (RFC 9457)

All domain errors extend `DomainRuleException` (carries own HTTP status, title, message). `GlobalExceptionHandler` maps everything to `ProblemDetail` with `traceId` (from MDC) and `timestamp`. No `ResponseStatusException` in services.

### Cross-Module Communication

- `*InternalApi` interfaces at module roots — the only allowed cross-module call path
- Pass **ids, not entities** across boundaries; receiving module hydrates through its own repository
- Atomic multi-step flows orchestrated synchronously in same transaction (not events)
- Application events reserved for genuinely async side effects (email dispatch)

---

## Technical Stack

| Component | Technology |
|-----------|------------|
| Language | Java 25 |
| Framework | Spring Boot 4.1.1, Spring Modulith 2.1.1 |
| Server | Tomcat 11 (Servlet 6.1) |
| Persistence | PostgreSQL, Spring Data JPA / Hibernate 7 |
| Migrations | Flyway (V1–V5) |
| Cache | Caffeine |
| DTO Mapping | MapStruct 1.6.3 |
| Validation | Jakarta Validation |
| Security | Spring Security, OAuth2 Resource Server (JWT) |
| Documentation | SpringDoc OpenAPI 3.1.1 |
| Testing | JUnit 5, Testcontainers, ArchUnit 1.5.1 |
| Metrics | Micrometer + Prometheus |
| Logs | Logstash Logback Encoder 8.1 |

---

## Structural Enforcement (Tests as Architecture)

The codebase has **no** flat ArchUnit "layer violation" rule. Instead, three mechanisms enforce invariants:

| Mechanism | What it enforces |
|-----------|------------------|
| **`TenancyEnforcementTest`** (ArchUnit) | Every `@Entity` has `@TenantId` (allowlisted system entities excepted); every native `@Query` contains `tenant_id = :tenantId` (allowlisted cross-tenant queries); `UserRepository` reads are tenant-scoped |
| **`SoftDeleteRestrictionTest`** (ArchUnit) | Every `@Entity` subclass of `TenantSoftDeletableEntity` carries `@SQLRestriction("deleted_at IS NULL")` |
| **`B2BSystemArchitectureTest`** (Spring Modulith) | Module boundary and dependency violations fail at `ApplicationModules.of(...)` construction |

Additionally: `AbstractJpaSliceTest` provides a reusable JPA slice test base with Testcontainers Postgres, and dedicated cache tests verify tenant-scoped key isolation.

---

## Read Patterns (Sanctioned)

All read endpoints follow one of these patterns — no ad-hoc queries:

1. **Projection with `findBy`** (primary): Spring Data fluent API with type-safe projections (`findById(id, Response.class)`, `findAllBy(Response.class)`)
2. **Filtered/Pageable**: `Specification.allOf(specs)` + fluent `findBy(spec, q -> q.as(Summary.class).page(pageable))`
3. **Heavy-table two-stage**: JdbcTemplate `RowMapper<Summary>` → `Slice` → `Slice.map(mapper::toDetails)` (contacts, access_logs)
4. **Cross-tenant**: scalar ID fetch → per-tenant reload after `TenantContext.setTenantId()`

---

## Quick Start

### Prerequisites

- JDK 21+
- PostgreSQL 15+ (or use Testcontainers)
- Maven 3.9+ (wrapper included)

### Run

```bash
# Start Postgres (or use docker-compose)
docker compose up -d

# Run the application
./mvnw spring-boot:run

# Run tests (requires Docker)
./mvnw test
```

### Environment variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/catalog_inventory` | JDBC URL |
| `POSTGRES_USER` | `postgres` | DB user |
| `POSTGRES_PASSWORD` | `postgres` | DB password |
| `JWT_SECRET` | (required) | 256-bit JWT signing secret |
| `JWT_EXPIRATION` | `86400000` | Token TTL (ms) |
| `APP_FRONTEND_URL` | `http://localhost:3000` | CORS origin |
| `RESEND_API_KEY` | (optional) | Email service key |
| `RESEND_FROM_EMAIL` | `onboarding@resend.dev` | Sender address |

---

## Database Schema

Schema state as of **V5** (5 Flyway migrations). Key conventions:

- **UUID primary keys** (application-generated, no `gen_random_uuid()`)
- **Tenant isolation**: `tenant_id UUID NOT NULL` + index on every tenant-scoped table
- **Soft delete**: `deleted_at TIMESTAMPTZ` (nullable; NULL = active) with partial unique indexes
- **Optimistic locking**: `version INTEGER NOT NULL DEFAULT 0` on all tables
- **Audit columns**: `created_at`, `created_by`, `last_modified_at`, `last_modified_by`
- **Timestamps**: `TIMESTAMPTZ` (timezone-aware)
- **Money**: `NUMERIC(12,2)` with `_ars` suffix

---

## Testing Strategy

- **No in-memory DB** — all repository tests run against real Postgres via Testcontainers
- **No `@MockBean`** — standardized on `@MockitoBean` (Spring Boot 4.1)
- **No test ordering** — random method order, isolated by construction
- **No `Instant.now()` in domain** — thread `Instant` for deterministic tests
- **Cache tests** bring up own Caffeine context with tenant-scoped key assertions
- **Architecture tests** fail the build on boundary violations

---

## Design Principles

1. **Multi-tenancy is non-negotiable** — `@TenantId` + `TenantIdentifierResolver`, never query across tenants without explicit system scope
2. **Entities are never exposed** — services return DTOs/projection records only
3. **Reads are intentional** — every read path follows a sanctioned pattern; no `findAll()` on collections
4. **Mutations are atomic** — cross-module writes in same transaction via `*InternalApi`, not events
5. **Cache keys include tenant** — one tenant never reads another's cached data
6. **Soft delete over hard delete** — `deleted_at` + `@SQLRestriction`, never `DELETE` on tenant data
7. **Migrations are versioned** — V-numbered, append-only, never edit applied migrations
8. **Tests enforce architecture** — invariants are code, not comments

---

## License

This project is a reference architecture blueprint. Use it as a starting point for your own multi-tenant SaaS backend.
