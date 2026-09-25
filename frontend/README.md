# Frontend

Next.js 16 / React 19 / TypeScript application foundation ตามโจทย์

- `src/app/`: routes/pages/layout
- `src/features/equipment-requests/components/`: request form/list/detail
- `src/features/equipment-requests/hooks/`: form/API/unsaved-state hooks
- `src/features/equipment-requests/api/`: API client และ DTO mapping
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
