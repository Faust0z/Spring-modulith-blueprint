<!-- Load when: adding/editing any read endpoint, controller list method, repository query, or page projection. -->

# Reads and Hydration

Runtime ~512MB. Every entity hydrated into persistence context costs memory, but bounded page of hydrated entities acceptable. All tables must follow one of following patterns.

## Read pattern — Projection with `findBy` (primary)

Standard read pattern uses Spring Data's fluent `findBy` API with type-safe projections. Record field names must match entity/view field names exactly.

### Single entity

```java
ContactResponse response = repository.findById(id, ContactResponse.class).orElseThrow(...);
```

### List of entities

```java
List<ContactResponse> list = repository.findAllBy(ContactResponse.class);
```

> Requires `<T> List<T> findAllBy(Class<T> type)` declaration on repository. See `BranchRepository` for canonical example.

### Filtered list

```java
List<ContactResponse> list = repository.findBy(
    Specification.allOf(specs),
    fluent -> fluent.as(ContactResponse.class).all()
);
```

### Page of entities

```java
Page<ContactSummary> page = repository.findBy(
    pageable,
    ContactSummary.class
);
```

### Filtered page

```java
Page<ContactSummary> page = repository.findBy(
    Specification.allOf(specs),
    fluent -> fluent.as(ContactSummary.class).page(pageable)
);
```

### Sorting

`@PageableDefault` always present on paginated controller endpoints as client-facing fallback (size + default sort), regardless of backing query mechanism.

- **JPA paths**: pass `pageable` straight through to fluent `findBy` call. Spring Data derives `ORDER BY` from `pageable.getSort()` and validates property names against entity — no injection risk. Never hand-write `.sortBy(...)` next to `page(pageable)`/`slice(pageable)`.
- **JDBC paths**: sort key is fixed per-query constant (e.g. `"created_at"`); only `ASC`/`DESC` direction read from first `Pageable` order. No user-controlled identifier ever reaches `ORDER BY` (see §Heavy-table reads below).

### Heavy tables with `Slice`

For heavy tables where `COUNT(*)` roundtrip of `Page` undesirable, use `Slice`:

```java
Slice<ContactSummary> slice = repository.findBy(
    Specification.allOf(specs),
    fluent -> fluent.as(ContactSummary.class).slice(pageable)
);
```

Leave choice between `Page` and `Slice` up to developer based on specific use case.

## Derived fields — `[Domain]Summary` + `[Domain]Mapper`

In case of calculated attributes (like `canAccess` from `ContactResponse`), follow this pattern:

1. **Create `[Domain]Summary` interface** — pure projection interface declaring getters only for columns needed by entity.

```java
public interface ContactSummary {
    UUID getId();
    String getDni();
    String getFirstName();
    String getLastName();
    String getCurrentMembershipName();
    Instant getAccessUntil();
    // ... only entity-level fields, no derived values
}
```

2. **Use summary for projection** in repository calls:

```java
repository.findById(id, ContactSummary.class);
repository.findBy(spec, q -> q.as(ContactSummary.class).page(pageable));
```

3. **Rely on `[Domain]Mapper`** (MapStruct) to calculate final derived values. Mapper provides overloaded `toResponse(Summary)` with explicit `@Mapping` expressions:

```java
@Mapping(target = "fullName", expression = "java(summary.getFirstName() + \" \" + summary.getLastName())")
@Mapping(target = "canAccess", expression = "java(summary.getAccessUntil() != null && summary.getAccessUntil().isAfter(java.time.Instant.now()))")
ContactResponse toResponse(ContactSummary summary);
```

This gives minimal RAM footprint: projection interface avoids full entity hydration, and mapper computes derived values in memory without additional database calls.

## Read pattern B — `JdbcTemplate` returning DTOs directly

For very hot endpoints, bypass JPA entirely. Canonical shape now two-stage:

1. Static `RowMapper<[Domain]Summary>` maps snake_case columns into package-private `[Domain]Summary` record — **pure column extraction**, no derived logic, no formatting.
2. `[Domain]Mapper` (MapStruct) computes derived values via `@Mapping(expression = "java(...)")` and `default` helpers.
3. Repository returns `Slice<[Domain]Summary>`; service maps it to `Slice<Response>` with `Slice.map(mapper::toDetails)`.

See `ContactSummary`/`ContactDetailsRepository`/`ContactMapper` and `AccessLogSummary`/`AccessLogDetailsRepository`/`AccessLogMapper` for canonical examples.

## Value extraction

Use `rs.getObject("col", UUID.class)` for UUID columns and `rs.getObject("col", Instant.class)` for nullable timestamp columns.

## What to avoid

- `findAll()` on `@OneToMany` collection for list endpoint.
- `EntityGraph` for fan-out reads — denormalize instead.
- Returning entities from services to controllers; services return DTOs.
- Native SQL joins across tenant scopes — Hibernate multi-tenancy rejects them, but raw `JdbcTemplate` does not enforce it for you.
- Native `@Query` returning `@TenantId`-bearing managed entities for cross-tenant result sets. Hibernate's behavior hydrating entities via native SQL version-dependent and ambiguous. Use one of patterns below instead.

## Cross-tenant reads and native queries

Three cases, three shapes:

1. **Genuinely session-less reads (identity, credentials, existence checks).**
   Use `JdbcTemplate` (`AuthLookupRepository` in `user/infrastructure/`). No `EntityManager`, no Hibernate session, no `CurrentTenantIdentifierResolver` call at all. This is only shape structurally guaranteeing zero tenant-resolver involvement — required when tenant not yet known (login, registration, payment-cycle lookup). Non-negotiable for those paths.

2. **Cross-tenant aggregation returning scalars / DTO projections.**
   Native `@Query` returning `UUID`, `Long`, or interface/record projections fine. Query rides on Hibernate session (so resolver consulted), but since nothing tenant-annotated gets hydrated there is no ambiguity. This is correct shape for batched lookups such as `findTenantIdsWithExpiredActive` and `findActiveIds` in `BillingCycleRepository`.

3. **Cross-tenant reads that ultimately need managed, dirty-tracking entities.**
   Never native-query `@TenantId`-bearing entities across tenants. Instead:
   - Fetch only IDs (scalar, safe) via native query like `findActiveIds` / `findTenantIdsWithExpiredActive`.
   - Reload managed entity **inside** per-tenant loop, after `TenantContext.setTenantId()` set, using normal tenant-scoped `findByIdForWrite` (pessimistic lock when loop writes).

   This exactly shape `rollSingleTenant` already follows (`findActiveForWrite` after context set). `recordNightlyPeaks` converted from risky `findActivesNative` (entity-hydrating native cross-tenant query) to this same pattern.

`TenancyEnforcementTest` enforces any other native query must contain `tenant_id = :tenantId`.

## Cross-tenant bulk writes and sweeper jobs

For genuinely tenant-agnostic bulk work (threshold-based cleanup, global sweeps, nightly batch updates across all tenants), native `@Modifying` queries or cross-tenant native reads must be wrapped explicitly so resolver does not fail or silently fall back to stale tenant.

```java
// TenantContext
public static final UUID SYSTEM_SCOPE = UUID.fromString("00000000-0000-0000-0000-000000000000");

// Call site
TenantContext.setTenantId(TenantContext.SYSTEM_SCOPE);
try {
    // cross-tenant native query or @Modifying statement
} finally {
    TenantContext.clear();
}
```

Rules:
- Escape hatch **explicit and intentional at call site**. Nothing about resolver changes — it still throws if nothing set post-readiness.
- After initial cross-tenant lookup returns IDs, per-tenant work must run inside `TenantJobRunner.forEach(...)` (or equivalent), which sets real tenant ID for each iteration.
- `TenancyEnforcementTest` allowlists approved cross-tenant native queries (`ShiftRepository.closeUnfinishedShifts`, `BillingCycleRepository.findActiveIds`, `BillingCycleRepository.findTenantIdsWithExpiredActive`). Each call site must carry `TenantContext.SYSTEM_SCOPE` wrap.

## Cross-tenant identity lookups (no session, no resolver)

When tenant not yet known — login, registration, payment-cycle lookup — never hit `UserRepository` with JPA. `users` table hybrid (tenant users + global system admins), so `User` has no `@TenantId` and query would either:
- run against bootstrap tenant (pre-readiness fallback), or
- throw `IllegalStateException` post-readiness (fail-fast resolver) with no `TenantContext`.

Use `AuthLookupRepository` (`JdbcTemplate` in `user/infrastructure/`) instead. It selects directly from `users` with no Hibernate session, no `CurrentTenantIdentifierResolver` involvement, and handles nullable `tenant_id` (global admin) naturally. Every pre-context identity read (credential lookup, `getUserResponseByIdentifier`, `existsByEmail/Username`, `getUserResponseById`) routes through it.

## Heavy-table reads — boundaries and staging

- `v_*` detail views carry no JPA entity mapping — nothing hydrates them through Hibernate. They are DB join layer, consumed only through domain's `JdbcTemplate` repository (`AccessLogDetailsRepository` for `v_access_log_details`, etc.).
- New heavy-table read endpoints go straight to two-stage shape in §Read pattern B (summary `RowMapper`, then domain mapper for derived values). One-stage `RowMapper<Dto>` acceptable for very hot simple reads that do no derived formatting. `[Domain]Summary` records public, used only by repository and its mapper.
- Two-stage shape does not apply to cross-tenant aggregation that must reload managed entities for writes — those paths stay on scalar/native ID fetch plus per-tenant reload (see §Cross-tenant reads).
