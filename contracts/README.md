# API contracts

Base path ตามโจทย์: `/api/v1/equipment-requests`

TASK-002 ต้องสร้าง `openapi.yaml` พร้อม schemas/examples จริงสำหรับ create, list, detail, update, submit, approve, reject และ cancel

ระบุ role/identity headers, ownership, expected version, field validation, pagination/sort, error envelope และ 400/404/409/422/500 รวมถึง denied-access behavior ที่ตกลงไว้ ไม่ส่ง entities เป็น response

Frontend/backend ใช้ contract เดียวกัน; ทุก breaking change ต้องปรับ consumers และ contract tests ในงานเดียวกัน
