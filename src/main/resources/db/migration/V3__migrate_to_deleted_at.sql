-- ============================================================
-- Spring Boot Production Blueprint — V3
-- Migrate soft-delete-able tables from is_active to deleted_at
-- ============================================================

-- locations (branches): TenantSoftDeletableEntity
ALTER TABLE locations ADD COLUMN deleted_at TIMESTAMPTZ NULL;
UPDATE locations SET deleted_at = NULL WHERE is_active = TRUE;
ALTER TABLE locations DROP COLUMN is_active;
DROP INDEX uq_tenant_location_name;
CREATE UNIQUE INDEX uq_tenant_location_name ON locations(tenant_id, name) WHERE deleted_at IS NULL;

-- contacts (members): TenantSoftDeletableEntity
ALTER TABLE contacts ADD COLUMN deleted_at TIMESTAMPTZ NULL;
UPDATE contacts SET deleted_at = NULL WHERE is_active = TRUE;
ALTER TABLE contacts DROP COLUMN is_active;
DROP INDEX uk_contacts_tenant_dni;
CREATE UNIQUE INDEX uq_tenant_contacts_dni ON contacts(tenant_id, dni) WHERE deleted_at IS NULL;

-- staff: TenantSoftDeletableEntity
ALTER TABLE staff ADD COLUMN deleted_at TIMESTAMPTZ NULL;
UPDATE staff SET deleted_at = NULL WHERE is_active = TRUE;
ALTER TABLE staff DROP COLUMN is_active;

-- products: TenantSoftDeletableEntity
ALTER TABLE products ADD COLUMN deleted_at TIMESTAMPTZ NULL;
UPDATE products SET deleted_at = NULL WHERE is_active = TRUE;
ALTER TABLE products DROP COLUMN is_active;

-- catalog_templates: TenantSoftDeletableEntity
ALTER TABLE catalog_templates ADD COLUMN deleted_at TIMESTAMPTZ NULL;
UPDATE catalog_templates SET deleted_at = NULL WHERE is_active = TRUE;
ALTER TABLE catalog_templates DROP COLUMN is_active;