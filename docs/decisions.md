# Decisions

Settled decisions from codebase change history (commit/PR sets), recorded here so they do not live as historical prose in topic docs. Topic docs current-state only; anything worth remembering as change lives here.

## D1 — Heavy-table reads use two-stage JdbcTemplate shape, no view entities

- **Recorded:** 2026-09-18, from commit `9a5d8cd` (PR #75, "Feat/tenancy and hydration/mapping improvements").
- **Decision:** Heavy-table domains (members, membershipSales, productSales, workerShifts, entryLogs) read through `v_*` SQL views via domain-owned `JdbcTemplate` repository with static `RowMapper<[Domain]Summary>`. Second record mapped by `[Domain]Mapper` (MapStruct), which computes derived values (`canAccess`, `fullName`, etc.). No `v_*` view has `@Immutable` Hibernate view entity; JPA never hydrates them.
- **Consequences:**
  - Lists return `Slice` (no `COUNT(*)` roundtrip).
  - Derived-value computation lives in `[Domain]Mapper` (`MoneyFormatter`/`NameFormatter` standardize common hydration).
  - `MemberEntity.lastScannedAt` stamped at check-in, removing need for `MemberDetailsView`; `entry_logs.scanned_at` removed (auditing uses `created_at`).
  - Methods return DTO records instead of hydrated view options; void/delete methods return nothing.
  - Membership extension no longer uses event; `MemberRegistrationService` orchestrates `*InternalApi` calls synchronously in same transaction (see `docs/modulith.md`).
- **Current state documented in:** `docs/reads-hydration.md` (§Read pattern B, §Heavy-table reads) and `docs/database.md` (view shapes).

## D2 — Cloudinary uploads: deterministic public IDs, tenant-pinned folders, backend-signed direct client uploads

- **Recorded:** 2026-09-22.
- **Decision:** Cloudinary uploads use deterministic public IDs `tenants/{tenant_id}/{asset_type}/{entity_id}`. Backend signs upload params (public_id, timestamp, overwrite, invalidate) via stdlib SHA-1, never exposes API secret. Frontend uploads directly to Cloudinary multipart endpoint; backend only stores returned public_id + version in Postgres. URL composition happens client-side using `VITE_CLOUDINARY_CLOUD_NAME` and `/v{version}/` segment for cache busting.
- **Contract:**
  - Signature: `POST /v1/uploads/cloudinary-signature` (any authenticated tenant user; PIN gate dropped 2026-09-23 so kiosk/registration can upload member photos) body `{ entityType, entityId? }`.
  - Response: `CloudinarySignatureResponse` exposes `folder` (directory) + `publicId` (filename only) + `timestamp` + `overwrite` + `invalidate` + `signature`. Frontend uploads both `folder` and `public_id` (filename only) to Cloudinary; both are included in the HMAC signing string in alphabetical order. Cloudinary resolves the final asset path as `folder + "/" + public_id`; the frontend stores the resolved full path returned by Cloudinary in the completion PATCH.
  - Resolved paths: `pfp`/`bg` → `tenants/{tid}/{name}`; `worker`/`member` → `tenants/{tid}/workers|members/{uuid}`. No schema change (stored `avatarPublicId` values remain valid).
  - Supported types: `pfp`, `bg` (tenant-level, no entityId), `worker`/`member` (UUID entityId).
  - Completion: worker → fold into `PATCH /v1/gyms/workers/{id}` with `avatarPublicId` + `avatarVersion`. Member → fold into `PATCH /v1/gyms/members/{id}` with `avatarPublicId` + `avatarVersion`. Tenant assets → `PUT /v1/tenants/me/assets` body `{ assetType, publicId, version }`.
- **Storage:**
  - Workers: `avatar_public_id varchar(255)`, `avatar_version bigint` (V51 migration backfills legacy `avatar_url` by parsing; legacy column dropped).
  - Members: `avatar_public_id varchar(255)`, `avatar_version bigint` (V51; no legacy column to backfill).
  - Tenants: `pfpPublicId`, `pfpVersion`, `bgPublicId`, `bgVersion` keys in `settings` JSONB. Legacy `logoUrl` key removed.
- **Consequences:**
  - No Cloudinary SDK dependency (stdlib SHA-1 for signing; frontend builds CDN URLs).
  - Response records carry raw refs (publicId + version); consumers compose URLs with `/f_auto,q_auto/v{version}/{publicId}`.
  - Face-scan/entry-log responses (`MemberAccessInternalResponse`, `MemberAccessPublicResponse`, `EntryLogResponse`, `MemberResponse`, `MemberSummary`) surface member `avatarPublicId` + `avatarVersion` so kiosks can render the matched member photo.
  - Legacy absolute Cloudinary URLs (`avatar_url`, `settings.logoUrl`) dropped cleanly; orphaned Cloudinary assets are deferred for cleanup via Admin API.
  - Worker/member read paths (`WorkerResponse`, `WorkerShiftResponse`, `WorkerShiftSummary`, `MemberResponse`, `EntryLogResponse`) expose raw refs; frontend owns URL composition.
- **Current state documented in:** `docs/database.md` (V51, workers, views) and frontend contract in this file.

## D3 — Spring Boot 4.1.1 upgrade (Java 25, Tomcat, Jackson 3)

- **Recorded:** 2026-10-16, from upgrade work.
- **Decision:**
  - Runtime moved to Java 25. Parent `spring-boot-starter-parent` → `4.1.1`, Spring Modulith `2.1.1` (via BOM), MapStruct `1.6.3`, springdoc `3.1.1`, Testcontainers `2.0.5`, ArchUnit `1.5.1`, logstash-logback-encoder `8.1`.
  - Embedded server: Undertow dropped (Boot 4 / Servlet 6.1 no longer ships Undertow) → `spring-boot-starter-webmvc` (Tomcat 11). Removed now-dead `spring.undertow` YAML (it was also nested under `spring:`, never applied).
  - JSON stack: Jackson 3 (`tools.jackson.databind.*`). `@JsonTest` managed mapper bean is Jackson 3; autowire `tools.jackson.databind.ObjectMapper`.
  - Removed `spring-boot-properties-migrator` (migration aid done) and `spring.main.allow-bean-definition-overriding` (springdoc 3.1.1 no longer conflicts).
  - Test slices: `spring-boot-starter-*-test` artifact names replaced to Boot 4 modules (`data-jpa-test`/`webmvc-test`/`jackson-test`/`restclient-test`/`cache-test`), Testcontainers artifacts renamed to `testcontainers-postgresql` + `testcontainers-junit-jupiter`.
  - `@DataJpaTest` slices don't get a `CacheManager` (Boot 4 excludes cache autoconfig from the slice), so `AbstractJpaSliceTest` provides a no-op `ConcurrentMapCacheManager` via nested `TestCacheConfig`.
  - `GlobalExceptionHandler` no longer emits `type` when ProblemDetail type is the default `about:blank` (Spring 7 / RFC 9457 alignment); tests assert `doesNotExist()`.
  - `RiskLevelCalculator.compute` now takes an `Instant now` overload so time-sensitive tests are deterministic (previously time-bombed against `Instant.now()` with fixed fixtures).
  - Added `spring-boot-starter-restclient` to main deps: Boot 4 moved `RestClient.Builder` autoconfiguration into the `spring-boot-restclient` module (excluded from `spring-boot-starter-webmvc`), so `EmailService` (which injects `RestClient.Builder`) needs this starter at runtime. Removed the unused `spring-boot-starter-restclient-test` test-scoped starter — it was the only transitive source of `RestClient.Builder` on the test classpath and masked this gap (tests shadow main-classpath bean-availability checks).
- **Consequences:**
  - Build/test require JDK 25 locally and in CI (`actions/setup-java` bumped to 25); Docker base images switched to Temurin 25.
  - Memory-constrained 512MB runtime unchanged; Tomcat thread/HTTP config defaults re-introduced (the `server.undertow` tuning never took effect before). Consider `server.tomcat.threads.max` as a future knob.
- **Current state documented in:** `README.md` (tech stack), `AGENTS.md` (header), `docs/testing.md` (slices, Jackson 3, cache type).
