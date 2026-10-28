-- ============================================================
-- Spring Boot Production Blueprint — V5
-- Add missing audit columns (created_by, last_modified_by)
-- ============================================================

-- Tables missing last_modified_by only
ALTER TABLE access_logs ADD COLUMN last_modified_by UUID;
ALTER TABLE order_items ADD COLUMN last_modified_by UUID;
ALTER TABLE invoices ADD COLUMN last_modified_by UUID;
ALTER TABLE tenant_invoices ADD COLUMN last_modified_by UUID;

-- Tables missing both created_by and last_modified_by
ALTER TABLE notifications ADD COLUMN created_by UUID;
ALTER TABLE notifications ADD COLUMN last_modified_by UUID;
ALTER TABLE email_verification_tokens ADD COLUMN created_by UUID;
ALTER TABLE email_verification_tokens ADD COLUMN last_modified_by UUID;