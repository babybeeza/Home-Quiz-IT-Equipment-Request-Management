# k6 performance testing

ก่อนสร้าง script ให้กำหนด target environment, dataset size, role/user mix, search/detail/mutation proportions, VUs, ramp-up, duration และ thresholds ใน TASK-007

ตรวจ status/body correctness ควบคู่ latency; mutation ต้องใช้ ID/version ของ test data ที่เตรียมไว้ อย่ายิง load test ไปยัง production โดยไม่มีขอบเขตที่ได้รับอนุญาต

เก็บ scripts ในโฟลเดอร์นี้ และรายงานที่สรุปแล้วใน `results/` ใช้ [report template](results/TEMPLATE.md) แยก cold/warm cache และบันทึก dataset/setup/cleanup เพื่อให้รันซ้ำได้
