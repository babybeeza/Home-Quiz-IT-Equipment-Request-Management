# IT Equipment Request Management — AI-native SDLC

โครงสร้างตั้งต้นสำหรับพัฒนาระบบตาม [โจทย์ต้นฉบับ](Home-Quiz-IT-Equipment-Request-Management_revise_1.html) โดยให้มนุษย์และ AI ใช้ requirement, แผนงาน และหลักฐานทดสอบชุดเดียวกัน

**สถานะ:** TASK-001 foundation อยู่ระหว่าง human review มี Git repository, frontend/backend manifests, tests และ local PostgreSQL/Redis; business features ยังไม่ได้ implement

## Project structure

```text
.
├── AGENTS.md                     # กติกาการทำงานของ AI ใน repository
├── docs/
│   ├── playbook.md                # workflow, ผู้รับผิดชอบ และ quality gates
│   ├── product/                   # requirements, assumptions, traceability
│   ├── architecture/              # design และ decision records (ADR)
│   ├── delivery/                  # backlog และ task packets
│   ├── quality/                   # test strategy และ evidence
│   └── operations/                # release, rollback และ feedback
├── ai/
│   ├── context/                   # context ที่ต้องอ่านก่อนเริ่มงาน
│   ├── prompts/                   # prompt สำหรับแต่ละขั้นตอน SDLC
│   └── evaluations/               # เกณฑ์ประเมินงานที่ AI สร้าง
├── frontend/src/                  # Next.js / React / TypeScript
├── backend/src/                   # Spring Boot / Kotlin / Maven
├── contracts/                     # API contract และตัวอย่าง request/response
├── tests/                        # cross-system, contract และ k6 tests
├── infra/                        # local environment และ deployment config
└── .github/pull_request_template.md
```

## เริ่มใช้งาน

1. อ่าน [playbook](docs/playbook.md) และ [requirements](docs/product/requirements.md)
2. เริ่มจาก [TASK-001](docs/delivery/tasks/TASK-001-bootstrap.md): ตัดสินใจ runtime versions, bootstrap แอป และบันทึกคำสั่งจริง
3. อ่าน [แผนงานทั้งหมด](docs/delivery/implementation-plan.md) และเลือก task packet จาก [backlog](docs/delivery/backlog.md); งานเพิ่มเติมใช้ [task template](docs/delivery/tasks/TEMPLATE.md)
4. ให้ AI อ่าน [context](ai/context/project.md) และใช้ [prompts](ai/prompts/README.md) ทำงานทีละ vertical slice
5. แนบ test evidence และอัปเดต [traceability](docs/product/traceability.md) ก่อนปิดงาน

ทุก phase ต้องผ่าน [human approval gate](docs/governance/approvals.md) ตามลำดับ Requirements → Design → Implement → Verify → Delivery

## Run foundation locally

Prerequisites: Docker Desktop; JDK 21+ สำหรับรัน backend บน host; Node.js 24.15.0 หรือใช้ Node container

```powershell
Copy-Item .env.example .env
docker compose up -d --wait
cd backend
.\mvnw.cmd spring-boot:run
```

อีก terminal จาก repository root:

```powershell
docker run --rm -it -p 3000:3000 -v "${PWD}:/workspace" -w /workspace/frontend node:24.15.0-alpine npm run dev -- --hostname 0.0.0.0
```

Frontend: http://localhost:3000 ส่วน backend เริ่มที่ http://localhost:8080 และยังไม่มี business endpoints จนกว่า TASK-003 จะเสร็จ ใช้ `docker compose down` เพื่อหยุด local servicesโดย volumes ยังอยู่

Pinned stack: Next.js 16.3.6, React 19.3.0, TypeScript 5.9.3, Node.js 24.15.0; Spring Boot 4.1.1, Kotlin 2.3.21, Java 21 target, Maven Wrapper; PostgreSQL 17 และ Redis 8 ดูเหตุผลใน [ADR-001](docs/architecture/decisions/ADR-001-toolchains.md)

## Assumptions / limitations

- ใช้โครงสร้าง frontend/backend แยกกันใน repository เดียว
- จำลอง role ได้ตามโจทย์; ต้องกำหนด user identity สำหรับ ownership และตรวจสิทธิ์ที่ backend
- ยังไม่ได้เลือก deployment target หรือ provision external services
- อ่านประเด็นค้างและแนวทางเสนอใน [assumptions](docs/product/assumptions.md)
