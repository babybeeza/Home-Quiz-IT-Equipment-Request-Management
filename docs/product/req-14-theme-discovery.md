# REQ-14 Discover: ปรับธีม UI ตามสีจาก Spark Deck

Status: Requirements baseline approved by the project owner 2026-09-26 (`main` `5597476`); later gates are in the [approval record](../governance/approvals.md)
Date: 2026-09-26
Source: คำขอใหม่ของผู้ใช้ “Discover ปรับ UI เป็น ธีม อ้าง สีจาก spark deck” และตารางสีที่ผู้ใช้ส่งในบทสนทนา 2026-09-26; ไม่ใช่ข้อกำหนดใน assignment HTML เดิม

## สิ่งที่ยืนยันได้

- ผู้ใช้ต้องการปรับธีม UI และให้ค่าสีอ้างอิงจาก Spark Deck โดยตรงตามตารางด้านล่าง จึงไม่ต้องเดาค่าสีจากชื่อ deck แม้ยังไม่มีไฟล์/เวอร์ชัน deck ใน repository
- UI ปัจจุบันใช้ธีมสว่าง สีหลักเขียว (`--primary: #176b5d`) ใน [`styles.css`](../../frontend/src/app/styles.css) และมีสีเขียนตรงในพื้นหลัง ฟอร์ม ปุ่ม สถานะ ข้อผิดพลาด และ conflict
- เส้นทาง UI ที่ได้รับผลคือ list/search, create/edit form และ detail ตาม [UI flow](../architecture/ui-flow.md) รวม modal การปฏิเสธ, loading/error/empty states และหน้าจอขนาดเล็ก
- สี `hlink` และ `folHlink` อยู่ใน template แต่ไม่อยู่ใน `THEME` ตามคำอธิบายของผู้ใช้; ต้องคงที่มาแยกจาก theme slots

## Palette ที่ผู้ใช้ระบุ

| Slot | ชื่อใน `THEME` | Hex | บทบาทที่ระบุ |
| --- | --- | --- | --- |
| dk1 | `dark` | `#000000` | ข้อความหลัก |
| lt1 | `light` | `#FFFFFF` | พื้นหลัง |
| dk2 | `grey_dark` | `#797979` | ยังไม่ระบุ |
| lt2 | `grey_light` | `#A9A9A9` | ยังไม่ระบุ |
| accent1 | `blue` | `#0050F0` | Confident Blue; สีหลักของ ttb |
| accent2 | `orange` | `#F68B1F` | Refreshing Orange |
| accent3 | `navy` | `#002C63` | Trusted Navy; title และ header ตาราง |
| accent4 | `red_orange` | `#F95922` | ยังไม่ระบุ |
| accent5 | `red` | `#DA2010` | ยังไม่ระบุ |
| accent6 | `yellow` | `#FEC800` | ยังไม่ระบุ |
| hlink | — | `#65B2E8` | สีลิงก์ใน template; ไม่อยู่ใน `THEME` |
| folHlink | — | `#1EB950` | สีลิงก์ที่กดแล้วใน template; ไม่อยู่ใน `THEME` |

ค่านี้เป็น **source palette** ที่ยืนยันจากผู้ใช้ ไม่ใช่การกำหนดว่าแต่ละสีใช้กับ control ทุกชนิดโดยตรง ตัวอย่างที่ชัดเจนคือ `blue` เป็นสีหลัก, `navy` สำหรับ title/header ตาราง, `dark` บน `light` สำหรับข้อความหลัก ส่วนสีสถานะ/ข้อความแจ้งเตือนต้อง mapping ใน Design

## ขอบเขตที่เสนอให้ตรวจใน Design

นำ palette ที่ผู้ใช้ให้มาจัดเป็น design tokens สำหรับพื้นหลัง พื้นผิว ข้อความ primary action, secondary action, border, focus, success, warning และ error แล้วใช้สม่ำเสมอทุกหน้าของแอป คงภาษา เนื้อหา route สิทธิ์ และ workflow เดิม สีสถานะต้องมีข้อความประกอบตาม [accessibility baseline](../architecture/ui-flow.md) ไม่อาศัยสีเพียงอย่างเดียว

คำขอระบุ **สี**; การเปลี่ยนโลโก้ ภาพประกอบ typography โครงหน้าใหม่ dark mode หรือ backend/API **ยังไม่ใช่ requirement ที่ยืนยันแล้ว** แม้ deck อาจมีองค์ประกอบเหล่านี้

## ข้อควรระวังเรื่อง contrast ก่อน Design

ตาม [WCAG 2.2 ระดับ AA](https://www.w3.org/TR/wcag/#contrast-minimum) ข้อความปกติต้องมี contrast อย่างน้อย 4.5:1, ข้อความใหญ่ 3:1 และ [องค์ประกอบ UI ที่จำเป็นต่อการรับรู้](https://www.w3.org/TR/wcag/#non-text-contrast) 3:1. การคำนวณเบื้องต้นกับพื้นขาวพบว่า `blue` 6.17:1 และ `navy` 13.63:1 ใช้เป็นข้อความปกติได้; `grey_dark` 4.35:1 ต่ำกว่า 4.5:1 เล็กน้อย. `orange` 2.43:1, `yellow` 1.56:1, `hlink` 2.31:1 และ `folHlink` 2.59:1 ไม่ควรใช้เป็นข้อความขนาดปกติบนพื้นขาวตรง ๆ. Design ต้องเลือกสีตัวอักษร/พื้นหลังที่เข้าคู่กันหรือเฉดที่ปรับเพื่อให้อ่านได้ โดยระบุความต่างจาก source palette ชัดเจน; ยังไม่ได้ทดสอบ contrast ของ UI จริงทุก state

## เกณฑ์รับงานที่สังเกตได้ (รอ Design และ approval)

1. Design ใช้ค่า hex ในตารางผู้ใช้เป็น source palette และบันทึก mapping เป็น UI tokens พร้อมบทบาทและคู่สี foreground/background; ไม่อ้างว่าได้ตรวจไฟล์ deck ที่ยังไม่ได้รับ
2. List, form, detail, navigation, buttons, filters, dialogs, status/error/conflict/empty/loading states ใช้ tokens ชุดเดียวกันทั้ง desktop และ mobile โดย workflow เดิมยังทำงาน
3. ข้อความและ interactive controls ผ่านเกณฑ์ contrast WCAG 2.2 AA ตามบริบท, focus indicator มองเห็น, และ status/error ไม่สื่อความหมายด้วยสีอย่างเดียว; ตรวจคู่สีจริงในทุก state ที่ใช้
4. การเปลี่ยนธีมไม่แก้ backend contract, role/ownership, validation หรือ state transitions; automated functional acceptance ที่เกี่ยวข้องยังผ่าน และมีการตรวจภาพ UI บน viewport อย่างน้อย desktop/mobile
5. เอกสารระบุว่า deck color ไหนถูกใช้หรือถูกปรับเพื่อความอ่านง่าย พร้อมเหตุผลของการปรับ

## คำถามที่ต้องตอบ

| ID | คำถาม | ผลกระทบถ้ายังไม่ตอบ |
| --- | --- | --- |
| Q-14-01 | สีอ้างอิงคืออะไร? | **Resolved:** ผู้ใช้ส่งค่า hex ทั้ง 12 สี; ไฟล์ deck ไม่จำเป็นต่อการระบุ palette นี้ แต่ยังไม่ได้รับเพื่อตรวจ visual details อื่น |
| Q-14-02 | ต้องการอ้างเฉพาะสีหรือรวม typography/logo/layout/ภาพ? | จากถ้อยคำปัจจุบันกำหนด scope เป็นสีเท่านั้น; องค์ประกอบอื่นต้องมีคำขอเพิ่ม |
| Q-14-03 | จับคู่หรือปรับเฉดอย่างไรเมื่อสีต้นทางมี contrast ไม่พอ? | ให้ Design เสนอ mapping ที่ใช้ source hex เมื่ออ่านได้ และระบุสีที่ปรับพร้อมเหตุผล ก่อน Implement |

การเลือก source palette ไม่ติดบล็อกแล้ว ขั้น Design ยังต้องเลือก semantic mapping และตรวจ contrast ก่อนเปลี่ยน UI.
