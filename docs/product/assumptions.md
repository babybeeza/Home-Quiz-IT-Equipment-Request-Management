# Assumptions and open decisions

ทุกแถวเป็นข้อเสนอที่ยังต้องบันทึกข้อสรุป ไม่ใช่ requirement ที่ได้รับการยืนยันแล้ว

ดู [proposed defaults ในแผน implementation](../delivery/implementation-plan.md) สำหรับรายละเอียดที่ใช้วางแผน ณ 2026-09-25; ยังไม่ถือเป็น accepted ADR หรือพฤติกรรมที่ implement แล้ว

| ID | ประเด็น | แนวทางเสนอ | Resolve in |
| --- | --- | --- | --- |
| A-01 | Identity และ ownership เมื่อจำลอง role | Resolved by Requirements approval: `X-User-Id` + `X-Role`, owner ID แยกจาก email | [Discover Q-01](discovery.md) |
| A-02 | Validation ของ draft ที่ยังไม่ครบ | Resolved: scalar fields valid; items ว่างได้จน submit | [Discover Q-02](discovery.md) |
| A-03 | วันที่และ timezone | Resolved: Asia/Bangkok และ revalidate create/update/submit | [Discover Q-03](discovery.md) |
| A-04 | Version ของ action endpoints | Resolved: ทุก mutation ส่ง expectedVersion | [Discover Q-04](discovery.md) |
| A-05 | totalItems หมายถึงอะไร | Resolved: ผลรวม quantity | [Discover Q-05](discovery.md) |
| A-06 | Performance target | Resolved at requirement level: ต้อง finalize workload/thresholds ก่อนรัน; ค่าจริงกำหนดใน TASK-007 | [Discover Q-12](discovery.md) |
| A-07 | Exact runtime/library versions | Resolved 2026-09-25 ตาม [ADR-001](../architecture/decisions/ADR-001-toolchains.md) | TASK-001 |

เมื่อ resolve ให้เพิ่ม decision, เหตุผล, วันที่ และลิงก์ ADR/task โดยไม่ลบประวัติข้อเสนอเดิม
