<!-- Load when: you need the current shape of a table, view, or index, or are writing a migration and must know what already exists. -->

# Database state

Schema state as of **V51** — last updated 2026-09-23.
Bump this header to next V number in same commit as any migration that creates, alters, renames, or drops table or view (migrate-on-touch).

Column lists below are diff-accumulated state at V51. For migration lineage see each table's "last changed" reference. For conventions that govern migrations, see `docs/migrations.md`. V46 drops real-estate module (V25–V32) — those tables and views no longer present after migration runs. V47 drops redundant `entry_logs.scanned_at` column (auditing uses inherited `created_at`) and recreates `v_entry_log_details` on `created_at`. V50 adds `face_embedding` (vector) to `members` plus HNSW index `idx_members_face_embedding`. V51 drops legacy `workers.avatar_url` and `tenants.settings.logoUrl`, adds deterministic Cloudinary asset refs to `workers`, `members` and `tenants.settings`. V52 drops `members.face_search_active` and recreates `idx_members_face_embedding` with predicate `face_embedding IS NOT NULL`.

## identity

### tenants
- Entity: `TenantEntity` (`SystemMutableEntity` — global, no `tenant_id`)
- Soft delete: `is_active BOOLEAN NOT NULL DEFAULT TRUE` (legacy)
- Version: yes (V42)
- Created: `V1__init.sql` | Last changed: `V42__add_version_column.sql`
- Columns: `id, name, tax_id, settings, is_active, created_at, created_by, last_modified_at, last_modified_by, valid_until, slug, version`
- Indexes: `uq_tenant_slug UNIQUE(slug)` (V20)

### users
- Entity: `UserEntity` (`SystemMutableEntity` — hybrid: tenant users + global admins; `tenant_id` nullable)
- Soft delete: `is_active BOOLEAN NOT NULL DEFAULT TRUE` (legacy)
- Version: yes (V42)
- Created: `V1__init.sql` | Last changed: `V43__add_user_email_validated.sql`
- Columns: `id, email, password, role, tenant_id, is_active, created_at, created_by, last_modified_at, last_modified_by, terms_accepted_at, username, admin_pin, full_name, version, email_validated_at`
- Indexes: `uq_global_email UNIQUE(email)`, `uq_username UNIQUE(username)`, `chk_user_credentials CHECK`

### branches
- Entity: `BranchEntity` (`TenantSoftDeletableEntity`)
- Soft delete: `deleted_at TIMESTAMPTZ` (V41)
- Version: yes (V18, inherited from creation)
- Created: `V18__add_branches.sql` | Last changed: `V41__soft_delete_gym_entities.sql`
- Columns: `id, tenant_id, name, created_at, created_by, last_modified_at, last_modified_by, version, deleted_at`
- Indexes: `uq_tenant_branch_name UNIQUE(tenant_id, name) WHERE deleted_at IS NULL` (V41)


### subscription_plans
- Entity: `SubscriptionPlanEntity` (`TenantTogglableEntity`)
- Soft delete: `is_active BOOLEAN NOT NULL DEFAULT TRUE` (legacy)
- Version: yes (V24)
- Created: `V24__add_subscription_plans_and_billing_cycles.sql` | Last changed: `V36__columns_cleanup_for_better_registration.sql`
- Columns: `id, tier, price, version, is_active, created_at, created_by, last_modified_at, last_modified_by, max_members`

### billing_cycles
- Entity: `BillingCycleEntity` (`TenantMutableEntity`)
- Soft delete: none (`is_active` dropped V41)
- Version: yes (V24)
- Created: `V24__add_subscription_plans_and_billing_cycles.sql` | Last changed: `V41__soft_delete_gym_entities.sql`
- Columns: `id, tenant_id, plan_id, period_start, period_end, peak_members, discount_percentage, amount, status, is_demo, created_at, created_by, last_modified_at, last_modified_by, version, remaining_discount_months, next_plan_id`

### email_verification_tokens
- Entity: `EmailVerificationTokenEntity` (system-level, no tenant)
- Soft delete: none
- Version: yes (V42)
- Created: `V37__add_next_plan_id_to_billing_cycles.sql` | Last changed: `V42__add_version_column.sql`
- Columns: `token, user_id, expires_at, version`

## gyms

### members
- Entity: `MemberEntity` (`TenantSoftDeletableEntity`)
- Soft delete: `deleted_at TIMESTAMPTZ` (V41)
- Version: yes (V42)
- Created: `V2__gym_tables.sql` | Last changed: `V52__drop_members_face_search_active.sql`
- Columns: `id, tenant_id, dni, first_name, last_name, qr_token, access_until, current_membership_name, created_at, created_by, last_modified_at, last_modified_by, email, birth_date, phone_number, gender, debt_amount, deleted_at, version, last_scanned_at, avatar_public_id, avatar_version, face_embedding`
- Indexes: `uq_tenant_dni_active UNIQUE(tenant_id, dni) WHERE deleted_at IS NULL` (V41), `uq_tenant_qr_token_active UNIQUE(qr_token) WHERE deleted_at IS NULL` (V41), `idx_members_tenant_last_scanned` (V44), `idx_members_face_embedding` (V52, HNSW on `face_embedding` L2, partial WHERE `deleted_at IS NULL AND face_embedding IS NOT NULL`)

### memberships
- Entity: `MembershipEntity` (`TenantSoftDeletableEntity`)
- Soft delete: `deleted_at TIMESTAMPTZ` (V41)
- Version: yes (V42)
- Created: `V2__gym_tables.sql` (as `membership`) | Last changed: `V42__add_version_column.sql`
- Columns: `id, tenant_id, name, duration_amount, price_ars, created_at, created_by, last_modified_at, last_modified_by, unit, deleted_at, version`

### membership_sales
- Entity: `MembershipSaleEntity` (`ImmutableBaseEntity`)
- Soft delete: none (ledger)
- Version: yes (V42)
- Created: `V2__gym_tables.sql` (as `payments`) | Last changed: `V42__add_version_column.sql`
- Columns: `id, tenant_id, method, amount_ars, amount_days, membership_name, paid_at, membership_id, member_id, created_at, created_by, is_voided, branch_id, version`

### entry_logs
- Entity: `EntryLogEntity` (`ImmutableBaseEntity`)
- Soft delete: none (ledger)
- Version: yes (V42)
- Created: `V2__gym_tables.sql` | Last changed: `V47__drop_redundant_scanned_at_from_entry_logs.sql`
- Columns: `id, tenant_id, status, member_id, created_at, created_by, reason, branch_id, version`

### products
- Entity: `ProductEntity` (`TenantSoftDeletableEntity`)
- Soft delete: `deleted_at TIMESTAMPTZ` (V41)
- Version: yes (V42)
- Created: `V10__memberships_and_products_sale_update.sql` | Last changed: `V42__add_version_column.sql`
- Columns: `id, tenant_id, name, price, created_at, created_by, last_modified_at, last_modified_by, deleted_at, version`

### product_sales
- Entity: `ProductSaleEntity` (`ImmutableBaseEntity`)
- Soft delete: none (ledger)
- Version: yes (V42)
- Created: `V10__memberships_and_products_sale_update.sql` | Last changed: `V42__add_version_column.sql`
- Columns: `id, tenant_id, member_id, payment_method, total_price, is_voided, created_at, created_by, branch_id, version`

### product_sale_items
- Entity: `ProductSaleItemEntity` (`ImmutableBaseEntity`)
- Soft delete: none (ledger)
- Version: yes (V42)
- Created: `V10__memberships_and_products_sale_update.sql` | Last changed: `V42__add_version_column.sql`
- Columns: `id, tenant_id, sale_id, product_id, name, quantity, unit_price, total_price, created_at, created_by, version`

### worker_shifts
- Entity: `WorkerShiftEntity` (`TenantMutableEntity`)
- Soft delete: none (no `deleted_at`; lifecycle via `clock_out_at IS NULL` unique index)
- Version: yes (V12)
- Created: `V12__create_workershifts.sql` | Last changed: `V41__soft_delete_gym_entities.sql`
- Columns: `id, tenant_id, worker_id, clock_in_at, clock_out_at, auto_closed, created_at, created_by, last_modified_at, last_modified_by, version, branch_id`
- Indexes: `idx_worker_shifts_one_active_per_worker UNIQUE(tenant_id, worker_id) WHERE clock_out_at IS NULL` (V41)

### workers
- Entity: `WorkerEntity` (`TenantSoftDeletableEntity`)
- Soft delete: `deleted_at TIMESTAMPTZ` (V41)
- Version: yes (V21)
 - Created: `V21__separate-worker_shifts-from-users.sql` | Last changed: `V51__add_worker_avatar_asset.sql`
 - Columns: `id, tenant_id, first_name, last_name, dni, created_at, created_by, last_modified_at, last_modified_by, version, avatar_public_id, avatar_version, deleted_at`

### routines
- Entity: `RoutineEntity` (`TenantSoftDeletableEntity` — hybrid: `is_active` retained for published-routine toggle)
- Soft delete: `deleted_at TIMESTAMPTZ` (V41) + intentional `is_active BOOLEAN NOT NULL DEFAULT TRUE`
- Version: yes (V19)
- Created: `V19__add_routines.sql` | Last changed: `V41__soft_delete_gym_entities.sql`
- Columns: `id, tenant_id, name, content, is_active, created_at, created_by, last_modified_at, last_modified_by, version, category, deleted_at`
- Indexes: `idx_routines_tenant_active(tenant_id, is_active) WHERE deleted_at IS NULL` (V41)

## Views

### v_worker_shift_details
- Purpose: worker shift list with worker display name
- Joins: `worker_shifts ws JOIN workers w ON ws.worker_id = w.id`
- Filter: none
- Java consumer: `WorkerShiftDetailsRepository` (JdbcTemplate)
 - Created: `V12__create_workershifts.sql` | Last changed: `V51__add_worker_avatar_asset.sql`
 - Columns: `id, tenant_id, worker_id, first_name, last_name, dni, avatar_public_id, avatar_version, clock_in_at, clock_out_at, auto_closed, branch_id`

### v_entry_log_details
- Purpose: entry log list with member display name
- Joins: `entry_logs el JOIN members m ON el.member_id = m.id AND el.tenant_id = m.tenant_id`
- Filter: none
- Java consumer: `EntryLogDetailsRepository` (JdbcTemplate)
- Created: `V7__add_member_attributes_and_view.sql` | Last changed: `V47__drop_redundant_scanned_at_from_entry_logs.sql`
- Columns: `id, tenant_id, member_id, dni, first_name, last_name, current_membership_name, access_until, created_at, status, member_debt_amount, branch_id`

### v_membership_sales_details
- Purpose: membership sale list with member display name
- Joins: `membership_sales ms LEFT JOIN members m ON ms.member_id = m.id AND ms.tenant_id = m.tenant_id`
- Filter: none
- Java consumer: `MembershipSaleDetailsRepository` (JdbcTemplate)
- Created: `V7__add_member_attributes_and_view.sql` (as `v_payments_details`) | Last changed: `V40__membership_unit.sql`
- Columns: `id, tenant_id, method, amount_ars, amount_days, membership_name, paid_at, membership_id, member_id, is_voided, first_name, last_name, dni, branch_id`

### v_product_sales_details
- Purpose: product sale list with member display name + aggregated products JSON
- Joins: `product_sales ps LEFT JOIN members m LEFT JOIN product_sale_items psi ON ps.id = psi.sale_id`
- Filter: none
- Java consumer: `ProductSaleDetailsRepository` (JdbcTemplate)
- Created: `V10__memberships_and_products_sale_update.sql` | Last changed: `V18__add_branches.sql`
- Columns: `id, tenant_id, member_id, payment_method, total_price, created_at, is_voided, dni, member_first_name, member_last_name, products, branch_id`

## Legacy / target conventions

| Convention | Applies to | Reference |
|---|---|---|
| Soft delete via `deleted_at TIMESTAMPTZ` | members, memberships, workers, products, branches | `docs/migrations.md` §Soft delete |
| Soft delete via `is_active BOOLEAN` (legacy) | tenants, users, subscription_plans | `docs/migrations.md` §Soft delete |
| Hybrid (`deleted_at` + `is_active`) | routines — `is_active` toggles published/unpublished for public routines | `docs/migrations.md` |
| Append-only, no soft-delete flag | entry_logs, membership_sales, product_sales, product_sale_items, worker_shifts | `docs/migrations.md` §Soft delete |
| Optimistic locking (`version`) | All catalogued tables | `docs/migrations.md` §Optimistic locking |

Out of documentation scope: `notifications` (V33) — leftover with no Java consumer. Real-estate tables and views removed in V46.
