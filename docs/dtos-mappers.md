<!-- Load when: adding or editing request DTO, response DTO, or MapStruct mapper. -->

# DTOs and Mappers

## DTO naming and visibility

- `*CreateRequest` (input, write) — public `record`, strict Jakarta validation, `@Schema(description=..., example=...)` on every field. See `contacts/web/ContactCreateRequest.java`.
- `*UpdateRequest` (input, write) — typically package-private `record`, optional fields (no `@NotBlank`), cross-field guards via `@AssertTrue`. See `contacts/web/ContactUpdateRequest.java`.
- `*Response` (output, read) — public `record`, no validation, every field has `@Schema(description=..., example=...)`. See `contacts/web/ContactResponse.java`.
- Module-shared responses and `*InternalApi` companion DTOs live at module root (e.g. `contacts/ContactAccessInternalResponse.java`, `user/UserAuthInternalResponse.java`, `tenant/TenantInternalResponse.java`).
- Web-layer request DTOs live under each submodule's `web/` folder only in `tenant/`; elsewhere flat at submodule root.

## Validation

- Strict: `@NotBlank`, `@NotNull`, `@Size`, `@Pattern`, `@Email`, `@Past`, `@Positive` (Create requests).
- Lenient: same annotations without `Not*`, plus `@AssertTrue` for cross-field rules (Update requests).
- Nested payloads: annotate nested field with `@Valid`.

## MapStruct mappers

- `@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)`.
- Mapper interfaces **package-private** (no `public`) — exception: `tenant/application/TenantMapper.java` is `public`; keep mappers package-private for new code.
- When mapper owns JSON parsing or other stateful logic, it becomes **abstract class** with protected `@Autowired` field (e.g. `ObjectMapper`).
- For mappers used by both mutations and heavy-list reads, provide two mapping methods with distinct names:
    1. `Response toResponse(Entity entity)` — used by mutations (create). Ignore derived fields unknown at write time (e.g. `fullName`, `canAccess`) with `@Mapping(target = "...", ignore = true)`.
    2. `Response toDetails(Summary summary)` — used by list endpoints. All derived values expressed here via `@Mapping(expression = "java(...)")` and `default` helpers.
- `unmappedTargetPolicy` for read mappers may be `WARN` to catch regressions when response DTO changes. For write-path mappers, keep `IGNORE` and explicit `ignore = true` on write-only targets.
- Always provide standard mutation methods:
    1. `Entity toEntity(CreateRequest req)`
    2. `void updateXxxFromRequest(UpdateRequest req, @MappingTarget Entity entity)` annotated `@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)` so null fields not applied.
- See `contacts/application/ContactMapper.java` for canonical two-method example and `access/application/AccessLogMapper.java` for another example.

## Derived fields and read-shape logic

Derived response values that do not require join belong in mapper, not in view or service helper:

- Concatenations, boolean flags, fallback strings expressed via `@Mapping(target = ..., expression = "java(...)")`.
- Reusable read-shape helpers are `default` methods on mapper interface.

For `JdbcTemplate`-backed list endpoints, use two-stage shape:

1. Static `RowMapper<[Domain]Summary>` extracts columns into package-private `[Domain]Summary` record (e.g. `ContactSummary`, `AccessLogSummary`).
2. Mapper owns derived logic via `toDetails(Summary)` with `@Mapping(expression=...)`.

```java
// Repository returns Slice<ContactSummary>, service maps with Slice.map(mapper::toDetails)
@Mapping(target = "fullName", expression = "java(summary.firstName() + \" \" + summary.lastName())")
@Mapping(target = "canAccess", expression = "java(summary.accessUntil() != null && summary.accessUntil().isAfter(java.time.Instant.now()))")
ContactResponse toDetails(ContactSummary summary);
```

## Mutation response shape

- Mutations return base response DTO (entity values only, no joined/hydrated fields). Example: `createContact` returns `ContactResponse`, not a details variant.
- Void mutations (`voidShift`, `voidEntryLog`) return **no body** (`@ResponseStatus(HttpStatus.NO_CONTENT)`, `void`). Controller returns HTTP 204 with empty payload. See `staffing/application/ShiftService.voidShift` for canonical shape.

## No-re-fetch rule for mutations

After mutation, return response DTO directly from managed entity in memory. Do not re-query database unless action affected data included in response that cannot be computed from entity already in persistence context (e.g. cross-table aggregate populated by listener in same transaction). In that case, re-read is documented exception, not default.

## Service contracts

- Services return DTOs (or projection records) — never JPA entities. `ContactService.getContactById` returns `ContactResponse`; only narrow, deliberately-typed exceptions exist.
- See `docs/caching.md` for how DTOs and cache keys interact.
