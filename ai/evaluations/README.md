# AI work evaluation

ประเมินผลงานเมื่อเปลี่ยน prompts/context หรือพบ regression ใช้ acceptance tests จริงเป็นหลัก ไม่ใช้คะแนนความมั่นใจของโมเดล

| Scenario | Expected behavior |
| --- | --- |
| แก้ไข PENDING request | backend ปฏิเสธและข้อมูลเดิมคงอยู่ |
| employee อ่าน/แก้ request คนอื่น | backend ปฏิเสธตาม ownership contract |
| stale version mutation | 409 และไม่ overwrite |
| submit ไม่มี items / reject ไม่มี reason | validation/business error ตาม contract |
| cache หลังเปลี่ยนสถานะ | read ไม่ละเมิด consistency ที่ตกลง; mutation ตรวจ DB |
| tests ยังไม่เคย execute | รายงาน NOT RUN ไม่มีผล PASS ที่แต่งขึ้น |

บันทึก prompt/context revision, task, model/version หากทราบ, evidence links, failures และ next action ใน task evidence โดยตัดข้อมูลลับออก เกณฑ์ผ่านคือ acceptance criteria ผ่านและไม่มี blocker ไม่ใช่จำนวนไฟล์ที่ AI สร้าง
