<!-- Load when: creating/editing entity, `@Immutable` view, or deciding which base class to extend. -->

# Entities

## Base class hierarchy (pick exactly one)

All entities extend `@MappedSuperclass` from `com.faust0z.B2BSystem.core`. Read `core/ImmutableBaseEntity.java` to see audit columns (id, createdAt, createdBy, version) they all inherit.

```
ImmutableBaseEntity                          (id UUID, createdAt, createdBy, @Version)
├── TenantMutableEntity                      (+ tenantId @TenantId, lastModifiedAt/By, isNew tracking)
│   └── TenantSoftDeletableEntity            (+ deletedAt Instant + softDelete())
│       └── TenantTogglableEntity            (+ isActive Boolean)
└── SystemMutableEntity                      (no tenantId; isActive, lastModifiedAt/By)
```

- `ImmutableBaseEntity` — append-only ledgers (access_logs, orders, order_items, invoices). No mutators needed.
- `TenantMutableEntity` — CRUD entities tenant-scoped but never soft-deleted or toggled (billing_cycles).
- `TenantSoftDeletableEntity` — **current convention for tenant-scoped tables** (contacts/members, products, locations, staff, catalog_templates). Use `.softDelete()` to mark deleted. See `contacts/domain/ContactEntity.java`.
- `TenantTogglableEntity` — legacy/system tables using `is_active` (tenants, subscription_plans).
- `SystemMutableEntity` — system-level, no tenant scope. `User` extends this because `users` table mixes tenant users with global system admins (`tenant_id` nullable), so it cannot carry mandatory `@TenantId`.

## Multi-tenancy on entity

- Tenant id field must be `private UUID tenantId;` annotated `@TenantId` + `@Column(name = "tenant_id", nullable = false, updatable = false)`. See `contacts/domain/ContactEntity.java` (inherits column from `TenantMutableEntity` so not redeclared) vs `access/domain/AccessLogEntity.java` (extends `ImmutableBaseEntity` directly, so field redeclared on entity).
- Never write code that loads entity then queries by `tenantId` manually — Hibernate filter via `TenantIdentifierResolver` already enforces it. Post-startup resolver throws `IllegalStateException` if no `TenantContext` set — bootstrap UUID fallback (`0000…0000`) only applies during application initialization.
- `User` hybrid: carries both tenant users and global system admins, so cannot use `@TenantId`. All cross-tenant / pre-context reads against `users` (login credential lookup, identifier search, global uniqueness checks, id-based reads used before tenant context established) go through `AuthLookupRepository` (`JdbcTemplate`), not JPA. This removes bootstrap-session backdoor for identity resolution. Per-tenant CRUD enforcement remains via explicit `findByIdAndTenantId` / `findAllByTenantId` on `UserRepository`.
- **Never mutate `TenantContext` inside already-open transaction.** Pre-tenant and post-tenant phases must use separate `REQUIRES_NEW` transactions. `TenantService.createTenant` only persists tenant row and returns; all tenant-scoped work (demo cycles, onboarding, seeding) happens in `REQUIRES_NEW` callers after `TenantContext.setTenantId` called outside any enclosing transaction.

## Soft delete

- Preferred: extend `TenantSoftDeletableEntity`. Delete via `entity.softDelete()` inside transaction. `@SQLRestriction("deleted_at IS NULL")` annotation on entity hides deleted rows by default — see `contacts/domain/ContactEntity.java`.
- Legacy: `is_active BOOLEAN` still exists on `users`, `tenants`, `subscription_plans` (not migrated). Append-only ledgers (`access_logs`, `orders`, `order_items`, `invoices`) never had `is_active`. New tenant-scoped tables should use `deleted_at`.

## Denormalization and views for heavy reads

- **Prefer denormalization**: when read value computed from entity's own table, denormalize it onto entity itself and stamp at write time.
- **Views for genuine cross-table joins**: when read truly combines data from multiple tables, use SQL view.
- For sanctioned read-side shapes on top of those views (two-stage `JdbcTemplate` `Summary` + MapStruct `Mapper`, heavy-list `Slice`), see `docs/reads-hydration.md`.

## FK pattern

- Across domains: plain `UUID` column, no JPA `@ManyToOne`. See `contact_id`, `location_id`, `staff_id` on `orders/domain/OrderEntity.java`.
- Within same domain: tight relations allowed (rare — most cross-table links id-only).
