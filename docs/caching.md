<!-- Load when: adding/editing `@Cacheable`, `@CachePut`, `@CacheEvict`, or any `CacheManager` programmatic use. -->

# Caching (Caffeine)

Cache config lives in `core/CacheConfig.java` (`@EnableCaching`, primary `CaffeineCacheManager`). Per-cache TTLs + max sizes defined there:

- `dashboard` — 5 min, max 50
- `revocations` — 24 h, max 1000
- `user_dnis` — 12 h, max 500
- default — 30 min, max 150
- `rateLimitBuckets` (Bucket4j) — max 1000, `expireAfterAccess(1h)`

`CacheErrorHandler` logs cache failures instead of throwing.

## Tenant scoping rule

- **Tenant-scoped cache values MUST include tenant in key**, otherwise one tenant reads another's data.
- Two idioms in use today:
  - SpEL: `key = "T:com.faust0z.B2BSystem.core.TenantContext.getTenantId() + ':' + #memberId"` — see `gyms/members/MemberService.getMemberById`.
  - Parameter: `key = "#contextTenantId"` when service takes tenant id explicitly — see `identity/user/UserService.getAllUsers`.
- Pick one per cache. Don't mix.

## Evict on mutate — `@Caching`

When mutation invalidates more than one cache (e.g. single-item + list), use `@Caching` to group evictions/puts in one annotation. See `identity/user/UserService.updateUser`:

```java
@Caching(
    put = @CachePut(value = "users", key = "#userId"),
    evict = {
        @CacheEvict(value = "tenant_users", key = "#contextTenantId"),
        @CacheEvict(value = "user_dnis", allEntries = true)
    }
)
```

- `@CachePut` refreshes single-item cache with new value.
- `@CacheEvict(allEntries = true)` only when key shape not expressible in SpEL (e.g. dni → userId lookup table).
- For soft-deletes, `@CacheEvict` (not `@CachePut`); see `MemberService.deleteMember` and `MemberService.anonymizeMember`.

## Programmatic `CacheManager` use

- Reserved for non-entity revocation records. See `UserService` writing to `"revocations"` cache: `cacheManager.getCache("revocations").put("user:" + userId, Instant.now().getEpochSecond())` after password/role/status changes. OAuth2 token validator consults this cache to reject tokens for users whose credentials rotated.

## Cache misses

- When service method that is `@Cacheable` is also called from within same class, call bypasses proxy and skips cache. Move such calls to separate `@Service` bean or use `ApplicationContext` to fetch proxy.
- Resolved case: `TenantService.updateOnboarding` mutates settings directly with its own `@Caching` evictions, eliminating stale-read path.
