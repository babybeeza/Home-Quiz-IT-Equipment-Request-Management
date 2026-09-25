# Assumptions and open decisions

ทุกแถวเป็นข้อเสนอที่ยังต้องบันทึกข้อสรุป ไม่ใช่ requirement ที่ได้รับการยืนยันแล้ว

ดู [proposed defaults ในแผน implementation](../delivery/implementation-plan.md) สำหรับรายละเอียดที่ใช้วางแผน ณ 2026-09-25; ยังไม่ถือเป็น accepted ADR หรือพฤติกรรมที่ implement แล้ว

| ID | ประเด็น | แนวทางเสนอ | Resolve in |
| --- | --- | --- | --- |
| A-01 | Identity และ ownership เมื่อจำลอง role | ใช้ stable user ID แยกจาก employee email; ระบุ mock headers และการตรวจสิทธิ์ | TASK-002 |
| A-02 | Validation ของ draft ที่ยังไม่ครบ | draft ไม่มี item ได้; สรุป field validation ที่บังคับตอน save เทียบ submit | TASK-002 |
| A-03 | วันที่และ timezone | ใช้ business timezone ที่กำหนดชัดและ injectable clock สำหรับ tests | TASK-002 |
| A-04 | Version ของ action endpoints | ทุก mutation หลัง create ส่ง expected version; state/version conflict เป็น 409 | TASK-002 |
| A-05 | totalItems หมายถึงอะไร | เลือกจำนวนบรรทัดหรือผลรวม quantity แล้วใช้ตรงกันทุกชั้น | TASK-002 |
| A-06 | Performance target | กำหนด dataset, VUs, duration, latency/error thresholds ก่อนวัด | TASK-007 |
| A-07 | Exact runtime/library versions | Resolved 2026-09-25 ตาม [ADR-001](../architecture/decisions/ADR-001-toolchains.md) | TASK-001 |

เมื่อ resolve ให้เพิ่ม decision, เหตุผล, วันที่ และลิงก์ ADR/task โดยไม่ลบประวัติข้อเสนอเดิม
