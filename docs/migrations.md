<!-- Load when: creating new Flyway migration under `src/main/resources/db/migration`. -->

# Migrations

## File naming

- `V{num}__{description}.sql` — capital V, double underscore, lowercase snake description. Sequence: `V1__init.sql` → `V47__drop_redundant_scanned_at_from_entry_logs.sql` current. Next number is V48. For current column list and last-changed migration of any table/view, see `docs/database.md` — update it in same commit (migrate-on-touch).

## Required patterns

- **UUID primary keys** declared inline: `id UUID PRIMARY KEY,` (no separate `ADD CONSTRAINT`, no `gen_random_uuid()` default — IDs generated application-side). See `V1__init.sql`.
- **FK columns**: `UUID REFERENCES <table>(id) ON DELETE RESTRICT` (or omit clause). See `V1__init.sql`, `V2__gym_tables.sql`.
- **Tenant index**: every tenant-scoped table gets `CREATE INDEX idx_<table>_tenant_id ON <table>(tenant_id);` (V1/V2) or `idx_<table>_tenant` (V2). See `V1__init.sql`, `V2__gym_tables.sql`.
- **Soft-delete-aware indexes** (post-V41) use partial index:
  ```sql
  CREATE UNIQUE INDEX uq_tenant_branch_name ON branches(tenant_id, name) WHERE deleted_at IS NULL;
  ```
  See `V41__soft_delete_gym_entities.sql`.

## Soft delete

- **New tenant-scoped tables**: use `deleted_at TIMESTAMPTZ` (nullable; NULL = active) and entity extends `TenantSoftDeletableEntity`. `V41__soft_delete_gym_entities.sql` migrated gym catalog tables from `is_active` to `deleted_at`.
- **Legacy/system tables** (`users`, `tenants`, `subscription_plans`) still use `is_active BOOLEAN NOT NULL DEFAULT TRUE` and `TenantTogglableEntity` / direct `Boolean isActive`. New columns here should follow legacy convention to stay consistent. `entry_logs`, `membership_sales`, `product_sales`, `product_sale_items`, `worker_shifts` are append-only ledgers — they have no soft-delete flag at all.

## Views

- Prefer **denormalized columns** on entity over view when read value can be computed from entity's own table (e.g. `last_scanned_at` on `members`).
- Use view only for genuine cross-table joins that cannot be denormalized.
- When view needed, naming: `v_<entity>_details` (snake_case plural form: `v_entry_log_details`, `v_worker_shift_details`).
- `CREATE VIEW` for first creation, `DROP VIEW IF EXISTS ... ; CREATE VIEW ...` when shape changes (V41 pattern). `CREATE OR REPLACE VIEW` also used in V7. See `V41__soft_delete_gym_entities.sql` for drop+create style.
- Live views (e.g. `v_entry_log_details`, `v_membership_sales_details`) do **not** filter `deleted_at`. Soft-deleted rows appear with fallback display name `"Socio Eliminado/Desconocido"` via `MemberDisplayNames.DELETED_MEMBER` in mapper. Old `@Immutable` view-entity pattern that relied on `@SQLRestriction` retired (see `docs/reads-hydration.md`).
- Add `tenant_id` of every view so Hibernate `@TenantId` resolves.

## Optimistic locking

- New entities get `version INTEGER NOT NULL DEFAULT 0` column via `V42__add_version_column.sql`'s pattern, and inherit `@Version` from `ImmutableBaseEntity`. Do not re-declare field on entity.
- All catalogued tables now have `version`. (`notifications` out of documentation scope.)

## Other conventions

- Booleans: prefix `is_` (`is_active`, `is_voided`).
- Timestamps: suffix `_at`, type `TIMESTAMPTZ` (`created_at`, `paid_at`, `clock_in_at`).
- Audit: `created_at`, `created_by`, `last_modified_at`, `last_modified_by` (inherited from `ImmutableBaseEntity`; declare only on raw `@Entity` classes that don't extend base).
- Money: `<thing>_ars NUMERIC(12,2)`. See `amount_ars` in `V2__gym_tables.sql`.
- FK columns: `<singular_table>_id` (`membership_id`, `member_id`).
- Index names: `idx_<table>_<cols>` plain, `<table>_<cols>_active` partial, `uq_<table>_<cols>` unique partial.

## Catalog sync

- Update `docs/database.md` in same commit as any migration that creates, alters, renames, or drops table or view. Bump "as of V{next}" header to match.
