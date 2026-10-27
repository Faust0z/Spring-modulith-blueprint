-- ============================================================
-- Spring Boot Production Blueprint — Initial Schema (V1)
-- ============================================================

CREATE TABLE tenants
(
    id               UUID PRIMARY KEY,
    name             VARCHAR(255) NOT NULL,
    tax_id           VARCHAR(50),
    settings         JSONB        NOT NULL DEFAULT '{}'::jsonb,
    slug             VARCHAR(30)  NOT NULL UNIQUE,
    location         VARCHAR(100),
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by       UUID,
    last_modified_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_modified_by UUID
);

CREATE TABLE users
(
    id               UUID PRIMARY KEY,
    full_name        VARCHAR(255) NOT NULL DEFAULT '',
    email            VARCHAR(255),
    username         VARCHAR(50)  UNIQUE,
    password         VARCHAR(255),
    dni              VARCHAR(20),
    role             VARCHAR(50)  NOT NULL,
    tenant_id        UUID REFERENCES tenants (id) ON DELETE RESTRICT,
    terms_accepted_at TIMESTAMPTZ,
    admin_pin        VARCHAR(255),
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by       UUID,
    last_modified_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_modified_by UUID
);

CREATE INDEX idx_users_tenant_id ON users (tenant_id);
CREATE INDEX idx_tenants_settings ON tenants USING GIN (settings);

CREATE TABLE locations
(
    id               UUID PRIMARY KEY,
    tenant_id        UUID                      NOT NULL,
    name             VARCHAR(50)               NOT NULL,
    is_active        BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    created_by       UUID,
    last_modified_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    last_modified_by UUID,
    version          INT         DEFAULT 0,
    CONSTRAINT uq_tenant_location_name UNIQUE (tenant_id, name)
);
CREATE INDEX idx_locations_tenant ON locations (tenant_id);

CREATE TABLE service_plans
(
    id               UUID PRIMARY KEY,
    tenant_id        UUID           NOT NULL,
    name             VARCHAR(100)   NOT NULL,
    duration_days    INTEGER        NOT NULL,
    price            NUMERIC(12, 2) NOT NULL,
    is_active        BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    created_by       UUID,
    last_modified_at TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    last_modified_by UUID
);
CREATE INDEX idx_service_plans_tenant ON service_plans (tenant_id);

CREATE TABLE contacts
(
    id                    UUID PRIMARY KEY,
    tenant_id             UUID         NOT NULL,
    dni                   VARCHAR(20)  NOT NULL,
    first_name            VARCHAR(100) NOT NULL,
    last_name             VARCHAR(100) NOT NULL,
    access_token          UUID         NOT NULL UNIQUE,
    access_until          TIMESTAMPTZ,
    current_service_plan_name VARCHAR(255),
    email                 VARCHAR(255),
    birth_date            DATE,
    phone_number          VARCHAR(50),
    gender                VARCHAR(50),
    debt_amount           INTEGER      NOT NULL DEFAULT 0,
    is_active             BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by            UUID,
    last_modified_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_modified_by      UUID,
    CONSTRAINT uk_contacts_tenant_dni UNIQUE (tenant_id, dni)
);
CREATE INDEX idx_contacts_tenant ON contacts (tenant_id);
CREATE INDEX idx_contacts_access_token ON contacts (access_token);

CREATE TABLE products
(
    id               UUID PRIMARY KEY,
    tenant_id        UUID           NOT NULL,
    name             VARCHAR(100)   NOT NULL,
    price            NUMERIC(12, 2) NOT NULL,
    is_active        BOOLEAN DEFAULT TRUE NOT NULL,
    created_at       TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    created_by       UUID,
    last_modified_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    last_modified_by UUID
);
CREATE INDEX idx_products_tenant ON products (tenant_id);

CREATE TABLE staff
(
    id               UUID PRIMARY KEY,
    tenant_id        UUID         NOT NULL,
    first_name       VARCHAR(255) NOT NULL,
    last_name        VARCHAR(255) NOT NULL,
    dni              VARCHAR(255) NOT NULL,
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by       UUID,
    last_modified_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_modified_by UUID,
    version          INT          NOT NULL DEFAULT 0
);
CREATE INDEX idx_staff_tenant ON staff (tenant_id);

CREATE TABLE shifts
(
    id               UUID PRIMARY KEY,
    tenant_id        UUID         NOT NULL,
    staff_id         UUID         NOT NULL,
    location_id      UUID         NOT NULL,
    clock_in_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    clock_out_at     TIMESTAMPTZ,
    auto_closed      BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by       UUID,
    last_modified_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_modified_by UUID,
    version          INT          NOT NULL DEFAULT 0,
    CONSTRAINT fk_shifts_staff FOREIGN KEY (staff_id) REFERENCES staff (id),
    CONSTRAINT fk_shifts_location FOREIGN KEY (location_id) REFERENCES locations (id)
);

CREATE UNIQUE INDEX idx_shifts_one_active_per_staff
    ON shifts (tenant_id, staff_id)
    WHERE clock_out_at IS NULL AND is_active IS TRUE;

CREATE INDEX idx_shifts_tenant_lookup ON shifts (tenant_id, clock_in_at DESC);
CREATE INDEX idx_shifts_tenant_location ON shifts (tenant_id, location_id);

CREATE TABLE access_logs
(
    id         UUID PRIMARY KEY,
    tenant_id  UUID        NOT NULL,
    scanned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status     VARCHAR(50) NOT NULL,
    contact_id UUID        NOT NULL,
    location_id UUID       NOT NULL,
    reason     VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID
);
CREATE INDEX idx_access_logs_tenant ON access_logs (tenant_id);
CREATE INDEX idx_access_logs_contact ON access_logs (contact_id);
CREATE INDEX idx_access_logs_tenant_scanned ON access_logs (tenant_id, scanned_at);
CREATE INDEX idx_access_logs_tenant_location_scanned ON access_logs (tenant_id, location_id, scanned_at DESC);

CREATE TABLE orders
(
    id               UUID PRIMARY KEY,
    tenant_id        UUID           NOT NULL,
    contact_id       UUID           NOT NULL,
    location_id      UUID           NOT NULL,
    payment_method   VARCHAR(50)    NOT NULL,
    total_amount     NUMERIC(12, 2) NOT NULL,
    is_voided        BOOLEAN        NOT NULL DEFAULT FALSE,
    paid_at          TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    created_by       UUID,
    last_modified_at TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    last_modified_by UUID,
    CONSTRAINT fk_orders_contact FOREIGN KEY (contact_id) REFERENCES contacts (id),
    CONSTRAINT fk_orders_location FOREIGN KEY (location_id) REFERENCES locations (id)
);
CREATE INDEX idx_orders_tenant ON orders (tenant_id);
CREATE INDEX idx_orders_tenant_location_paid ON orders (tenant_id, location_id, paid_at DESC);

CREATE TABLE order_items
(
    id               UUID PRIMARY KEY,
    tenant_id        UUID           NOT NULL,
    order_id         UUID           NOT NULL,
    item_type        VARCHAR(50)    NOT NULL,
    service_plan_id  UUID,
    product_id       UUID,
    name             VARCHAR(255)   NOT NULL,
    quantity         INT            NOT NULL,
    unit_price       NUMERIC(12, 2) NOT NULL,
    total_price      NUMERIC(12, 2) NOT NULL,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    created_by       UUID,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE
);
CREATE INDEX idx_order_items_sale ON order_items (order_id);

CREATE TABLE catalog_templates
(
    id               UUID PRIMARY KEY,
    tenant_id        UUID                      NOT NULL,
    name             VARCHAR(100)              NOT NULL,
    content          TEXT                      NOT NULL,
    category         VARCHAR(50),
    is_active        BOOLEAN                   NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    created_by       UUID,
    last_modified_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    last_modified_by UUID,
    version          INT         DEFAULT 0
);
CREATE INDEX idx_catalog_templates_tenant ON catalog_templates (tenant_id);

CREATE TABLE invoices
(
    id               UUID PRIMARY KEY,
    tenant_id        UUID           NOT NULL,
    order_id         UUID           NOT NULL,
    invoice_number   VARCHAR(50)    NOT NULL UNIQUE,
    issue_date       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    due_date         TIMESTAMPTZ,
    total_amount     NUMERIC(12, 2) NOT NULL,
    status           VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    created_by       UUID,
    CONSTRAINT fk_invoices_order FOREIGN KEY (order_id) REFERENCES orders (id)
);
CREATE INDEX idx_invoices_order ON invoices (order_id);

CREATE TABLE subscription_plans
(
    id               UUID PRIMARY KEY,
    tier             VARCHAR(50)   NOT NULL,
    price            INTEGER       NOT NULL,
    max_members      INTEGER,
    version          INTEGER       NOT NULL DEFAULT 0,
    is_active        BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by       UUID,
    last_modified_at TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    last_modified_by UUID
);

CREATE TABLE billing_cycles
(
    id                    UUID PRIMARY KEY,
    tenant_id             UUID         NOT NULL,
    plan_id               UUID         NOT NULL REFERENCES subscription_plans (id),
    period_start          TIMESTAMP    NOT NULL,
    period_end            TIMESTAMP    NOT NULL,
    peak_members          INTEGER      NOT NULL DEFAULT 0,
    discount_percentage   INTEGER      NOT NULL DEFAULT 0,
    amount                INTEGER,
    status                VARCHAR(20)  NOT NULL,
    is_demo               BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active             BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by            UUID,
    last_modified_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_modified_by      UUID,
    version               INTEGER      NOT NULL DEFAULT 0,
    next_plan_id          UUID         NULL REFERENCES subscription_plans (id)
);

CREATE INDEX idx_billing_cycles_tenant_id ON billing_cycles (tenant_id);
CREATE INDEX idx_billing_cycles_plan_id ON billing_cycles (plan_id);
CREATE INDEX idx_billing_cycles_period ON billing_cycles (period_start, period_end);
CREATE INDEX idx_billing_cycles_status ON billing_cycles (status);

CREATE TABLE tenant_invoices
(
    id                    UUID PRIMARY KEY,
    tenant_id             UUID         NOT NULL,
    amount_paid_ars       INT          NOT NULL,
    members_amount_snap   INT          NOT NULL DEFAULT 0,
    period_start          TIMESTAMPTZ  NOT NULL,
    period_end            TIMESTAMPTZ  NOT NULL,
    paid_at               TIMESTAMPTZ  NOT NULL,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by            UUID,
    CONSTRAINT fk_tenant_invoices_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
);
CREATE INDEX idx_tenant_invoices_tenant_id ON tenant_invoices (tenant_id);

CREATE TABLE notifications
(
    id           UUID PRIMARY KEY,
    tenant_id    UUID                      NOT NULL,
    target_topic VARCHAR(255)              NOT NULL,
    message      TEXT                      NOT NULL,
    created_at   TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    created_by   UUID
);
CREATE INDEX idx_notifications_tenant ON notifications (tenant_id);
CREATE INDEX idx_notifications_topics ON notifications (target_topic, created_at DESC);

CREATE TABLE email_verification_tokens
(
    token      VARCHAR(64) PRIMARY KEY,
    tenant_id  UUID         NOT NULL,
    email      VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_email_verification_tokens_expires ON email_verification_tokens (expires_at);

CREATE TABLE tenant_registrations
(
    id               UUID PRIMARY KEY,
    email            VARCHAR(255)            NOT NULL,
    plan_id          UUID REFERENCES subscription_plans (id),
    token_hash       VARCHAR(64)             NOT NULL,
    token_expires_at TIMESTAMPTZ             NOT NULL,
    completed_at     TIMESTAMPTZ,
    is_active        BOOLEAN                 NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ             NOT NULL DEFAULT NOW(),
    created_by       UUID,
    last_modified_at TIMESTAMPTZ             NOT NULL DEFAULT NOW(),
    last_modified_by UUID
);

CREATE UNIQUE INDEX uq_tenant_registrations_pending_email ON tenant_registrations (email) WHERE completed_at IS NULL;
CREATE INDEX idx_tenant_registrations_token_hash ON tenant_registrations (token_hash);

CREATE VIEW v_contact_details AS
SELECT c.id,
       c.tenant_id,
       c.dni,
       c.first_name,
       c.last_name,
       c.first_name || ' ' || c.last_name                                   AS full_name,
       c.current_service_plan_name,
       c.access_until,
       c.access_token,
       c.is_active,
       c.created_at,
       c.email,
       c.birth_date,
       c.phone_number,
       c.gender,
       c.debt_amount,
       (SELECT MAX(al.scanned_at)
        FROM access_logs al
        WHERE al.contact_id = c.id AND al.status = 'PERMITIDO')                AS last_scanned_at
FROM contacts c;

CREATE VIEW v_access_log_details AS
SELECT al.id,
       al.tenant_id,
       al.contact_id,
       c.dni,
       c.first_name,
       c.last_name,
       c.current_service_plan_name,
       c.access_until,
       al.scanned_at,
       al.status,
       c.debt_amount AS contact_debt_amount,
       al.location_id,
       l.name AS location_name
FROM access_logs al
JOIN contacts c ON al.contact_id = c.id AND al.tenant_id = c.tenant_id
LEFT JOIN locations l ON al.location_id = l.id;

CREATE VIEW v_shift_details AS
SELECT s.id,
       s.tenant_id,
       s.staff_id,
       st.first_name,
       st.last_name,
       s.clock_in_at,
       s.clock_out_at,
       s.auto_closed,
       s.is_active,
       s.location_id,
       l.name AS location_name
FROM shifts s
JOIN staff st ON s.staff_id = st.id
LEFT JOIN locations l ON s.location_id = l.id;

CREATE VIEW v_order_details AS
SELECT o.id,
       o.tenant_id,
       o.contact_id,
       o.location_id,
       o.payment_method,
       o.total_amount,
       o.paid_at,
       o.is_voided,
       c.dni,
       c.first_name AS contact_first_name,
       c.last_name  AS contact_last_name,
       l.name       AS location_name,
       COALESCE(JSONB_OBJECT_AGG(oi.name, oi.quantity) FILTER (WHERE oi.name IS NOT NULL), '{}'::jsonb) AS items
FROM orders o
LEFT JOIN contacts c ON o.contact_id = c.id
LEFT JOIN locations l ON o.location_id = l.id
LEFT JOIN order_items oi ON o.id = oi.order_id
GROUP BY o.id, c.id, l.id;

ALTER TABLE users
    ADD CONSTRAINT chk_user_credentials
        CHECK (role = 'TENANT_WORKER' OR (email IS NOT NULL AND password IS NOT NULL));

CREATE INDEX idx_orders_tenant_branch_paid ON orders (tenant_id, location_id, paid_at DESC);
