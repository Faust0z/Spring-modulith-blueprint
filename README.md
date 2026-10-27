# Catalog Inventory Blueprint

Multi-tenant REST API for managing catalog and access operations across local businesses (gyms, treatment centers, clubs). Enforces strict tenant isolation via `TenantContext` and Hibernate `@TenantId` filtering. Built with Spring Modulith to enforce architectural boundaries between domain modules.

## Key Architecture Points

- Modulith: strict boundaries between modules with `*InternalApi` interfaces at each module root for cross-module communication
- Multi-tenancy: data isolation by `tenant_id` via Hibernate filter and ThreadLocal `TenantContext`
- Stateless authentication: JWT with Spring Security OAuth2 Resource Server
- RBAC authorization: roles `ADMIN`, `TENANT_ADMIN`, `TENANT_USER`
- Local cache: Caffeine with differentiated TTLs and tenant-scoped keys
- SQL projections: `@Immutable` JPA entities mapped from database views for complex reads
- Concurrency: optimistic locking with `@Version`, virtual threads enabled
- Standardized errors: `ProblemDetail` (RFC 9457) with traceId and timestamp
- Audit: `created_by`, `last_modified_by`, `created_at`, `last_modified_at` on mutable entities

## Technical Stack

| Component | Technology |
|------------|------------|
| Language | Java 21 |
| Framework | Spring Boot 3.5.7, Spring Modulith 1.3.0 |
| Server | Undertow |
| Persistence | PostgreSQL, Spring Data JPA / Hibernate 6 |
| Migrations | Flyway |
| Cache | Caffeine |
| DTO Mapping | MapStruct 1.5.5.Final |
| Validation | Jakarta Validation + Hibernate Validator |
| Security | Spring Security, OAuth2 Resource Server (JWT) |
| Documentation | SpringDoc OpenAPI 2.8.15 |
| Testing | JUnit 5, Testcontainers, ArchUnit |
| Metrics | Micrometer + Prometheus |
| Logs | Logstash Logback Encoder |

## Configuration and Deployment (Quick Start)

### Prerequisites

- JDK 21
- PostgreSQL 15+
- Maven 3.9+ (or use the included wrapper)

### Environment variables

```bash
DATABASE_URL=jdbc:postgresql://localhost:5432/catalog_inventory
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
JWT_SECRET=<replace_with_256_bit_secret>
JWT_EXPIRATION=3600000
ACTUATOR_USERNAME=admin
ACTUATOR_PASSWORD=<replace_with_password>
ADMIN_EMAIL=admin@test.com
ADMIN_PASSWORD=<replace_with_password>
CORS_ALLOWED_ORIGINS=http://localhost:3000
RESEND_API_KEY=re_xxx
RESEND_FROM_EMAIL=noreply@example.com
```

### Local startup

```bash
# Compile
./mvnw.cmd compile -q

# Run
./mvnw.cmd spring-boot:run
```

Endpoints:
- API: `http://localhost:8080`
- Actuator: `/actuator/health`, `/actuator/prometheus`
- OpenAPI: `/swagger-ui.html`

Flyway applies migrations automatically on startup (`baseline-on-migrate: true`).

## Modules

The Modulith architecture organizes code into domain modules. Two structural patterns:

**Layered modules** (multi-entity, complex logic):

```
catalog/
├── domain/          # Entities, enums, exceptions
├── infrastructure/  # JPA repositories
├── application/     # Services, mappers
└── web/             # Controllers, request DTOs
```

**Flat modules** (simple entity or infrastructure):

```
auth/
dashboard/
locations/
publics/
registration/
user/
notifications/
```

Each module exposes its public surface via `*InternalApi` interfaces at the module root.

## Domain Logic and Security

### Tenant isolation

Every mutable entity includes `tenant_id`. The Hibernate filter injects the value automatically. `TenantContext` is set per request via `TenantContextFilter`.

### Permission model

| Role | Scope |
|-----|---------|
| `ADMIN` | Full access across all tenants |
| `TENANT_ADMIN` | Administration of own tenant |
| `TENANT_USER` | Basic operations within own tenant |

Composite security annotations:
- `@RequireAdmin`: `hasRole('ADMIN')`
- `@RequirePinAuthorization`: admin PIN elevation for sensitive actions

### Critical business rules

- **Contacts**: DNI unique per tenant. Access valid if `is_active = true` and `access_until > now()`.
- **Locations**: name unique per tenant. Soft delete via `is_active`.
- **Catalog**: products and service plans are created per tenant and assigned to catalog templates. Templates define available items for sale.
- **Orders**: linked to a location and a contact. Items can be products or service plans.
- **Access logs**: registered by DNI scan or token. `ScanType` defines the identification mode.
- **Staff**: attendance records per tenant.

### Registration flow

1. `RegistrationService.register()` creates the tenant, default location, default service plan, and initial admin user.
2. Verification email is sent via `EmailVerificationTokenService`.
3. Tenant becomes active for login once verified.

### Cache

| Cache | TTL |
|-------|-----|
| `dashboard` | 5 min |
| `user_dnis` | 12 h |
| `revocations` | 24 h |
| Default | 30 min |

Keys always include `tenant_id` for isolation. Eviction on write via `@CacheEvict` and `@Caching`.

### Error handling

`GlobalExceptionHandler` centralizes exception mapping to `ProblemDetail` (RFC 9457) with traceId, timestamp, and Spanish messages. Domain exceptions extend `DomainRuleException` carrying HTTP `title`, `status`, and user-facing detail.

## Architecture Decisions and Limitations (Trade-offs)

- **Virtual threads**: enabled by default. Undertow configured with 1 IO thread. Suitable for I/O-bound workloads (DB, HTTP). Not tuned for CPU-intensive operations.
- **HikariCP**: pool of 2 to 5 connections. Adequate for small deployments. Requires retuning for high volume.
- **Local cache (Caffeine)**: not distributed. In multi-replica deployments, each instance caches independently. No cross-node invalidation.
- **Single database**: shared PostgreSQL schema with `tenant_id` as isolation key. No schema-per-tenant or sharding.
- **Flyway baseline**: `baseline-on-migrate: true` assumes historical migrations have already been applied in existing environments.
- **Modulith verification**: `modules.verify()` detects cycles between modules (e.g. `access` <-> `contacts` via `ScanType`). The project uses Modulith documentation generation as the verification baseline.
- **Technical debt**:
  - The `access` module couples `contacts` domain types (`ScanType`, `ContactAccessInternalResponse`) in its internal API.
  - `InvoiceInternalApi` in `orders` is empty (placeholder for future billing integration).
  - Domain messages and logs are in Spanish. Standardizing to English would ease integration with international tooling.
