# AI-native SDLC playbook

AI ช่วยผลิตและตรวจ artifact ทุกช่วง โดยใช้ไฟล์ใน repository เป็นบริบทถาวร เจ้าของงานยังรับผิดชอบ scope และความถูกต้องของผลลัพธ์ การใช้ AI ใน SDLC ไม่ได้หมายความว่า application ต้องมี AI feature

## Workflow

| ขั้น | Input | งานของ AI / ผู้รับผิดชอบ | Output | Exit criteria |
| --- | --- | --- | --- | --- |
| Discover | โจทย์และปัญหาผู้ใช้ | AI สกัด requirement; เจ้าของงานตอบความคลุมเครือที่กระทบ behavior | requirements, assumptions | มี requirement ID และ acceptance criteria |
| Design | requirements | AI เสนอ contract, data model, tradeoffs; ผู้พัฒนาตัดสินใจ | architecture, ADR, API contract | ระบุ ownership, concurrency, errors, caching |
| Plan | design และ backlog | AI แตกงาน; ผู้พัฒนากำหนดลำดับ | task packet | scope เล็ก, dependencies ชัด, ทดสอบได้ |
| Build | task และ context | AI/ผู้พัฒนา implement ทีละ slice | code, migrations, tests | ตรง acceptance criteria, ไม่มี unrelated changes |
| Verify | diff และ acceptance criteria | AI ช่วย review; ผู้พัฒนาตรวจผลจริง | evidence, findings, traceability | required checks ผ่าน; ไม่ใช้คำกล่าวของ AI เป็นหลักฐาน |
| Deliver | verified changes | ผู้พัฒนาทบทวน PR และ release readiness | PR, runbook, release record | วิธีรันและ rollback ใช้ได้; ไม่มี blocker |
| Learn | bugs และผลใช้งาน | AI สรุป pattern; เจ้าของงานจัดลำดับ | backlog, regression cases, prompt updates | สิ่งที่เรียนรู้มี action และ owner |

การเปลี่ยน phase ใช้ human approval gate ตาม [approval records](governance/approvals.md): Requirements → Design → Implement → Verify → Delivery ผู้อนุมัติอาจเป็นคนเดียวกันในโปรเจกต์ขนาดเล็ก แต่ AI ไม่อนุมัติงานของตนเอง และการเปลี่ยนสาระสำคัญทำให้ phase ที่ได้รับผลกระทบกลับเป็น In review

## Context loop ต่อหนึ่ง task

1. อ่าน AGENTS.md → ai/context/project.md → task → requirements/ADR/contract ที่เกี่ยวข้อง
2. สรุปสิ่งที่ทราบ, assumption และไฟล์ที่จะเปลี่ยนใน task packet
3. ทำ implementation และ verification ตามความเสี่ยง; ถ้า scope เปลี่ยน ให้อัปเดต task ก่อน
4. Review diff เทียบ acceptance criteria ไม่ใช่แค่ตรวจ syntax
5. บันทึก evidence, อัปเดต traceability และ handoff ที่คนหรือ AI รอบถัดไปอ่านต่อได้

## Definition of ready

- มี requirement IDs, acceptance criteria, dependencies และขอบเขตที่ไม่ทำ
- product ambiguity ที่ขวางงานได้รับคำตอบ หรือมี assumption ที่ระบุชัด
- มีวิธีตรวจผลและแหล่ง context ที่จำเป็น

## Definition of done

- acceptance criteria ผ่านพร้อมหลักฐานจริง และรายการตรวจที่ไม่ได้รันมีเหตุผล
- lint/typecheck/build/unit tests ของส่วนที่เปลี่ยนผ่านเมื่อมี tooling แล้ว
- integration/contract/concurrency tests ผ่านสำหรับงานที่เกี่ยวข้อง; k6 report สำหรับ performance task
- API, migration, run instructions และ traceability อัปเดตตามผลกระทบ
- ไม่มี unresolved blocker; reviewer และ release owner ระบุในงานส่งมอบ

## Working rhythm

ทำงานทีละ slice: draft → submit → decision → search/cache → performance/delivery ลด context ให้เหลือไฟล์ที่เกี่ยวข้อง เก็บเหตุผลการตัดสินใจใน ADR และบันทึกผลจริงใน evidence ไม่เก็บ chain-of-thought หรือข้อมูลลับ

## Improvement metrics

บันทึกต่อ task: เวลาเริ่ม/จบ, จำนวนรอบแก้ review, defect ที่หลุด, acceptance criteria ที่ผ่าน และต้นทุน AI หากมีข้อมูล เปรียบเทียบกับ baseline ของทีม ห้ามสร้างตัวเลขแทนผลที่ยังไม่ได้วัด
