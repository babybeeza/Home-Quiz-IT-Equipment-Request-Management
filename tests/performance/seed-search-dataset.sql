-- Search/list baseline dataset (TASK-005, reused by TASK-006/007).
-- Idempotent: removes only rows owned by 'seed-%' users, then inserts 1,200 requests
-- across 10 owners, 6 departments and all 5 statuses, with 0-3 items each.
-- Run: docker compose exec -T postgres psql -U equipment_app -d equipment_requests < tests/performance/seed-search-dataset.sql

BEGIN;

DELETE FROM equipment_requests WHERE owner_id LIKE 'seed-%';

INSERT INTO equipment_requests (
    id, request_number, owner_id, employee_name, employee_email, department, title, purpose,
    required_date, status, rejection_reason, version, created_at, updated_at
)
SELECT
    md5('seed-request-' || n)::uuid,
    'REQ-2026-' || lpad((900000 + n)::text, 6, '0'),
    'seed-employee-' || lpad((n % 10 + 1)::text, 2, '0'),
    (ARRAY['Somchai', 'Suda', 'Kanokwan', 'Anan', 'Pim', 'Niran', 'Malee', 'Thanakorn', 'Wipa', 'Chai'])[n % 10 + 1]
        || ' Seed ' || (n % 37),
    'seed' || n || '@example.com',
    (ARRAY['Software Engineering', 'Finance', 'Operations', 'Marketing', 'Human Resources', 'Design'])[n % 6 + 1],
    (ARRAY['Notebook replacement', 'Monitor upgrade', 'Ergonomic keyboard', 'Wireless mouse', 'Headset for calls',
           'Docking station', 'จอภาพสำหรับงานออกแบบ'])[n % 7 + 1] || ' #' || n,
    'Seeded purpose for search baseline number ' || n,
    DATE '2099-01-01' + (n % 300),
    s.status,
    CASE WHEN s.status = 'REJECTED' THEN 'Seeded rejection reason' END,
    n % 4,
    TIMESTAMPTZ '2026-01-01 00:00:00+00' + (n * INTERVAL '97 minutes'),
    TIMESTAMPTZ '2026-01-01 00:00:00+00' + (n * INTERVAL '97 minutes')
FROM generate_series(1, 1200) AS n
CROSS JOIN LATERAL (
    SELECT (ARRAY['DRAFT', 'PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'])[n % 5 + 1] AS status
) AS s;

INSERT INTO equipment_request_items (id, request_id, equipment_type, quantity, specification, created_at, updated_at)
SELECT
    md5('seed-item-' || n || '-' || k)::uuid,
    md5('seed-request-' || n)::uuid,
    (ARRAY['NOTEBOOK', 'MONITOR', 'KEYBOARD', 'MOUSE', 'HEADSET', 'OTHER'])[(n + k) % 6 + 1],
    (n + k) % 5 + 1,
    NULL,
    TIMESTAMPTZ '2026-01-01 00:00:00+00' + (n * INTERVAL '97 minutes') + (k * INTERVAL '1 microsecond'),
    TIMESTAMPTZ '2026-01-01 00:00:00+00' + (n * INTERVAL '97 minutes') + (k * INTERVAL '1 microsecond')
FROM generate_series(1, 1200) AS n
CROSS JOIN LATERAL generate_series(1, n % 4) AS k;

COMMIT;

ANALYZE equipment_requests;
ANALYZE equipment_request_items;
