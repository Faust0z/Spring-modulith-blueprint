<!-- Load when: writing or editing any test -->

# Testing

Current state of test suite. Each pattern below either distilled from, or points at, real file demonstrating it.

## Tools and how suite runs

- JUnit 5 (Jupiter) + AssertJ + Mockito, from `spring-boot-starter-test:4.1.1`; `spring-security-test` for MockMvc security; Testcontainers `postgresql` + `junit-jupiter` 2.0.5 (Boot-managed versions); `archunit-junit5` 1.5.1; `spring-modulith-starter-test` 2.1.1. All declared in `pom.xml`.
- Full suite (needs Docker): `./mvnw -B test`.
- Targeted classes: `./mvnw -B test -Dtest="ClassName,OtherClass"` (quote filter on Windows PowerShell).
- No-Docker run: `-P local-no-docker` — excludes `LoginIntegrationTest`, `**/*JpaTest`, `AbstractJpaSliceTest` (see `pom.xml`).
- `src/test/resources/junit-platform.properties`: parallel execution disabled + random method order. Tests must be isolated by construction — own tenant, own data, no reliance on run order.
- Database-backed tests use static `pgvector/pgvector:pg18` Testcontainers container per base class (`core/AbstractJpaSliceTest.java`, `BaseIntegrationTest.java`). No in-memory DB.

## Unit tests (service and domain logic)

Best reference: `user/UserServiceTest.java`.

- Skeleton (service tests): `@ExtendWith(MockitoExtension.class)`, `@Mock` for each dependency, `@InjectMocks` for class under test, `@Captor` for batch/collection arguments.
- Pure-value units skip Mockito entirely and call SUT with real inputs (`core/TenantJobRunnerTest.java` — if exists).
- Small security/web units may use static `mock()`/`when()` without extension (e.g. `core/PinAuthorizationSecurityTest.java`, which resets `RequestContextHolder` in `@AfterEach` instead of `TenantContext`).
- One `@Nested` inner class per public method, named after method (`class GetAllUsers { ... }`, `class CreateUser { ... }`).
- Service tests name methods `method_condition_expectedResult` (`getUserById_NonExistingId_ThrowsUserNotFoundException`); pure-value and JPA slice tests use shorter `method_returnsExpected`.
- Assertions are AssertJ: `assertThat(...)` and `assertThatThrownBy(() -> ...).isInstanceOf(...)`.
- Projections and response DTOs that SUT only reads are `mock(...)ed` with stubbed getters.
- Input-variation coverage may use `@ParameterizedTest` (see `auth/CustomUserDetailsServiceTest.java`).

Tenant-scoped services set tenant inside each test and always clear it after, so context never leaks into next test (random method order):

```java
@AfterEach
void tearDown() {
    TenantContext.clear();
}

// inside each tenant-scoped test:
TenantContext.setTenantId(tenantId);
```

## Controller tests — two standardized flavors

Exactly two flavors, each with clear purpose. Do not invent third.

**Flavor A — standalone MockMvc (default for behavior/mapping).**
Best reference: `contacts/ContactControllerTest.java` (if exists).

`@ExtendWith(MockitoExtension.class)`, `@Mock` services, `@InjectMocks` controller. MockMvc built by hand with `LocalValidatorFactoryBean` validator and `PageableHandlerMethodArgumentResolver`, and class carries private `TestSlice<T>` implementation (needed to stub paginated results) + private `createDefaultResponse()` builder. Copy those from reference. This flavor does **not** load Security filter chain.

**Flavor B — `@WebMvcTest` slice (required for security-gated controllers).**
Best reference: `user/UserControllerTest.java`.

Use when controller has authorization semantics (`@RequirePinAuthorization`, role checks, rate limits), because they live in filter chain. Pattern: `@WebMvcTest(XxxController.class)` + `@Import(...MethodSecurityConfig.class)` with `@TestConfiguration` `@EnableMethodSecurity`, `@MockitoBean` for every dependency (including `@MockitoBean(name = "pinAuthorizationSecurity")` to bypass PIN gate), and authentication via `SecurityMockMvcRequestPostProcessors.jwt()` with explicit authorities. `@BeforeEach` stubs `given(pinAuthorizationSecurity.isElevated()).willReturn(true)` for happy path.

Other slice-based security tests: `user/UserControllerSecurityTest.java`.

## JSON serialization tests

Pattern small and stable:

```java
@JsonTest
class ContactResponseJsonTest {
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void serializesFullShape() throws Exception {
        String actual = objectMapper.writeValueAsString(response);
        String expected = """ { /* exact JSON */ } """;
        assertEquals(expected, actual, false);   // JSONAssert, loose mode
    }
}
```

`JSONAssert.assertEquals(expected, actual, false)` (loose) checks whole shape; `.contains("\"field\":null")` covers optional-field nulls. Boot 4.1 / Jackson 3 config what's under test, so `@JsonTest` is right slice — note the test-managed mapper bean is `tools.jackson.databind.ObjectMapper` (jackson2 no longer on the `@JsonTest` classpath); autowire it directly rather than constructing one.

## JPA slice tests (repositories, projections, `@TenantId` behavior)

Best reference: `core/AbstractJpaSliceTest.java`.

- Extend `AbstractJpaSliceTest`: `@DataJpaTest` + `@AutoConfigureTestDatabase(replace = NONE)` + Flyway enabled + `ddl-auto=validate` + tenancy `@Import({HibernateTenancyConfig, TenantIdentifierResolver, JpaConfig, SecurityAuditorAware, AbstractJpaSliceTest.TestCacheConfig})` + `@Transactional(NOT_SUPPORTED)` + static Postgres container + `@DynamicPropertySource`. The nested `TestCacheConfig` supplies a no-op `ConcurrentMapCacheManager` so `@DataJpaTest` slices that pull cache-dependent beans (e.g. `TenantDemoDataResetService`) resolve a `CacheManager` without pulling in the full cache auto-configuration.
- Each test allocates own `UUID tenant`, sets `TenantContext.setTenantId(tenant)`, seeds entities through private helper (`createContact(...)`), asserts on projection/aggregate result. Base class clears context in `@AfterEach`.
- Naming here `method_returnsExpected` (`getDashboardStats_returnsCorrectAggregates`), same short form pure-value unit tests use.
- Repository code Postgres-specific (native queries, views, JSONB), so these tests must run on real Postgres — never H2. `@AutoConfigureTestDatabase(NONE)` mandatory.

## Integration tests

Best reference: `auth/LoginIntegrationTest.java`, base at `BaseIntegrationTest.java`.

- `@ActiveProfiles("test")`, `@SpringBootTest(webEnvironment = RANDOM_PORT)`, Testcontainers wiring via `@DynamicPropertySource`, `app.*` properties from base class (`app.admin.email=admin@test.com`, `app.admin.password=12345678`, dummy Resend key, frontend URL), Flyway enabled, `@AfterEach TenantContext.clear()`.
- HTTP plain `TestRestTemplate`. Cookies not managed by client — extract token from `Set-Cookie` header and replay as `Cookie` request header (see `extractCookie(...)` helper).
- Startup admin bootstrapped from `app.admin.*` by `user/SystemAdminInitializer.java`.
- Only one integration test exists today. Add to it for full-stack flows (cookies, `@RequirePinAuthorization`) that slice tests cannot express.

## Cache tests (`@Cacheable` / Caffeine)

Test profile sets `spring.cache.type=simple`, so caching off everywhere unless test opts in. Cache behavior asserted in dedicated classes that bring up their own tiny cache context.

Best reference: `core/CacheBehaviorTest.java` (also `contacts/ContactServiceCacheTest.java` and `access/AccessLogServiceCacheTest.java`).

Class `@ExtendWith(SpringExtension.class)` + `@ContextConfiguration(classes = ...TestConfig.class)`, where `TestConfig` is `@Configuration` with `@EnableCaching` (`+ @EnableAspectJAutoProxy(proxyTargetClass = true)` when proxied service is real class), `CaffeineCacheManager` bean, and dependencies as `mock(...)` beans. `@AfterEach` clears `TenantContext` and every cache touched. Tenant-scoped caches key entries with tenant (`TenantContext.getTenantId() + ':' + key`); tests assert isolation and eviction via `CaffeineCache.getNativeCache().asMap()`.

## Architecture / structural tests (what actually enforces invariants)

Three mechanisms, each testing different things. There is **no** flat "layer violation" ArchUnit rule.

**`TenancyEnforcementTest`** (`src/test/java/com/faust0z/B2BSystem/TenancyEnforcementTest.java`) — ArchUnit, run manual way: `ClassFileImporter` over app package, `rules()` list checked in loop, and `assertThat(evaluated).isGreaterThan(0)` so rule that accidentally imports nothing still fails. Rules:
1. Every `@Entity` must carry `@TenantId` field (global/system entities allowlisted by name).
2. Every native `@Query` string must contain `tenant_id = :tenantId` (documented cross-tenant/id-scoped queries allowlisted).
3. `UserRepository` reads must be tenant-scoped or allowlisted 2-arg `findById`.

**`SoftDeleteRestrictionTest`** — classpath scan over `@Entity` classes asserting every concrete subclass of `core/TenantSoftDeletableEntity` carries `@SQLRestriction` referencing `deleted_at`.

**`B2BSystemArchitectureTest`** — Spring Modulith `ApplicationModules.of(...)` + `Documenter` (writes PlantUML). Modulith boundary/dependency violations fail at `of(...)` construction; `Documenter` step only produces docs. See `docs/modulith.md`.

## Forbidden patterns

- Do not use `verify(...).update(sql, mock(Object[].class))` for varargs — Mockito cannot materialize array mocks and throws; use `any(Object[].class)` (also avoids `JdbcTemplate.update` overload ambiguity). See `user/SystemAdminInitializerTest.java`.
- Do not leave `TenantContext` set after test — thread-local leaks into next test and method order random.
- Do not start container per test — suite pattern is one `static` container + `@DynamicPropertySource` per base class; per-method containers destroy Spring context caching and add Docker-start cost everywhere.
- Do not use H2 or any in-memory DB for slice tests — repository code Postgres-only; slices force `@AutoConfigureTestDatabase(NONE)` and run Flyway against `pgvector/pgvector:pg18`.
- Do not write domain calculators that call `Instant.now()` directly — thread an `Instant` argument (or `Clock`) so tests inject a deterministic fixture; otherwise the suite goes green today and red every run after (see `RiskLevelCalculator.compute(summary, avgRenewalDays, now)`).
- Do not rely on test execution order or enable parallel JUnit execution — `junit-platform.properties` sets `parallel.enabled=false` and random order on purpose.
- Do not use `@MockBean` in slice tests — suite standardized on Spring Boot 4.1's `@MockitoBean`; using both APIs forks test setup and complicates upgrades.
- Do not write `@ArchTest` classes that can silently import nothing — follow `TenancyEnforcementTest` shape of iterating `rules()` list with `evaluated > 0` guard.
- Do not place tests in different package than (package-private) production class they exercise — package-private code only reachable from its own package (e.g. `core/PinAuthorizationSecurityTest.java`, `user/SystemAdminInitializerTest.java`).

## Fixtures and factories

No shared test-factory/object-mother class today. Test data built by private builder methods owned by each test class (`createEntity`, `createContact`, `createDefaultResponse`). Keeps each file's data shape explicit next to its assertions, but builders duplicated across files — if duplication grows, shared `TestData`/`ObjectMother` would be new decision to record in `docs/decisions.md`, not convention to assume.
