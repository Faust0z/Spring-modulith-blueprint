# Implementation Plan: Blueprint Project → Doc Standards Alignment

## Overview
Update the generic B2B catalog/inventory blueprint to exemplify all architectural patterns from the gym ERP docs, **without changing the domain** (keep access, auth, catalog, contacts, locations, orders, staffing, tenant, user modules).

---

## Phase 1: Tech Stack Upgrade (Foundation)

### 1.1 pom.xml — Version Bumps
| Dependency | Current | Target |
|---|---|---|
| Java | 21 | 25 |
| Spring Boot | 3.5.7 | 4.1.1 |
| Spring Modulith | 1.3.0 (BOM) | 2.1.1 (BOM) |
| MapStruct | 1.5.5.Final | 1.6.3 |
| Testcontainers | 2.0.3 / BOM 1.21.3 | 2.0.5 |
| ArchUnit | 1.3.0 | 1.5.1 |
| springdoc | 2.8.15 | 3.1.1 |
| logstash-logback-encoder | 8.0 | 8.1 |

### 1.2 Server Migration: Undertow → Tomcat
- Remove `<exclusion>` for `spring-boot-starter-tomcat` from `spring-boot-starter-web`
- Remove `spring-boot-starter-undertow` dependency
- Keep `spring-boot-starter-web` (Boot 4 now pulls Tomcat by default)
- Remove `spring.undertow` config if present
- Add `spring-boot-starter-restclient` (Boot 4 moved `RestClient.Builder` autoconfig here)

### 1.3 Jackson 3 Migration
- Update `@JsonTest` tests to autowire `tools.jackson.databind.ObjectMapper`
- Verify no jackson2 annotations remain

---

## Phase 2: Entity Base Class Hierarchy (Core)

### 2.1 Create Missing Base Classes in `core/`
1. **`ImmutableBaseEntity`** — add `@Version` field (currently missing)
   ```java
   @Version @Column(name = "version", nullable = false) private Integer version;
   ```

2. **`TenantMutableEntity`** — extends `ImmutableBaseEntity`
   - `@TenantId @Column(name="tenant_id", nullable=false, updatable=false) UUID tenantId`
   - `lastModifiedAt`, `lastModifiedBy`, `isNew` tracking

3. **`TenantSoftDeletableEntity`** — extends `TenantMutableEntity`
   - `deletedAt` (Instant), `softDelete()` method
   - Add `@SQLRestriction("deleted_at IS NULL")` on entity

4. **`TenantTogglableEntity`** — extends `TenantMutableEntity`
   - `isActive` Boolean (legacy tables: tenants, subscription_plans)

5. **`SystemMutableEntity`** — extends `ImmutableBaseEntity`
   - No `tenantId`
   - `isActive`, `lastModifiedAt`, `lastModifiedBy`
   - For hybrid tables like `users` (tenant users + global admins)

### 2.2 Migrate Existing Entities to Correct Base Class
| Entity | Current Base | Target Base | Table |
|---|---|---|---|
| `AccessLogEntity` | `ImmutableBaseEntity` | `ImmutableBaseEntity` ✓ | `access_logs` (append-only) |
| `OrderEntity` | `ImmutableBaseEntity` | `ImmutableBaseEntity` ✓ | `orders` (append-only) |
| `OrderItemEntity` | `ImmutableBaseEntity` | `ImmutableBaseEntity` ✓ | `order_items` (append-only) |
| `InvoiceEntity` | `ImmutableBaseEntity` | `ImmutableBaseEntity` ✓ | `invoices` (append-only) |
| `ShiftEntity` | `MutableBaseEntity` | `ImmutableBaseEntity` | `shifts` (append-only, no soft delete) |
| `Location` | `MutableBaseEntity` | `TenantSoftDeletableEntity` | `branches` → add `deleted_at` |
| `StaffEntity` | `MutableBaseEntity` | `TenantSoftDeletableEntity` | `staff` → add `deleted_at` |
| `ProductEntity` | `MutableBaseEntity` | `TenantSoftDeletableEntity` | `products` → add `deleted_at` |
| `ServicePlanEntity` | `MutableBaseEntity` | `TenantSoftDeletableEntity` | `service_plans` → add `deleted_at` |
| `ContactEntity` | `MutableBaseEntity` | `TenantSoftDeletableEntity` | `contacts`/`members` → add `deleted_at` |
| `CatalogTemplateEntity` | `MutableBaseEntity` | `TenantSoftDeletableEntity` | `catalog_templates` → add `deleted_at` |
| `Tenant` | `MutableBaseEntity` | `TenantTogglableEntity` | `tenants` (legacy `is_active`) |
| `User` | `MutableBaseEntity` | `SystemMutableEntity` | `users` (hybrid, nullable tenant_id) |
| `BillingCycleEntity` | `MutableBaseEntity` | `TenantMutableEntity` | `billing_cycles` (no soft delete per docs) |
| `SubscriptionPlanEntity` | `MutableBaseEntity` | `TenantTogglableEntity` | `subscription_plans` (legacy `is_active`) |
| `EmailVerificationTokenEntity` | — | `ImmutableBaseEntity` | `email_verification_tokens` (system, no tenant) |

### 2.3 Create New Migration Files (V2, V3, V4...)
Per docs: `V{num}__{description}.sql`, UUID PKs, `tenant_id` indexed, partial indexes for soft delete.
- **V2__add_version_columns.sql** — add `version INTEGER NOT NULL DEFAULT 0` to all tables missing it
- **V3__migrate_to_soft_delete.sql** — add `deleted_at TIMESTAMPTZ` to 8 tables, create partial unique indexes, drop `is_active` where replaced
- **V4__fix_billing_cycles_timestamps.sql** — change `TIMESTAMP` → `TIMESTAMPTZ` on `period_start/end`
- **V5__add_audit_columns.sql** — add `created_by`, `last_modified_by` where missing
- Update `docs/database.md` in same commit (migrate-on-touch)
- Update `docs/migrations.md` "Next number is V6"

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

**`CycleRollEngine.recordNightlyPeaks()`** — replace `findActivesNative()` (returns managed entities across tenants) with:
```java
List<UUID> activeCycleIds = billingCycleRepository.findActiveIds(); // scalar IDs only
TenantJobRunner.forEach(activeCycleIds, cycleId -> {
    BillingCycleEntity cycle = billingCycleRepository.findByIdForWrite(cycleId).orElseThrow();
    // ... existing logic
});
```
- Add `findActiveIds()` to `BillingCycleRepository` (native query returning `List<UUID>`)

**`CycleRollEngine.executeNightlyRoll()`** — already uses `findTenantIdsWithExpiredActive()` (returns scalars) + per-tenant loop → add `SYSTEM_SCOPE` wrap around the ID fetch.

### 3.4 Update `TenancyEnforcementTest` Allowlists
- Add new entity class names to `@Entity` allowlist: `Location`, `StaffEntity`, `ProductEntity`, `ServicePlanEntity`, `ContactEntity`, `CatalogTemplateEntity`
- Add to native query allowlist:
  - `ShiftRepository.closeUnfinishedShifts`
  - `BillingCycleRepository.findActiveIds`
  - `BillingCycleRepository.findTenantIdsWithExpiredActive`

---

## Phase 4: AuthLookupRepository (Pre-Context Identity Reads)

### 4.1 Create `AuthLookupRepository` in `user/infrastructure/`
- Uses `JdbcTemplate` (no Hibernate session, no `CurrentTenantIdentifierResolver`)
- Methods returning projections (not entities):
  - `Optional<UserAuthInternalResponse> findUserAuthByIdentifier(String identifier)`
  - `Optional<UserAuthInternalResponse> findUserById(UUID userId)`
  - `boolean existsByEmail(String email)`
  - `boolean existsByUsername(String username)`
  - `Optional<UUID> getTenantIdByUserId(UUID userId)`

### 4.2 Refactor `UserService` Pre-Context Methods
- `getUserAuthInternalByIdentifier()` → delegate to `AuthLookupRepository`
- `getUserResponseByIdentifier()` → delegate to `AuthLookupRepository`
- `existsByEmail()`, `existsByUsername()` → delegate to `AuthLookupRepository`
- `getUserResponseById(UUID)` → delegate to `AuthLookupRepository`
- `getTenantIdByUserId(UUID)` → delegate to `AuthLookupRepository`

### 4.3 Keep Tenant-Scoped Reads on `UserRepository`
- `findByIdAndTenantId()`, `findAllByTenantId()` remain for post-context CRUD
- Remove `findById(UUID, Class<T>)` without tenant from `UserRepository` (unsafe post-bootstrap)

---

## Phase 5: JdbcTemplate Two-Stage Reads (Heavy Tables)

### 5.1 Two Heavy Tables: `contacts` + `access_logs`
Both map to gym `members`/`entry_logs` patterns.

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

**ContactDetailsRepository** (JdbcTemplate):
- Static `RowMapper<ContactSummary>` mapping snake_case columns from `v_contact_details` view
- Method: `Slice<ContactSummary> findAllByTenantId(UUID tenantId, Pageable pageable)`

**ContactMapper** (new, package-private):
- `ContactResponse toResponse(ContactEntity entity)` — mutation path, ignore derived
- `ContactResponse toDetails(ContactSummary summary)` — list path, compute derived:
  - `fullName = firstName + " " + lastName`
  - `canAccess = MembershipExpiryCalculator.canAccess(accessUntil)`
  - `lastScannedAt` from view subquery

**ContactService.getAllContactsPaged()**:
```java
Slice<ContactSummary> slice = contactDetailsRepository.findAllByTenantId(tenantId, pageable);
return slice.map(contactMapper::toDetails);
```

### 5.3 `access_logs` — AccessLogSummary + AccessLogDetailsRepository + AccessLogMapper
**AccessLogSummary** (package-private record in `access/domain/`):
```java
record AccessLogSummary(
    UUID id, UUID tenantId, UUID contactId, String dni, String firstName,
    String lastName, String currentMembershipName, Instant accessUntil,
    Instant scannedAt, AccessStatus status, int contactDebtAmount,
    UUID locationId, String locationName
) {}
```

**AccessLogDetailsRepository** (JdbcTemplate):
- Static `RowMapper<AccessLogSummary>` from `v_access_log_details` view
- Method: `Slice<AccessLogSummary> findAllByTenantId(UUID tenantId, Pageable pageable)`

**AccessLogMapper** (package-private):
- `AccessLogResponse toResponse(AccessLogEntity entity)` — mutation path
- `AccessLogResponse toDetails(AccessLogSummary summary)` — list path, compute derived

**AccessLogService.getAllAccessLogsPaged()**: two-stage pattern.

### 5.4 Update `ContactInternalApi.countContactsWithAccess()`
- Move implementation to `ContactInternalApi` impl using `ContactDetailsRepository`
- `CycleRollEngine` calls `contactInternalApi.countContactsWithAccess(tenantId, now)`

---

## Phase 6: DTO/Mapper Conventions

### 6.1 Make All Mappers Package-Private
- Remove `public` from all `@Mapper` interfaces
- Exception: `tenant/application/TenantMapper.java` stays `public` per docs

### 6.2 Apply Two-Method Pattern Where Needed
For mappers used by both mutations AND heavy-list reads:

```java
// Mutation path (create/update) — ignores derived fields
@Mapping(target = "fullName", ignore = true)
@Mapping(target = "canAccess", ignore = true)
ContactResponse toResponse(ContactEntity entity);

// List path (heavy reads) — computes derived fields
@Mapping(target = "fullName", expression = "java(summary.firstName() + \" \" + summary.lastName())")
@Mapping(target = "canAccess", expression = "java(MembershipExpiryCalculator.canAccess(summary.accessUntil()))")
ContactResponse toDetails(ContactSummary summary);
```

### 6.3 Target Mappers
| Mapper | Changes |
|---|---|
| `LocationMapper` | package-private, add `toDetails(LocationSummary)` |
| `ContactMapper` | new, two-method pattern |
| `AccessLogMapper` | package-private, two-method pattern |
| `UserMapper` | package-private |
| `ProductMapper` | package-private |
| `ServicePlanMapper` | package-private |
| `CatalogTemplateMapper` | package-private |
| `OrderMapper` | package-private |
| `InvoiceMapper` | package-private |
| `StaffMapper` | package-private |
| `ShiftMapper` | package-private |

---

## Phase 7: InternalApi Module Root Placement

### 7.1 Verify All at Module Root (per docs)
Each top-level package = module root. Current state ✓:
- `access/AccessLogInternalApi.java`
- `auth/AuthInternalApi.java`
- `catalog/CatalogTemplateInternalApi.java`, `ProductInternalApi.java`, `ServicePlanInternalApi.java`
- `contacts/ContactInternalApi.java`
- `locations/LocationInternalApi.java`
- `orders/OrderInternalApi.java`, `InvoiceInternalApi.java`
- `registration/RegistrationInternalApi.java`
- `staffing/ShiftInternalApi.java`, `StaffInternalApi.java`
- `tenant/TenantInternalApi.java`, `BillingCycleInternalApi.java`
- `user/UserInternalApi.java`

**Companion DTOs** at same level: `AccessLogInternalResponse`, `UserAuthInternalResponse`, etc.

---

## Phase 8: Test Infrastructure

### 8.1 `AbstractJpaSliceTest` in `core/test/`
```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@Import({
    HibernateTenancyConfig.class,
    TenantIdentifierResolver.class,
    JpaConfig.class,
    SecurityAuditorAware.class,
    AbstractJpaSliceTest.TestCacheConfig.class
})
@Transactional(NOT_SUPPORTED)
abstract class AbstractJpaSliceTest {

    @Configuration
    @EnableCaching
    static class TestCacheConfig {
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }
    }

    @RegisterExtension
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg18");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
    }

    @AfterEach
    void clearContext() { TenantContext.clear(); }
}
```

### 8.2 `SoftDeleteRestrictionTest` in root test
```java
@AnalyzeClasses(packages = "com.faust0z.B2BSystem")
class SoftDeleteRestrictionTest {
    @ArchTest
    static final ArchRule soft_deletable_entities_have_sql_restriction = classes()
        .that().areAnnotatedWith(Entity.class)
        .and().areAssignableTo(TenantSoftDeletableEntity.class)
        .should(haveSqlRestrictionOnDeletedAt())
        .because("TenantSoftDeletableEntity requires @SQLRestriction(\"deleted_at IS NULL\")");
}
```

### 8.3 Cache Tests
- `core/CacheBehaviorTest.java` — tenant-aware key isolation using `CaffeineCache.getNativeCache().asMap()`
- `contacts/ContactServiceCacheTest.java` — `@Cacheable` behavior with Caffeine, tenant-scoped keys
- `access/AccessLogServiceCacheTest.java` — same for access logs

### 8.4 JPA Slice Tests for Each Repository
- Extend `AbstractJpaSliceTest`
- Test projections, `@TenantId` behavior, soft-delete filtering
- One per module: `ContactRepositoryJpaTest`, `AccessLogRepositoryJpaTest`, `LocationRepositoryJpaTest`, etc.

### 8.5 Update Existing Tests
- All tests: add `@AfterEach TenantContext.clear()`
- Replace `@MockBean` → `@MockitoBean`
- Fix `verify(...).update(sql, mock(Object[].class))` → `any(Object[].class)`

---

## Phase 9: Cache Key Tenant Scoping (Audit)

### 9.1 Fix `UserService.getUserById` Cache Key
```java
// From: @Cacheable(value = "users", key = "#userId")
// To:   @Cacheable(value = "users", key = "#contextTenantId + ':' + #userId")
```

### 9.2 Audit All `@Cacheable` Methods
| Service/Method | Current Key | Fixed Key |
|---|---|---|
| `UserService.getUserById` | `#userId` | `#contextTenantId + ':' + #userId` |
| `ContactService.getContactById` | `#contactId` | `#contextTenantId + ':' + #contactId` |
| `AccessLogService.getAccessLogById` | `#logId` | `#contextTenantId + ':' + #logId` |
| `LocationService.getLocationById` | `#locationId` | `#contextTenantId + ':' + #locationId` |
| `ProductService.getProductById` | `#productId` | `#contextTenantId + ':' + #productId` |
| `StaffService.getStaffById` | `#staffId` | `#contextTenantId + ':' + #staffId` |

### 9.3 Verify `@Caching` Grouped Evictions on All Mutations
- `UserService` ✓ (updateUser, changeUserRole, toggleUserStatus)
- Add to `ContactService`, `AccessLogService`, `LocationService`, `ProductService`, `StaffService`, `ServicePlanService`, `CatalogTemplateService`

---

## Phase 10: Documentation Cleanup

### 10.1 Update `docs/database.md`
- Remove V2–V51 history (not applicable to this blueprint)
- Document current schema at V5 (after V2–V5 migrations)
- Keep only tables that exist in this blueprint (16 tables + 4 views)

### 10.2 Update `docs/decisions.md`
- Remove gym-specific decisions (D1, D2)
- Keep D3 (Spring Boot 4 upgrade) — now implemented
- Add new decisions:
  - D4: Entity base class hierarchy with 5 base classes
  - D5: AuthLookupRepository for pre-context identity reads
  - D6: Two-stage JdbcTemplate reads for heavy tables (contacts, access_logs)
  - D7: TenantJobRunner for cross-tenant scheduled jobs

### 10.3 Update `docs/migrations.md`
- Reset "Next number is V6" after V5 migration
- Document V2–V5 conventions used

### 10.4 Update `docs/reads-hydration.md`
- Keep patterns, remove gym-specific examples
- Add `ContactSummary`/`ContactDetailsRepository` and `AccessLogSummary`/`AccessLogDetailsRepository` as canonical examples

---

## Execution Order & Dependencies

```
Phase 1 (Tech Stack) 
    ↓
Phase 2.1 (Base Classes) → Phase 2.2 (Entity Migration) → Phase 2.3 (Migration SQL V2-V5)
    ↓
Phase 3 (Multi-Tenancy Utils) → Phase 4 (AuthLookupRepository)
    ↓
Phase 5 (JdbcTemplate Reads: contacts + access_logs)
    ↓
Phase 6 (Mapper Conventions)
    ↓
Phase 7 (InternalApi Audit) — independent, can run anytime
    ↓
Phase 8 (Test Infra) — needs Phases 1-3 working
    ↓
Phase 9 (Cache Audit)
    ↓
Phase 10 (Doc Cleanup)
```

---

## Estimated File Changes

| Category | Files to Create | Files to Modify |
|---|---|---|
| Base Entities | 4 (TenantMutable, TenantSoftDeletable, TenantTogglable, SystemMutable) | 1 (ImmutableBaseEntity +version) |
| Entity Migration | 0 | ~15 entities |
| Migration SQL | 4 (V2-V5) | 0 |
| Multi-Tenancy Utils | 2 (TenantContext.SYSTEM_SCOPE, TenantJobRunner) | 2 scheduled jobs |
| AuthLookupRepository | 1 repo + 1 interface | 1 (UserService) |
| JdbcTemplate Reads | 6 (2 Summaries, 2 Repos, 2 Mappers) | 2 services (Contact, AccessLog) |
| Mappers | 2 (ContactMapper, AccessLogMapper) | ~10 mappers (visibility + two-method) |
| Tests | 5 (AbstractJpaSliceTest, SoftDeleteRestrictionTest, CacheBehaviorTest, ContactServiceCacheTest, AccessLogServiceCacheTest + 8 JPA slice tests) | ~10 existing tests |
| Docs | 0 | 4 docs |

---

## Key Implementation Notes

1. **Migration Order**: Run `./mvnw flyway:migrate` after each V-file to verify before next
2. **Entity Migration**: Update imports, extend new base class, add `@SQLRestriction` where needed, remove manual `isActive`/`deletedAt` fields if inherited
3. **AuthLookupRepository**: Use `JdbcTemplate` with `queryForObject` + `RowMapper<UserAuthInternalResponse>`; handle nullable `tenant_id` for global admins
4. **Two-Stage Reads**: Keep `Summary` records package-private; mappers in same package as entity/service
5. **Cache Keys**: Use SpEL `#contextTenantId` parameter consistently; avoid `TenantContext.getTenantId()` in keys (proxy issues)
6. **TenancyEnforcementTest**: Run after Phase 2.2 to catch missing `@TenantId` or `@SQLRestriction` early

---

## Ready for Execution

All 6 clarifying questions resolved. Plan is complete and detailed enough for implementation.
