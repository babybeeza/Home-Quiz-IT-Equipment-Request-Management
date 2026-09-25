# Delivery and operations runbook

Status: template — ยังไม่ได้ deploy หรือทดสอบ rollback

## ก่อนส่งมอบ
- [ ] ระบุ release owner, revision, environment และ runtime versions
- [ ] README install/run/test ใช้ได้จาก clean checkout
- [ ] required checks มี evidence; API/schema และ limitations ตรงกับ code
- [ ] ระบุ config variables โดยไม่เก็บ secret values
- [ ] ทดสอบ migration บน test database และบันทึก backup/restore approach
- [ ] ระบุ smoke test: create draft → submit → decision → list

## Release record
- Revision / artifact: TBD
- Target / owner: TBD
- Exact deployment steps: TBD เมื่อเลือก target
- Health and smoke checks: TBD
- Monitoring: error rate, latency, DB conflicts, cache hit/miss โดยไม่ log PII

## Rollback
ระบุ previous artifact, rollback trigger, ผู้รับผิดชอบ และคำสั่งจริงก่อน release ถ้า schema change ย้อนกลับไม่ได้ ให้ระบุ restore/forward-fix plan พร้อมหลักฐาน rehearsal ห้ามสมมติว่า downgrade application แล้ว database จะเข้ากันเสมอ

## Feedback
บันทึก incident/defect → requirement → regression test → backlog task อัปเดต prompt หรือ context เมื่อพบข้อผิดพลาดซ้ำ
