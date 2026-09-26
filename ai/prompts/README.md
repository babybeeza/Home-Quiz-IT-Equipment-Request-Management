# Reusable SDLC prompts

แทนค่าภายใน `<...>` ก่อนใช้ แนบเฉพาะ context ของ task และอย่าส่ง secrets

## Discover
อ่าน AGENTS.md และโจทย์ สกัด requirement IDs กับ acceptance criteria แยกข้อเท็จจริง/assumption/คำถาม อัปเดต docs/product โดยไม่เพิ่ม scope ที่โจทย์ไม่ได้กำหนด

## Design
สำหรับ `<requirement IDs>` อ่าน architecture และ decisions เสนอ contract, state/role matrix, schema, concurrency และ error paths บันทึก tradeoffs ใน ADR ระบุ decision ที่ยังเปิดอยู่

## Plan
แตก `<feature>` เป็น vertical slices สร้าง task packets จาก template แต่ละงานมี dependencies, acceptance criteria, failure paths และ verification ที่ execute ได้

## Implement
ทำ `<task path>` ตาม AGENTS.md อ่าน context ที่ลิงก์ไว้ก่อนแก้ไฟล์ implement ขอบเขตงานพร้อม tests ที่เหมาะสม รัน checks จาก tooling จริง อัปเดต evidence/traceability และระบุ checks ที่ไม่ได้รัน

## Review
ตรวจ diff ของ `<task>` เทียบ requirements และ contract เน้น correctness, ownership, transitions, concurrent update, cache invalidation และ regression รายงาน severity, file/line, ผลกระทบ และวิธีพิสูจน์ อย่าอ้างว่า tests ผ่านโดยไม่มี output

## Deliver and learn
อ่าน evidence ของ `<tasks>` ตรวจ definition of done และ runbook สรุป readiness กับ blockers จากหลักฐาน ตรวจว่า P1 ทุกกรณีมีผล Pass หรือมี decision ของ owner ก่อนอ้าง Verify/Delivery readiness; หาก approval record ขัดกับเกณฑ์ให้รายงานความขัดแย้ง ไม่ตีความว่าอนุมัติ deviation โดยปริยาย อัปเดต run instructions/limitations และสร้าง follow-up tasks จาก findings โดยไม่ให้ AI ลงนาม gate แทน owner
