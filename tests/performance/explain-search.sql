-- EXPLAIN ANALYZE for the SQL Hibernate generates for GET /api/v1/equipment-requests (TASK-005).
-- Shapes captured from spring.jpa.show-sql; parameters inlined. Run after seed-search-dataset.sql:
-- docker compose exec -T postgres psql -U equipment_app -d equipment_requests < tests/performance/explain-search.sql
\echo '== Q1 Employee default page'
EXPLAIN (ANALYZE, BUFFERS, COSTS OFF) SELECT * FROM equipment_requests e
WHERE e.owner_id = 'seed-employee-03' ORDER BY e.created_at DESC, e.id DESC OFFSET 0 ROWS FETCH FIRST 10 ROWS ONLY;
EXPLAIN (ANALYZE, COSTS OFF) SELECT count(e.id) FROM equipment_requests e WHERE e.owner_id = 'seed-employee-03';

\echo '== Q2 Approver status filter'
EXPLAIN (ANALYZE, BUFFERS, COSTS OFF) SELECT * FROM equipment_requests e
WHERE e.status = 'PENDING' ORDER BY e.created_at DESC, e.id DESC OFFSET 0 ROWS FETCH FIRST 10 ROWS ONLY;

\echo '== Q3 Approver keyword'
EXPLAIN (ANALYZE, BUFFERS, COSTS OFF) SELECT * FROM equipment_requests e
WHERE (lower(e.request_number) LIKE '%monitor%' ESCAPE '\' OR lower(e.title) LIKE '%monitor%' ESCAPE '\'
       OR lower(e.employee_name) LIKE '%monitor%' ESCAPE '\')
ORDER BY e.created_at DESC, e.id DESC OFFSET 0 ROWS FETCH FIRST 10 ROWS ONLY;
EXPLAIN (ANALYZE, COSTS OFF) SELECT count(e.id) FROM equipment_requests e
WHERE (lower(e.request_number) LIKE '%monitor%' ESCAPE '\' OR lower(e.title) LIKE '%monitor%' ESCAPE '\'
       OR lower(e.employee_name) LIKE '%monitor%' ESCAPE '\');

\echo '== Q4 keyword + status + department'
EXPLAIN (ANALYZE, BUFFERS, COSTS OFF) SELECT * FROM equipment_requests e
WHERE (lower(e.request_number) LIKE '%monitor%' ESCAPE '\' OR lower(e.title) LIKE '%monitor%' ESCAPE '\'
       OR lower(e.employee_name) LIKE '%monitor%' ESCAPE '\')
  AND e.status = 'PENDING' AND lower(e.department) = 'software engineering'
ORDER BY e.created_at DESC, e.id DESC OFFSET 0 ROWS FETCH FIRST 10 ROWS ONLY;

\echo '== Q5 department only'
EXPLAIN (ANALYZE, BUFFERS, COSTS OFF) SELECT * FROM equipment_requests e
WHERE lower(e.department) = 'finance' ORDER BY e.created_at DESC, e.id DESC OFFSET 0 ROWS FETCH FIRST 10 ROWS ONLY;

\echo '== Q6 page totals'
EXPLAIN (ANALYZE, BUFFERS, COSTS OFF) SELECT i.request_id, sum(i.quantity) FROM equipment_request_items i
WHERE i.request_id IN (SELECT e.id FROM equipment_requests e WHERE e.owner_id = 'seed-employee-03'
                       ORDER BY e.created_at DESC, e.id DESC LIMIT 10)
GROUP BY i.request_id;
