# Project context

Purpose: ระบบให้ Employee ขออุปกรณ์ IT และ Approver พิจารณาคำขอ

Read order:
1. ../../AGENTS.md
2. ../../docs/product/requirements.md
3. ../../docs/product/assumptions.md
4. Current task, relevant contract and ADR

Constraints: Next.js/React/TypeScript; Spring Boot 4.x/Kotlin/Maven; PostgreSQL, Redis, Caffeine, k6 ตามโจทย์

Invariants: backend controls ownership/role/state/validation; edit DRAFT only; terminal states immutable; submit needs items; reject needs reason; stale version never overwrites; request/items update atomically

Source precedence: explicit user decisions → original assignment → accepted ADR/contract → derived summaries. ถ้า conflict ให้รายงานและแก้เอกสาร ไม่เดา requirement ใหม่

Current state ต้องดู task/evidence ล่าสุด ไม่ถือว่าโฟลเดอร์หรือ checklist แปลว่า feature implement แล้ว
