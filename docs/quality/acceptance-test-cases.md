# Acceptance test cases (Verify gate)

Status: Ready for execution by the QA / acceptance owner. Automated run recorded (below); manual QA results are not recorded yet.
Revision under test: `main` at `d3ca981` (TASK-001–007 merged)
Requirements: [REQ-01–REQ-13](../product/requirements.md). Source of truth: the [original assignment](../../Home-Quiz-IT-Equipment-Request-Management_revise_1.html)

**Automation:** 53 of 55 cases are automated with Playwright in [`tests/e2e`](../../tests/e2e/README.md) (`bash tests/e2e/run-e2e.sh`, test titles carry the AT-ID). Latest run 49/49 tests passed ([TASK-008 evidence](evidence/TASK-008.md)). AT-44 and AT-46 remain manual. Automated results are evidence; the QA sign-off below is still a human decision.

These are **black-box acceptance tests** run by a person through the UI, with a few API calls for the rules the backend must enforce even when the UI hides them. The automated tests (backend 128, frontend 31, k6) are listed in [TASK-007 evidence](evidence/TASK-007.md). This suite checks the system from the user's point of view and does not repeat that automation.

## Setup

1. Start the system by following the [README](../../README.md) (Compose → backend → frontend) and open `http://localhost:3000/requests`.
2. Switch users with the **ผู้ใช้งานตัวอย่าง** selector in the top-right corner:
   - **สมชาย — Employee** (`employee-001`)
   - **สมหญิง — Employee** (`employee-002`)
   - **หัวหน้าฝ่าย — Approver** (`approver-001`)
3. **Search/pagination data (section F):** run [`seed-search-dataset.sql`](../../tests/performance/seed-search-dataset.sql). It adds 1,200 requests owned by `seed-employee-*`, which only the Approver can see.
4. **API cases (section H):** use `curl` or any HTTP client against `http://localhost:8080/api/v1`, with the `X-User-Id` and `X-Role` headers.
5. For each case, record **Pass / Fail / Blocked** in the Result column. For Fail, add the actual behavior and a screenshot or response in the notes, and open a defect.

Priority: **P1** = a mandatory assignment rule; its failure blocks the Verify gate. **P2** = important UX or quality; its failure needs a follow-up but does not block the gate by itself.

## A. Identity and roles (REQ-01)

| ID | Pri | Preconditions | Steps | Expected result | Result |
| --- | --- | --- | --- | --- | --- |
| AT-01 | P1 | Signed in as สมชาย | Open `/requests` | The **สร้างคำขอใหม่** button is shown, and the table shows only สมชาย's requests | |
| AT-02 | P1 | Signed in as หัวหน้าฝ่าย | Open `/requests` | No **สร้างคำขอใหม่** button; the table shows every user's requests | |
| AT-03 | P1 | สมชาย has a request | Copy the request's detail URL → switch to สมหญิง → open that URL | "คุณไม่มีสิทธิ์ดูคำขอนี้" is shown and no request data appears | |
| AT-04 | P2 | You are on a detail or edit page | Switch users with the selector | You are taken back to `/requests`, which shows only the new user's data, with nothing left over from the previous user | |

## B. Creating a draft and form validation (REQ-03, REQ-04)

| ID | Pri | Preconditions | Steps | Expected result | Result |
| --- | --- | --- | --- | --- | --- |
| AT-05 | P1 | สมชาย, `/requests/new` | Click **บันทึก Draft** without filling anything in | Errors appear under each field: "กรุณาระบุชื่ออย่างน้อย 2 ตัวอักษร", "รูปแบบอีเมลไม่ถูกต้อง", "กรุณาระบุแผนก", "หัวข้อต้องมีอย่างน้อย 5 ตัวอักษร", "วัตถุประสงค์ต้องมีอย่างน้อย 10 ตัวอักษร", "กรุณาระบุวันที่"; nothing is saved | |
| AT-06 | P1 | Same | Enter boundary values: 1-character name, invalid email, 4-character title, 9-character purpose | Every field shows its error; saving is blocked | |
| AT-07 | P1 | Same | Pick yesterday as **วันที่ต้องการใช้** | "วันที่ต้องไม่เป็นอดีต"; today is accepted | |
| AT-08 | P1 | Same | Add 3 items with **+ เพิ่มรายการ** → delete the middle one with **ลบ** | Exactly the chosen row is removed and the other two keep their values | |
| AT-09 | P1 | Same | Enter quantity 0, then 6 | "จำนวนต่ำสุด 1" and "จำนวนสูงสุด 5"; 1–5 is accepted | |
| AT-10 | P2 | Same | Enter a specification over 250 characters, or an additional note over 500 | The error is shown and saving is blocked | |
| AT-11 | P1 | Same | Fill in valid data **without any items** → **บันทึก Draft** | Saved. You land on the detail page, which shows DRAFT, a number `REQ-<year>-NNNNNN`, and "ยังไม่มีรายการอุปกรณ์" | |
| AT-12 | P1 | Same | Fill in valid data with 2 items (quantities 2 and 3) → save | The detail page shows both items and "รายการอุปกรณ์ (5 ชิ้น)" (totalItems = sum of quantities) | |
| AT-13 | P1 | Same | Fill in data → click **บันทึก Draft** twice quickly | The button shows "กำลังบันทึก…" and is disabled; only **1** request is created (check the list) | |
| AT-14 | P2 | Same | In the **แผนก** field, open the suggestion list | Suggestions show 6 departments (Software Engineering, Finance, …), and a department not in the list can still be typed | |

## C. Editing and unsaved-changes protection (REQ-04, REQ-07)

| ID | Pri | Preconditions | Steps | Expected result | Result |
| --- | --- | --- | --- | --- | --- |
| AT-15 | P1 | สมชาย's DRAFT | Detail page → **แก้ไข Draft** → change the title and delete an item → save | The detail page shows the new title and remaining items. A follow-up edit starts from the saved data | |
| AT-16 | P1 | Same | Edit, then change only the quantity of an existing item → save | Saved and shown correctly (an item-only edit also counts as an edit) | |
| AT-17 | P2 | Editing without saving | Change a field → click **ยกเลิก** | "มีข้อมูลที่ยังไม่ได้บันทึก ต้องการออกจากหน้านี้หรือไม่?" appears; Cancel keeps you on the page | |
| AT-18 | P2 | Same | Change a field → refresh or close the tab | The browser asks before leaving. An unchanged form does not ask | |
| AT-19 | P1 | Edit page open (tab A) | In tab B, open the same request and save a change → return to tab A, change something else and save | Tab A reports that the data was changed elsewhere, shows **โหลดข้อมูลล่าสุด**, and keeps what you typed. Tab B's data is **not** overwritten | |
| AT-20 | P1 | Same as AT-19 | In tab A, click **โหลดข้อมูลล่าสุด** | Tab A reloads with tab B's data, and saving works normally again | |

## D. Workflow: submit, approve, reject and cancel (REQ-01, REQ-02, REQ-03)

| ID | Pri | Preconditions | Steps | Expected result | Result |
| --- | --- | --- | --- | --- | --- |
| AT-21 | P1 | สมชาย's DRAFT without items | Detail page → **ส่งคำขอ** | "ต้องมีรายการอุปกรณ์อย่างน้อย 1 รายการก่อนส่งคำขอ"; the status stays DRAFT | |
| AT-22 | P1 | สมชาย's DRAFT with items | **ส่งคำขอ** | "ส่งคำขอเพื่อพิจารณาแล้ว"; status PENDING; the **แก้ไข Draft** button disappears; only **ยกเลิกคำขอ** remains | |
| AT-23 | P1 | Request from AT-22, signed in as หัวหน้าฝ่าย | Detail page → **อนุมัติ** | Status APPROVED; no action buttons remain | |
| AT-24 | P1 | Another PENDING request, หัวหน้าฝ่าย | **ปฏิเสธ** → leave the reason empty or spaces only → **ยืนยันปฏิเสธ** | "กรุณาระบุเหตุผลการปฏิเสธ"; the dialog stays open and the status is unchanged | |
| AT-25 | P1 | Same | Enter a reason → **ยืนยันปฏิเสธ** | Status REJECTED; the detail page shows **เหตุผลการปฏิเสธ**; the owner (สมชาย) sees the same reason | |
| AT-26 | P1 | สมชาย's DRAFT, and another one that is PENDING | **ยกเลิกคำขอ** → confirm | Both become CANCELLED; clicking Cancel in the confirmation instead leaves the status unchanged | |
| AT-27 | P1 | Requests in APPROVED, REJECTED and CANCELLED | Open each one as both the owner and the Approver | No action buttons and no **แก้ไข Draft** for any of them (view only) | |
| AT-28 | P1 | สมชาย's DRAFT | Sign in as หัวหน้าฝ่าย and open it | The Approver can view it but has no action buttons (Approve/Reject exist only for PENDING) | |
| AT-29 | P2 | A PENDING request open in two tabs as the Approver | Click **อนุมัติ** in tab A, then **ปฏิเสธ** with a reason in tab B | Tab B reports a conflict with a reload button; the status stays APPROVED and is not overwritten | |
| AT-30 | P2 | Any action pending | Click the action button several times quickly | Only one action takes effect; every button is disabled while the request is in progress | |

## E. List page (REQ-05, assignment §4.2)

| ID | Pri | Preconditions | Steps | Expected result | Result |
| --- | --- | --- | --- | --- | --- |
| AT-31 | P1 | There are requests | Open `/requests` | The table has all 8 columns: เลขที่คำขอ, หัวข้อ, ผู้ขอ, แผนก, วันที่ต้องการใช้, จำนวนรวม, สถานะ, สร้างเมื่อ. Status is shown as text | |
| AT-32 | P1 | Same | Click a request number | You go to that request's detail page | |
| AT-33 | P1 | **Open item G-1** | Look for the per-status action buttons (Edit · Submit · Cancel / Approve · Reject · Cancel) **in the list page** | **Current behavior:** actions are on the detail page, not in list rows (TASK-005 decision). Record Pass/Fail against the assignment §4.2 criterion, and a Fail needs a decision from the owner | |

## F. Search, filter and pagination (REQ-05, REQ-08); run the seed data first

| ID | Pri | Preconditions | Steps | Expected result | Result |
| --- | --- | --- | --- | --- | --- |
| AT-34 | P1 | หัวหน้าฝ่าย | In **ค้นหา**, type `900777` (part of a request number) | The row `REQ-2026-900777` is found | |
| AT-35 | P1 | Same | Search `MONITOR` (uppercase), then `ergonomic`, then `kanokwan` | Case is ignored. The first two match on title and the last on employee name | |
| AT-36 | P1 | Same | Search `monitor` + **สถานะ** = PENDING + **แผนก** = `software engineering` | Only rows that meet **all three** conditions appear, and the total matches the table | |
| AT-37 | P1 | Same | Pick filters that match nothing (e.g. search `zzzz`) | "ไม่พบคำขอที่ตรงกับเงื่อนไข" with a **ล้างตัวกรอง** button, which returns you to the full list | |
| AT-38 | P1 | Same (1,200+ rows) | Look at the pagination bar → **ถัดไป** → **ก่อนหน้า** | Shows "หน้า N จาก M · ทั้งหมด X รายการ" with correct numbers; **ก่อนหน้า** is disabled on page 1 and **ถัดไป** on the last page | |
| AT-39 | P1 | Same | Change **เรียงตามวันที่สร้าง** between new → old and old → new | Order is by creation date in the chosen direction, and paging through shows no duplicate rows | |
| AT-40 | P1 | Same | Go to page 3, then change the status filter | You return to page 1 and the filter still applies | |
| AT-41 | P1 | Same | Set filters, then copy the URL into a new tab; press Back/Forward; refresh | The new tab shows the same results. Back/Forward return to the previous filters, and refresh keeps them (e.g. `?keyword=…&status=…`) | |
| AT-42 | P2 | Same | Type a search term quickly | Results update once after you stop typing (~0.3 s) and never flicker back to results for an older term | |
| AT-43 | P1 | สมชาย | Search for something that exists only in the seed data (e.g. `900777`) | Not found: an Employee cannot see other users' requests through search | |

## G. States and error handling (REQ-04, REQ-06)

| ID | Pri | Preconditions | Steps | Expected result | Result |
| --- | --- | --- | --- | --- | --- |
| AT-44 | P2 | Backend stopped | Open `/requests`, try to save a form, then open a detail page | The list shows "โหลดรายการไม่สำเร็จ" with **ลองใหม่**. Saving shows "ไม่สามารถเชื่อมต่อกับระบบได้ กรุณาลองใหม่" and keeps what you typed. Detail shows an error with no raw technical detail | |
| AT-45 | P2 | Any user | Open `/requests/00000000-0000-0000-0000-000000000000` | "ไม่พบคำขอ" | |
| AT-46 | P2 | Redis stopped (`docker compose stop redis`) | Open detail pages and the list, and do one action | Everything still works (possibly a little slower), with no error. Start Redis again afterwards | |

## H. Rules the backend must enforce even if the UI hides them (REQ-01–03, REQ-06, REQ-07); via API

Use a request `{id}` and its current `version` from `GET /equipment-requests/{id}`. Every error must use the `timestamp, status, code, message, path, fieldErrors` format.

| ID | Pri | Request | Expected result | Result |
| --- | --- | --- | --- | --- |
| AT-47 | P1 | `POST /equipment-requests/{id}/approve` with `X-Role: EMPLOYEE` (the owner) | 403 `ACCESS_DENIED`; status unchanged | |
| AT-48 | P1 | `PUT /equipment-requests/{id}` on a request that is not DRAFT, with the current version | 409 `REQUEST_STATE_CONFLICT` | |
| AT-49 | P1 | `PUT` with `expectedVersion` lower than the current version | 409 `REQUEST_VERSION_CONFLICT`, and a later `GET` shows the data **unchanged** | |
| AT-50 | P1 | Send `POST /equipment-requests` twice: (a) otherwise valid, with `requiredDate` = yesterday; (b) otherwise valid, with item `quantity: 9` | (a) 400 `VALIDATION_ERROR` with `fieldErrors.requiredDate`; (b) 400 `VALIDATION_ERROR` with `fieldErrors["items[0].quantity"]`. Neither creates a request | |
| AT-51 | P1 | `POST /{id}/approve` on a request that is already APPROVED (with its current version) | 409 `REQUEST_STATE_CONFLICT` (approving twice is not allowed) | |
| AT-52 | P1 | `POST /{id}/reject` with `{"expectedVersion":N}` and no `reason` | 422 `REJECTION_REASON_REQUIRED` | |
| AT-53 | P1 | Send `"status":"APPROVED"` in the body of `POST /equipment-requests` or `PUT` | The status field is ignored; a new request is always DRAFT and an edit does not change the status | |
| AT-54 | P1 | `GET /equipment-requests` with `X-User-Id: employee-002`, `X-Role: EMPLOYEE` | Only employee-002's own requests appear (`totalElements` counts only theirs) | |
| AT-55 | P2 | No `X-User-Id` header, or `X-Role: ADMIN` | 400 `MALFORMED_REQUEST` | |

## Coverage map

| Requirement | Cases |
| --- | --- |
| REQ-01 Roles and ownership | AT-01–04, 23, 28, 43, 47, 54 |
| REQ-02 Transitions and terminal states | AT-22–27, 48, 51, 53 |
| REQ-03 Validation and business rules | AT-05–12, 21, 24, 50, 52 |
| REQ-04 Form behavior | AT-08, 13, 15–20, 30, 44 |
| REQ-05 List, search, pagination | AT-31–43 |
| REQ-06 API and error format | AT-45, 47–55 |
| REQ-07 Version and data safety | AT-16, 19–20, 29, 49 |
| REQ-08 React state (URL, race, stale) | AT-04, 41–42 |
| REQ-10 Caching as users see it | AT-14, 46 |
| REQ-09, 11, 12, 13 | Automated / documentation. See [TASK-007 evidence](evidence/TASK-007.md) |

## QA sign-off

| Item | Value |
| --- | --- |
| Tester | |
| Execution date / environment | |
| P1 result | __ / 43 Pass |
| P2 result | __ / 12 Pass |
| Defects opened | |
| Decision on G-1 (AT-33) | |
| Verify gate decision | Approved / Changes requested |

The Verify gate may be approved only when every P1 case passes, or its failure has a decision recorded. Record that decision in [approvals.md](../governance/approvals.md).
