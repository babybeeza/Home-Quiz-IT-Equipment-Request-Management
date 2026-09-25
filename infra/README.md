# Infrastructure

พื้นที่สำหรับ local PostgreSQL/Redis และ deployment configuration หลังเลือก versions และ target แล้ว ยังไม่มี services ที่ provision หรือ Compose file ที่รันได้

เมื่อ implement: ระบุ ports, health checks, persistent volumes, environment variables, migrations และ seed data ใน README ใช้ `.env.example` เฉพาะชื่อ/ค่าตัวอย่างที่ไม่ใช่ความลับ ห้าม commit `.env`
