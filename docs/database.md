<!-- Load when: you need the current shape of a table, view, or index, or are writing a migration and must know what already exists. -->

# Database state

Schema state as of **V5** — last updated 2026-10-28.
Bump this header to next V number in same commit as any migration that creates, alters, renames, or drops table or view (migrate-on-touch).

Column lists below are diff-accumulated state at V5. For migration lineage see each table's "last changed" reference. For conventions that govern migrations, see `docs/migrations.md`. V2 adds `version` columns. V3 migrates soft-delete-able tables from `is_active` to `deleted_at`. V4 fixes `billing_cycles` timestamps to `TIMESTAMPTZ`. V5 adds missing audit columns.

## core

### tenants
- Entity: `Tenant` (`TenantTogglableEntity` — global, no `tenant_id`)
- Soft delete: `is_active BOOLEAN NOT NULL DEFAULT TRUE` (legacy)
- Version: yes (V2)
- Created: `V1__initial_schema.sql` | Last changed: `V2__add_version_columns.sql`
- Columns: `id, name, tax_id, settings, is_active, slug, location, created_at, created_by, last_modified_at, last_modified_by, version`
- Indexes: `uq_tenant_slug UNIQUE(slug)` (V1), `idx_tenants_settings` (V1, GIN)

### users
- Entity: `User` (`SystemMutableEntity` — hybrid: tenant users + global admins; `tenant_id` nullable)
- Soft delete: `is_active BOOLEAN NOT NULL DEFAULT TRUE` (legacy)
- Version: yes (V2)
- Created: `V1__initial_schema.sql` | Last changed: `V2__add_version_columns.sql`
- Columns: `id, full_name, email, username, password, dni, role, tenant_id, terms_accepted_at, admin_pin, is_active, created_at, created_by, last_modified_at, last_modified_by, version`
- Indexes: `idx_users_tenant_id` (V1), `uq_username` (V1), `chk_user_credentials` (V1)

## access

### access_logs
- Entity: `AccessLogEntity` (`ImmutableBaseEntity` — append-only ledger)
- Soft delete: none (ledger)
- Version: yes (V2)
- Created: `V1__initial_schema.sql` | Last changed: `V5__add_audit_columns.sql`
- Columns: `id, tenant_id, scanned_at, status, contact_id, location_id, reason, created_at, created_by, last_modified_by, version`
- Indexes: `idx_access_logs_tenant` (V1), `idx_access_logs_contact` (V1), `idx_access_logs_tenant_scanned` (V1), `idx_access_logs_tenant_location_scanned` (V1)

### locations (branches)
- Entity: `Location` (`TenantSoftDeletableEntity`)
- Soft delete: `deleted_at TIMESTAMPTZ` (V3)
- Version: yes (V1)
- Created: `V1__initial_schema.sql` | Last changed: `V3__migrate_to_deleted_at.sql`
- Columns: `id, tenant_id, name, deleted_at, created_at, created_by, last_modified_at, last_modified_by, version`
- Indexes: `idx_locations_tenant` (V1), `uq_tenant_location_name UNIQUE(tenant_id, name) WHERE deleted_at IS NULL` (V3)

## catalog

### products
- Entity: `ProductEntity` (`TenantSoftDeletableEntity`)
- Soft delete: `deleted_at TIMESTAMPTZ` (V3)
- Version: yes (V2)
- Created: `V1__initial_schema.sql` | Last changed: `V3__migrate_to_deleted_at.sql`
- Columns: `id, tenant_id, name, price, deleted_at, created_at, created_by, last_modified_at, last_modified_by, version`
- Indexes: `idx_products_tenant` (V1)

### service_plans
- Entity: `ServicePlanEntity` (`TenantSoftDeletableEntity`)
- Soft delete: `deleted_at TIMESTAMPTZ` (V3)
- Version: yes (V1)
- Created: `V1__initial_schema.sql` | Last changed: `V3__migrate_to_deleted_at.sql`
- Columns: `id, tenant_id, name, duration_days, price, deleted_at, created_at, created_by, last_modified_at, last_modified_by, version`
- Indexes: `idx_service_plans_tenant` (V1)

### catalog_templates
- Entity: `CatalogTemplateEntity` (`TenantSoftDeletableEntity`)
- Soft delete: `deleted_at TIMESTAMPTZ` (V3)
- Version: yes (V1)
- Created: `V1__initial_schema.sql` | Last changed: `V3__migrate_to_deleted_at.sql`
- Columns: `id, tenant_id, name, content, category, deleted_at, created_at, created_by, last_modified_at, last_modified_by, version`
- Indexes: `idx_catalog_templates_tenant` (V1)

## contacts

### contacts (members)
- Entity: `ContactEntity` (`TenantSoftDeletableEntity`)
- Soft delete: `deleted_at TIMESTAMPTZ` (V3)
- Version: yes (V2)
- Created: `V1__initial_schema.sql` | Last changed: `V3__migrate_to_deleted_at.sql`
- Columns: `id, tenant_id, dni, first_name, last_name, access_token, access_until, current_service_plan_name, email, birth_date, phone_number, gender, debt_amount, deleted_at, created_at, created_by, last_modified_at, last_modified_by, version`
- Indexes: `idx_contacts_tenant` (V1), `idx_contacts_access_token` (V1), `uq_tenant_contacts_dni UNIQUE(tenant_id, dni) WHERE deleted_at IS NULL` (V3)

## orders

### orders
- Entity: `OrderEntity` (`ImmutableBaseEntity` — append-only ledger)
- Soft delete: none (ledger)
- Version: yes (V2)
- Created: `V1__initial_schema.sql` | Last changed: `V2__add_version_columns.sql`
- Columns: `id, tenant_id, contact_id, location_id, payment_method, total_amount, is_voided, paid_at, created_at, created_by, last_modified_at, last_modified_by, version`
- Indexes: `idx_orders_tenant` (V1), `idx_orders_tenant_location_paid` (V1)

### order_items
- Entity: `OrderItemEntity` (`ImmutableBaseEntity` — append-only ledger)
- Soft delete: none (ledger)
- Version: yes (V2)
- Created: `V1__initial_schema.sql` | Last changed: `V5__add_audit_columns.sql`
- Columns: `id, tenant_id, order_id, item_type, service_plan_id, product_id, name, quantity, unit_price, total_price, created_at, created_by, last_modified_by`
- Indexes: `idx_order_items_sale` (V1)

### invoices
- Entity: `InvoiceEntity` (`ImmutableBaseEntity` — append-only ledger)
- Soft delete: none (ledger)
- Version: yes (V2)
- Created: `V1__initial_schema.sql` | Last changed: `V5__add_audit_columns.sql`
- Columns: `id, tenant_id, order_id, invoice_number, issue_date, due_date, total_amount, status, created_at, created_by, last_modified_by`
- Indexes: `idx_invoices_order` (V1)

## staffing

### staff
- Entity: `StaffEntity` (`TenantSoftDeletableEntity`)
- Soft delete: `deleted_at TIMESTAMPTZ` (V3)
- Version: yes (V1)
- Created: `V1__initial_schema.sql` | Last changed: `V3__migrate_to_deleted_at.sql`
- Columns: `id, tenant_id, first_name, last_name, dni, deleted_at, created_at, created_by, last_modified_at, last_modified_by, version`
- Indexes: `idx_staff_tenant` (V1)

### shifts
- Entity: `ShiftEntity` (`ImmutableBaseEntity` — append-only ledger)
- Soft delete: none (ledger)
- Version: yes (V1)
- Created: `V1__initial_schema.sql` | Last changed: `V1__initial_schema.sql`
- Columns: `id, tenant_id, staff_id, location_id, clock_in_at, clock_out_at, auto_closed, created_at, created_by, last_modified_at, last_modified_by, version`
- Indexes: `idx_shifts_one_active_per_staff UNIQUE(tenant_id, staff_id) WHERE clock_out_at IS NULL` (V1), `idx_shifts_tenant_lookup` (V1), `idx_shifts_tenant_location` (V1)

## tenant

### tenants
- Entity: `Tenant` (`TenantTogglableEntity` — legacy `is_active`)
- Soft delete: `is_active BOOLEAN NOT NULL DEFAULT TRUE` (legacy)
- Version: yes (V2)
- Created: `V1__initial_schema.sql` | Last changed: `V2__add_version_columns.sql`
- Columns: `id, name, tax_id, settings, slug, location, is_active, created_at, created_by, last_modified_at, last_modified_by, version`
- Indexes: `idx_tenants_settings` (V1, GIN)

### subscription_plans
- Entity: `SubscriptionPlanEntity` (`TenantTogglableEntity` — legacy `is_active`)
- Soft delete: `is_active BOOLEAN NOT NULL DEFAULT TRUE` (legacy)
- Version: yes (V1)
- Created: `V1__initial_schema.sql` | Last changed: `V1__initial_schema.sql`
- Columns: `id, tier, price, max_members, is_active, created_at, created_by, last_modified_at, last_modified_by, version`

### billing_cycles
- Entity: `BillingCycleEntity` (`TenantMutableEntity` — no soft delete)
- Soft delete: none
- Version: yes (V1)
- Created: `V1__initial_schema.sql` | Last changed: `V4__fix_billing_cycles_timestamps.sql`
- Columns: `id, tenant_id, plan_id, period_start, period_end, peak_members, discount_percentage, amount, status, is_demo, is_active, created_at, created_by, last_modified_at, last_modified_by, version, next_plan_id`
- Indexes: `idx_billing_cycles_tenant_id` (V1), `idx_billing_cycles_plan_id` (V1), `idx_billing_cycles_period` (V1), `idx_billing_cycles_status` (V1)

### tenant_invoices
- Entity: `TenantInvoiceEntity` (if exists) or `TenantInvoice` (if exists)
- Soft delete: none
- Version: yes (V2)
- Created: `V1__initial_schema.sql` | Last changed: `V5__add_audit_columns.sql`
- Columns: `id, tenant_id, amount_paid_ars, members_amount_snap, period_start, period_end, paid_at, created_at, created_by, last_modified_by`
- Indexes: `idx_tenant_invoices_tenant_id` (V1)

## registration

### email_verification_tokens
- Entity: `EmailVerificationTokenEntity` (`ImmutableBaseEntity` — system, no tenant)
- Soft delete: none
- Version: yes (V2)
- Created: `V1__initial_schema.sql` | Last changed: `V5__add_audit_columns.sql`
- Columns: `token, tenant_id, email, expires_at, created_by, last_modified_by, version`
- Indexes: `idx_email_verification_tokens_expires` (V1)

### tenant_registrations
- Entity: `TenantRegistrationEntity` (if exists)
- Soft delete: `is_active BOOLEAN NOT NULL DEFAULT TRUE` (legacy)
- Version: yes (V1)
- Created: `V1__initial_schema.sql` | Last changed: `V1__initial_schema.sql`
- Columns: `id, email, plan_id, token_hash, token_expires_at, completed_at, is_active, created_at, created_by, last_modified_at, last_modified_by, version`
- Indexes: `uq_tenant_registrations_pending_email` (V1), `idx_tenant_registrations_token_hash` (V1)

## notifications

### notifications
- Entity: `NotificationEntity` (if exists)
- Soft delete: none
- Version: no (out of scope)
- Created: `V1__initial_schema.sql` | Last changed: `V1__initial_schema.sql`
- Columns: `id, tenant_id, target_topic, message, created_at, created_by`
- Indexes: `idx_notifications_tenant` (V1), `idx_notifications_topics` (V1)

## Views

### v_contact_details
- Purpose: contact list with full name + last scanned at
- Joins: `contacts c`
- Filter: none (soft-deleted rows filtered by `@SQLRestriction` on entity, not view)
- Java consumer: `ContactDetailsViewRepository` (JPA, `@Immutable` view entity)
- Created: `V1__initial_schema.sql` | Last changed: `V1__initial_schema.sql`
- Columns: `id, tenant_id, dni, first_name, last_name, full_name, current_service_plan_name, access_until, access_token, is_active, created_at, email, birth_date, phone_number, gender, debt_amount, last_scanned_at`

### v_access_log_details
- Purpose: access log list with contact display name + location name
- Joins: `access_logs al JOIN contacts c ON al.contact_id = c.id AND al.tenant_id = c.tenant_id LEFT JOIN locations l ON al.location_id = l.id`
- Filter: none
- Java consumer: `AccessLogDetailsViewRepository` (JPA, `@Immutable` view entity) and `AccessLogDetailsRepository` (JdbcTemplate, two-stage reads)
- Created: `V1__initial_schema.sql` | Last changed: `V1__initial_schema.sql`
- Columns: `id, tenant_id, contact_id, dni, first_name, last_name, current_service_plan_name, access_until, scanned_at, status, contact_debt_amount, location_id, location_name`

### v_shift_details
- Purpose: shift list with staff display name + location name
- Joins: `shifts s JOIN staff st ON s.staff_id = st.id LEFT JOIN locations l ON s.location_id = l.id`
- Filter: none
- Java consumer: `ShiftDetailsViewRepository` (JPA, `@Immutable` view entity)
- Created: `V1__initial_schema.sql` | Last changed: `V1__initial_schema.sql`
- Columns: `id, tenant_id, staff_id, first_name, last_name, clock_in_at, clock_out_at, auto_closed, is_active, location_id, location_name`

### v_order_details
- Purpose: order list with contact display name + location name + aggregated items
- Joins: `orders o LEFT JOIN contacts c ON o.contact_id = c.id LEFT JOIN locations l ON o.location_id = l.id LEFT JOIN order_items oi ON o.id = oi.order_id`
- Filter: none
- Java consumer: `OrderDetailsViewRepository` (JPA, `@Immutable` view entity)
- Created: `V1__initial_schema.sql` | Last changed: `V1__initial_schema.sql`
- Columns: `id, tenant_id, contact_id, location_id, payment_method, total_amount, paid_at, is_voided, dni, contact_first_name, contact_last_name, location_name, items`

## Legacy / target conventions

| Convention | Applies to | Reference |
|---|---|---|
| Soft delete via `deleted_at TIMESTAMPTZ` | contacts, products, locations, staff, catalog_templates | `docs/migrations.md` §Soft delete |
| Soft delete via `is_active BOOLEAN` (legacy) | tenants, users, subscription_plans, tenant_registrations | `docs/migrations.md` §Soft delete |
| Append-only, no soft-delete flag | access_logs, orders, order_items, invoices, shifts | `docs/migrations.md` §Soft delete |
| Optimistic locking (`version`) | All catalogued tables | `docs/migrations.md` §Optimistic locking |

Out of documentation scope: `notifications` (no version column). Real-estate tables and views removed in V46 (not applicable to this blueprint).
