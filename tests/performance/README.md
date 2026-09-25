# k6 performance testing

ก่อนสร้าง script ให้กำหนด target environment, dataset size, role/user mix, search/detail/mutation proportions, VUs, ramp-up, duration และ thresholds ใน TASK-007

ตรวจ status/body correctness ควบคู่ latency; mutation ต้องใช้ ID/version ของ test data ที่เตรียมไว้ อย่ายิง load test ไปยัง production โดยไม่มีขอบเขตที่ได้รับอนุญาต

เก็บ scripts ในโฟลเดอร์นี้ และรายงานที่สรุปแล้วใน `results/` ใช้ [report template](results/TEMPLATE.md) แยก cold/warm cache และบันทึก dataset/setup/cleanup เพื่อให้รันซ้ำได้

## Search baseline dataset (TASK-005)

- [`seed-search-dataset.sql`](seed-search-dataset.sql) inserts 1,200 requests from `seed-%` owners, spread over 6 departments and 5 statuses, with 0–3 items each. It is idempotent: rerunning it first deletes only `seed-%` rows.
- [`explain-search.sql`](explain-search.sql) runs `EXPLAIN ANALYZE` on the SQL that Hibernate generates for the list endpoint. The recorded output is in [`results/TASK-005-explain.txt`](results/TASK-005-explain.txt).

```powershell
docker compose exec -T postgres psql -U equipment_app -d equipment_requests < tests/performance/seed-search-dataset.sql
docker compose exec -T postgres psql -U equipment_app -d equipment_requests < tests/performance/explain-search.sql
```
