# Frontend

Next.js 16 / React 19 / TypeScript application สำหรับ create/view/edit equipment-request drafts

- `src/app/`: routes/pages/layout
- `src/features/equipment-requests/`: request form/detail, typed API client, identity/query providers และ custom form/dirty-state hooks
- `src/shared/`: shared UI และ utilities

## Commands

ต้องใช้ Node.js 24.15.0 (ดู `.nvmrc`) และ npm 11+

```bash
npm ci
npm run dev
npm run lint
npm run typecheck
npm test
npm run build
```

หากไม่มี Node บน host ให้รันคำสั่งผ่าน container จาก repository root เช่น:

```bash
docker run --rm -v "$PWD:/workspace" -w /workspace/frontend node:24.15.0-alpine npm ci
docker run --rm -v "$PWD:/workspace" -w /workspace/frontend node:24.15.0-alpine npm test
```

วาง behavior tests คู่กับ feature โดยใช้ Vitest และ React Testing Library

ตั้ง `NEXT_PUBLIC_API_BASE_URL` หาก backend ไม่ได้อยู่ที่ `http://localhost:8080/api/v1` ผู้ใช้ตัวอย่างถูกส่งผ่าน `X-User-Id` และ `X-Role`; selector นี้ใช้สำหรับ demo เท่านั้น
