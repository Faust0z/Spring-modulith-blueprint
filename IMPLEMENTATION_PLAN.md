# Implementation Plan: Blueprint Project → Doc Standards Alignment

## Overview
Update the generic B2B catalog/inventory blueprint to exemplify all architectural patterns from the gym ERP docs, **without changing the domain** (keep access, auth, catalog, contacts, locations, orders, staffing, tenant, user modules).

**COMPLETED:**
- Phase 1: Tech Stack Upgrade (Spring Boot 4.1.1, Java 21, Tomcat, MapStruct 1.6.3, etc.)
- Phase 2: Entity Base Class Hierarchy (5 base classes, 15 entities migrated, deleted_at instead of is_active)

**REMAINING:**

---

## Phase 3: Multi-Tenancy Cross-Cutting Concerns

### 3.1 `TenantContext.SYSTEM_SCOPE` Constant
```java
// In TenantContext.java
public static final UUID SYSTEM_SCOPE = UUID.fromString("00000000-0000-0000-0000-000000000000");
```

### 3.2 `TenantJobRunner` Utility in `core/`
```java
// core/TenantJobRunner.java
public final class TenantJobRunner {
    private TenantJobRunner() {}
    
    public static void forEach(List<UUID> tenantIds, Consumer<UUID> action) {
        for (UUID tid : tenantIds) {
            TenantContext.setTenantId(tid);
            try { action.accept(tid); }
            finally { TenantContext.clear(); }
        }
    }
}
```

### 3.3 Wrap Cross-Tenant Scheduled Jobs
**`ShiftSweeperJob.closeExpiredShifts()`** — wrap native `@Modifying` call:
```java
TenantContext.setTenantId(TenantContext.SYSTEM_SCOPE);
try { shiftRepository.closeUnfinishedShifts(threshold, now); }
finally { TenantContext.clear(); }
```

**`CycleRollEngine.recordNightlyPeaks()`** — replace `findActivesNative()` with scalar IDs + TenantJobRunner:
```java
List<UUID> activeCycleIds = billingCycleRepository.findActiveIds();
TenantJobRunner.forEach(activeCycleIds, cycleId -> {
    BillingCycleEntity cycle = billingCycleRepository.findByIdForWrite(cycleId).orElseThrow();
    // ... existing logic
});
```
- Add `findActiveIds()` to `BillingCycleRepository` (native query returning `List<UUID>`)

**`CycleRollEngine.executeNightlyRoll()`** — add `SYSTEM_SCOPE` wrap around ID fetch.

### 3.4 Update `TenancyEnforcementTest` Allowlists
- Add new entity class names to `@Entity` allowlist
- Add `ShiftRepository.closeUnfinishedShifts`, `BillingCycleRepository.findActiveIds`, `BillingCycleRepository.findTenantIdsWithExpiredActive` to native query allowlist

---

## Phase 4: AuthLookupRepository (Pre-Context Identity Reads)

### 4.1 Create `AuthLookupRepository` in `user/infrastructure/`
- Uses `JdbcTemplate` (no Hibernate session, no `CurrentTenantIdentifierResolver`)
- Methods:
  - `Optional<UserAuthInternalResponse> findUserAuthByIdentifier(String identifier)`
  - `Optional<UserAuthInternalResponse> findUserById(UUID userId)`
  - `boolean existsByEmail(String email)`, `boolean existsByUsername(String username)`
  - `Optional<UUID> getTenantIdByUserId(UUID userId)`

### 4.2 Refactor `UserService` Pre-Context Methods
- Delegate pre-context methods to `AuthLookupRepository`
- Keep tenant-scoped reads on `UserRepository`

### 4.3 Fix `UserRepository` — remove unsafe `findById(UUID, Class<T>)` without tenant

---

## Phase 5: JdbcTemplate Two-Stage Reads (Heavy Tables)

### 5.1 Two Heavy Tables: `contacts` + `access_logs`

### 5.2 `contacts` — ContactSummary + ContactDetailsRepository + ContactMapper
**ContactSummary** (package-private record in `contacts/domain/`):
```java
record ContactSummary(
    UUID id, UUID tenantId, String dni, String firstName, String lastName,
    Instant accessUntil, String currentMembershipName, UUID accessToken,
    Instant createdAt, String email, LocalDate birthDate, String phoneNumber,
    String gender, int debtAmount
) {}
```

**ContactDetailsRepository** (JdbcTemplate): `Slice<ContactSummary> findAllByTenantId(UUID tenantId, Pageable pageable)`

**ContactMapper** (new, package-private): `toResponse(Entity)` + `toDetails(Summary)` methods

**ContactService.getAllContactsPaged()**: two-stage pattern

### 5.3 `access_logs` — AccessLogSummary + AccessLogDetailsRepository + AccessLogMapper
Same pattern as contacts.

### 5.4 Update `ContactInternalApi.countContactsWithAccess()` to use ContactDetailsRepository

---

## Phase 6: DTO/Mapper Conventions

### 6.1 Make All Mappers Package-Private
Remove `public` from all `@Mapper` interfaces.

### 6.2 Apply Two-Method Pattern Where Needed
- `toResponse(Entity)` — mutation path, ignore derived fields
- `toDetails(Summary)` — list path, compute derived fields

### 6.3 Target Mappers
~10 mappers: LocationMapper ✓, StaffMapper ✓, ProductMapper ✓, ServicePlanMapper ✓, CatalogTemplateMapper ✓, UserMapper, ContactMapper (new), AccessLogMapper (new), OrderMapper, InvoiceMapper

---

## Phase 7: InternalApi Module Root Placement

### 7.1 Verify All at Module Root
Each top-level package = module root. Current state ✓. Verify none nested inside submodules.

---

## Phase 8: Test Infrastructure

### 8.1 `AbstractJpaSliceTest` in `core/test/`
- `@DataJpaTest`, `@AutoConfigureTestDatabase(replace = NONE)`, Postgres container, `TestCacheConfig`

### 8.2 `SoftDeleteRestrictionTest` in root test
- Classpath scan for `@Entity` extending `TenantSoftDeletableEntity` → assert `@SQLRestriction`

### 8.3 Cache Tests
- `core/CacheBehaviorTest.java` — tenant-aware key isolation
- `contacts/ContactServiceCacheTest.java` — `@Cacheable` with tenant-scoped keys
- `access/AccessLogServiceCacheTest.java` — same for access logs

### 8.4 JPA Slice Tests for Each Repository
Extend `AbstractJpaSliceTest`, test projections, `@TenantId` behavior, soft-delete filtering

### 8.5 Fix Controller Tests for Spring Boot 4
- `MockHttpServletRequestBuilder.contentType(String)` not `MediaType`
- Fix `UserControllerSecurityTest`, `LoginIntegrationTest`, etc.

---

## Phase 9: Cache Key Tenant Scoping (Audit)

### 9.1 Verify All `@Cacheable` Include Tenant in Key
Fix `UserService.getUserById` key: `#contextTenantId + ':' + #userId`

### 9.2 Add `@Caching` Grouped Evictions on All Mutations

---

## Phase 10: Documentation Cleanup

### 10.1 Update `docs/database.md`
### 10.2 Update `docs/decisions.md`
### 10.3 Update `docs/migrations.md`
### 10.4 Update `docs/reads-hydration.md`

---

## Execution Order

Phase 3 → Phase 4 → Phase 5 → Phase 6 → Phase 7 → Phase 8 → Phase 9 → Phase 10
