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
| A-06 | Performance target | Resolved 2026-09-25 before any run: p95 < 500 ms, p99 < 1 s, unexpected errors < 1%, checks 100%; workload and runs in [ADR-007](../architecture/decisions/ADR-007-performance-and-delivery.md) | [Discover Q-12](discovery.md), TASK-007 |
| A-07 | Exact runtime/library versions | Resolved 2026-09-25 ตาม [ADR-001](../architecture/decisions/ADR-001-toolchains.md) | TASK-001 |
| A-08 | Git repository handoff location/access และ immutable revision สำหรับ REQ-13 | Resolved 2026-09-26: release owner accepted GitHub `main` revision `77b56241a9eef817c62e34d4e949bfc6ecfdc9f7`; an independent HTTPS clone without stored credentials succeeded. The decision and limitations are in [approvals](../governance/approvals.md) and [evidence](../quality/evidence/TASK-013.md) | [REQ-13 Discover](req-13-discovery.md), [TASK-013](../delivery/tasks/TASK-013-req13-release-handoff.md) |
| A-09 | แหล่งสีและขอบเขตธีม Spark Deck สำหรับ REQ-14 | Palette resolved 2026-09-26 จากตาราง hex 12 สีที่ผู้ใช้ให้โดยตรง; scope จากคำขอคือสี UI เท่านั้น. [ADR-008](../architecture/decisions/ADR-008-spark-theme.md) เสนอ semantic mapping และคู่สีที่ตรวจ contrast แล้วบน isolated implementation branch; ยังไม่ได้ตรวจไฟล์ deck จริงหรืออนุมัติ Design | [REQ-14 Discover](req-14-theme-discovery.md), [TASK-014](../delivery/tasks/TASK-014-spark-theme-design.md) |

เมื่อ resolve ให้เพิ่ม decision, เหตุผล, วันที่ และลิงก์ ADR/task โดยไม่ลบประวัติข้อเสนอเดิม
