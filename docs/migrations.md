<!-- Load when: creating new Flyway migration under `src/main/resources/db/migration`. -->

# Migrations

## File naming

- `V{num}__{description}.sql` — capital V, double underscore, lowercase snake description. Sequence: `V1__initial_schema.sql` → `V5__add_audit_columns.sql` current. Next number is V6. For current column list and last-changed migration of any table/view, see `docs/database.md` — update it in same commit (migrate-on-touch).

## Required patterns

- **UUID primary keys** declared inline: `id UUID PRIMARY KEY,` (no separate `ADD CONSTRAINT`, no `gen_random_uuid()` default — IDs generated application-side). See `V1__initial_schema.sql`.
- **FK columns**: `UUID REFERENCES <table>(id) ON DELETE RESTRICT` (or omit clause). See `V1__initial_schema.sql`.
- **Tenant index**: every tenant-scoped table gets `CREATE INDEX idx_<table>_tenant_id ON <table>(tenant_id);` (V1) or `idx_<table>_tenant` (V1). See `V1__initial_schema.sql`.
- **Soft-delete-aware indexes** (post-V3) use partial index:
  ```sql
  CREATE UNIQUE INDEX uq_tenant_contacts_dni ON contacts(tenant_id, dni) WHERE deleted_at IS NULL;
  ```
  See `V3__migrate_to_deleted_at.sql`.

## Soft delete

- **New tenant-scoped tables**: use `deleted_at TIMESTAMPTZ` (nullable; NULL = active) and entity extends `TenantSoftDeletableEntity`. `V3__migrate_to_deleted_at.sql` migrated soft-deletable tables from `is_active` to `deleted_at`.
- **Legacy/system tables** (`users`, `tenants`, `subscription_plans`) still use `is_active BOOLEAN NOT NULL DEFAULT TRUE` and `TenantTogglableEntity` / direct `Boolean isActive`. New columns here should follow legacy convention to stay consistent. `access_logs`, `orders`, `order_items`, `invoices`, `shifts` are append-only ledgers — they have no soft-delete flag at all.

## Views

- Prefer **denormalized columns** on entity over view when read value can be computed from entity's own table.
- Use view only for genuine cross-table joins that cannot be denormalized.
- When view needed, naming: `v_<entity>_details` (snake_case plural form: `v_contact_details`, `v_access_log_details`, `v_shift_details`, `v_order_details`).
- `CREATE VIEW` for first creation, `DROP VIEW IF EXISTS ... ; CREATE VIEW ...` when shape changes. See `V1__initial_schema.sql` for create style.
- Live views (e.g. `v_contact_details`, `v_access_log_details`) do **not** filter `deleted_at`. Soft-deleted rows appear with fallback display name in mapper.
- Add `tenant_id` of every view so Hibernate `@TenantId` resolves.

## Optimistic locking

- New entities get `version INTEGER NOT NULL DEFAULT 0` column via `V2__add_version_columns.sql`'s pattern, and inherit `@Version` from `ImmutableBaseEntity`. Do not re-declare field on entity.
- All catalogued tables now have `version`. (`notifications` out of documentation scope.)

## Other conventions

- Booleans: prefix `is_` (`is_active`, `is_voided`).
- Timestamps: suffix `_at`, type `TIMESTAMPTZ` (`created_at`, `paid_at`, `clock_in_at`).
- Audit: `created_at`, `created_by`, `last_modified_at`, `last_modified_by` (inherited from `ImmutableBaseEntity`; declare only on raw `@Entity` classes that don't extend base).
- Money: `<thing>_ars NUMERIC(12,2)`. See `amount_paid_ars` in `V1__initial_schema.sql`.
- FK columns: `<singular_table>_id` (`contact_id`, `location_id`).
- Index names: `idx_<table>_<cols>` plain, `<table>_<cols>_active` partial, `uq_<table>_<cols>` unique partial.

## Catalog sync

- Update `docs/database.md` in same commit as any migration that creates, alters, renames, or drops table or view. Bump "as of V{next}" header to match.
