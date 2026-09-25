CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE SEQUENCE equipment_request_number_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE equipment_requests (
    id UUID PRIMARY KEY,
    request_number VARCHAR(32) NOT NULL UNIQUE,
    owner_id VARCHAR(100) NOT NULL,
    employee_name VARCHAR(100) NOT NULL,
    employee_email VARCHAR(254) NOT NULL,
    department VARCHAR(100) NOT NULL,
    title VARCHAR(150) NOT NULL,
    purpose VARCHAR(500) NOT NULL,
    required_date DATE NOT NULL,
    additional_note VARCHAR(500),
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    rejection_reason VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_equipment_requests_number CHECK (request_number ~ '^REQ-[0-9]{4}-[0-9]{6,}$'),
    CONSTRAINT ck_equipment_requests_owner CHECK (length(btrim(owner_id)) BETWEEN 1 AND 100),
    CONSTRAINT ck_equipment_requests_name CHECK (length(btrim(employee_name)) BETWEEN 2 AND 100),
    CONSTRAINT ck_equipment_requests_department CHECK (length(btrim(department)) BETWEEN 1 AND 100),
    CONSTRAINT ck_equipment_requests_title CHECK (length(btrim(title)) BETWEEN 5 AND 150),
    CONSTRAINT ck_equipment_requests_purpose CHECK (length(btrim(purpose)) BETWEEN 10 AND 500),
    CONSTRAINT ck_equipment_requests_status CHECK (status IN ('DRAFT', 'PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT ck_equipment_requests_version CHECK (version >= 0),
    CONSTRAINT ck_equipment_requests_rejection CHECK (
        (status = 'REJECTED' AND rejection_reason IS NOT NULL AND length(btrim(rejection_reason)) BETWEEN 1 AND 500)
        OR (status <> 'REJECTED' AND rejection_reason IS NULL)
    )
);

CREATE TABLE equipment_request_items (
    id UUID PRIMARY KEY,
    request_id UUID NOT NULL,
    equipment_type VARCHAR(16) NOT NULL,
    quantity SMALLINT NOT NULL,
    specification VARCHAR(250),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_equipment_request_items_request
        FOREIGN KEY (request_id) REFERENCES equipment_requests(id) ON DELETE CASCADE,
    CONSTRAINT ck_equipment_request_items_type
        CHECK (equipment_type IN ('NOTEBOOK', 'MONITOR', 'KEYBOARD', 'MOUSE', 'HEADSET', 'OTHER')),
    CONSTRAINT ck_equipment_request_items_quantity CHECK (quantity BETWEEN 1 AND 5)
);

CREATE INDEX idx_equipment_requests_owner_created
    ON equipment_requests (owner_id, created_at DESC, id DESC);
CREATE INDEX idx_equipment_requests_status_created
    ON equipment_requests (status, created_at DESC, id DESC);
CREATE INDEX idx_equipment_requests_department_created
    ON equipment_requests (department, created_at DESC, id DESC);
CREATE INDEX idx_equipment_request_items_request
    ON equipment_request_items (request_id);
CREATE INDEX idx_equipment_requests_number_trgm
    ON equipment_requests USING GIN (lower(request_number) gin_trgm_ops);
CREATE INDEX idx_equipment_requests_title_trgm
    ON equipment_requests USING GIN (lower(title) gin_trgm_ops);
CREATE INDEX idx_equipment_requests_employee_name_trgm
    ON equipment_requests USING GIN (lower(employee_name) gin_trgm_ops);
