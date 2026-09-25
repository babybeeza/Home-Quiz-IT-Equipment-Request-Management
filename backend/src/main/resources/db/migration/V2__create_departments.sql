-- ADR-006: reference data served through the Caffeine-cached GET /api/v1/reference-data.
-- Departments are suggestions only; equipment_requests.department stays free text.
CREATE TABLE departments (
    code VARCHAR(32) PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    sort_order INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_departments_code CHECK (code ~ '^[A-Z][A-Z0-9_]{1,31}$'),
    CONSTRAINT ck_departments_name CHECK (length(btrim(name)) BETWEEN 1 AND 100)
);

INSERT INTO departments (code, name, sort_order) VALUES
    ('SOFTWARE_ENGINEERING', 'Software Engineering', 10),
    ('FINANCE', 'Finance', 20),
    ('OPERATIONS', 'Operations', 30),
    ('MARKETING', 'Marketing', 40),
    ('HUMAN_RESOURCES', 'Human Resources', 50),
    ('DESIGN', 'Design', 60);
