-- ============================================================
-- Spring Boot Production Blueprint — V2
-- Add version columns to tables missing optimistic locking
-- ============================================================

ALTER TABLE tenants ADD COLUMN version INTEGER NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN version INTEGER NOT NULL DEFAULT 0;
ALTER TABLE contacts ADD COLUMN version INTEGER NOT NULL DEFAULT 0;
ALTER TABLE products ADD COLUMN version INTEGER NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN version INTEGER NOT NULL DEFAULT 0;
ALTER TABLE order_items ADD COLUMN version INTEGER NOT NULL DEFAULT 0;
ALTER TABLE invoices ADD COLUMN version INTEGER NOT NULL DEFAULT 0;
ALTER TABLE access_logs ADD COLUMN version INTEGER NOT NULL DEFAULT 0;
ALTER TABLE tenant_invoices ADD COLUMN version INTEGER NOT NULL DEFAULT 0;
ALTER TABLE notifications ADD COLUMN version INTEGER NOT NULL DEFAULT 0;
ALTER TABLE email_verification_tokens ADD COLUMN version INTEGER NOT NULL DEFAULT 0;
ALTER TABLE tenant_registrations ADD COLUMN version INTEGER NOT NULL DEFAULT 0;

-- Tables already with version in V1: locations, staff, shifts, catalog_templates, billing_cycles, subscription_plans