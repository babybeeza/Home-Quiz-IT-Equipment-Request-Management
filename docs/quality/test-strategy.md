# Test strategy

| Layer | Required coverage | Location |
| --- | --- | --- |
| Frontend | ≥4 behavior cases: required fields, dynamic items, duplicate submit/loading, server errors; เพิ่ม conflict/search/filter ตาม slice | frontend/src |
| Backend | ≥6 cases: create draft, submit empty fails, DRAFT→PENDING, invalid approve, reject without reason, edit non-draft; เพิ่ม 404/409/search/rollback | backend/src/test |
| Contract | request/response, pagination metadata, error envelope และ mutation versions ตรงกัน | tests/contract |
| Integration | PostgreSQL constraints/transactions และ concurrent stale writes; Redis invalidation/Caffeine behavior | backend/src/test |
| E2E | employee draft→submit, approver approve/reject, unauthorized action และ conflict UX | tests/e2e |
| Performance | k6 search/detail/mutation mix; baseline เทียบ cache warm/cold พร้อม correctness checks | tests/performance |

ใช้ fake clock สำหรับ requiredDate; fixtures ไม่ใส่ข้อมูลบุคคลจริง; แยก unit tests จาก tests ที่ต้องมี services ระบุ integration/E2E ที่เลือกเพิ่มให้ชัด ไม่อ้างว่าเป็น minimum ของโจทย์ทั้งหมด

ทุก requirement ต้องตามกลับไปยัง test และ evidence ได้ ดู [traceability](../product/traceability.md) ผล AI review เป็นข้อมูลประกอบ ไม่แทนการ execute tests
