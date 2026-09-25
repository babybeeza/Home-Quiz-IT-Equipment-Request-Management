import { expect, test, type Page } from "@playwright/test";
import { api, arrange, draft, signInAs, tag, USERS } from "./support";

const actions = (page: Page) => page.getByRole("group", { name: "การดำเนินการกับคำขอ" });
const status = (page: Page) => page.locator(".page-heading .status-pill");

test.describe("D. Workflow: submit, approve, reject and cancel", () => {
  test("AT-21 submitting a draft without items is refused", async ({ page, request }) => {
    const empty = await api(request).create(USERS.somchai, draft(`${tag("at21")} empty`, { items: [] }));
    await signInAs(page, USERS.somchai);
    await page.goto(`/requests/${empty.id}`);

    await actions(page).getByRole("button", { name: "ส่งคำขอ" }).click();

    await expect(page.getByText("ต้องมีรายการอุปกรณ์อย่างน้อย 1 รายการก่อนส่งคำขอ")).toBeVisible();
    await expect(status(page)).toHaveText("DRAFT");
  });

  test("AT-22 AT-23 owner submits, then approver approves and no actions remain", async ({ page, request }) => {
    const created = await arrange(request, "DRAFT", `${tag("at22")} approve path`);
    await signInAs(page, USERS.somchai);
    await page.goto(`/requests/${created.id}`);

    await actions(page).getByRole("button", { name: "ส่งคำขอ" }).click();
    await expect(page.getByRole("status").filter({ hasText: "ส่งคำขอเพื่อพิจารณาแล้ว" })).toBeAttached();
    await expect(status(page)).toHaveText("PENDING");
    await expect(page.getByRole("link", { name: "แก้ไข Draft" })).toHaveCount(0);
    await expect(actions(page).getByRole("button")).toHaveText(["ยกเลิกคำขอ"]);

    await signInAs(page, USERS.approver);
    await page.goto(`/requests/${created.id}`);
    await actions(page).getByRole("button", { name: "อนุมัติ" }).click();
    await expect(status(page)).toHaveText("APPROVED");
    await expect(actions(page)).toHaveCount(0);
  });

  test("AT-24 AT-25 reject needs a reason, and the reason is shown to approver and owner", async ({ page, request }) => {
    const pending = await arrange(request, "PENDING", `${tag("at24")} reject path`);
    await signInAs(page, USERS.approver);
    await page.goto(`/requests/${pending.id}`);

    await actions(page).getByRole("button", { name: "ปฏิเสธ" }).click();
    const dialog = page.getByRole("dialog", { name: "ปฏิเสธคำขอ" });
    await dialog.getByLabel("เหตุผลการปฏิเสธ").fill("   ");
    await dialog.getByRole("button", { name: "ยืนยันปฏิเสธ" }).click();
    await expect(dialog.getByText("กรุณาระบุเหตุผลการปฏิเสธ")).toBeVisible();
    await expect(status(page)).toHaveText("PENDING");

    await dialog.getByLabel("เหตุผลการปฏิเสธ").fill("Budget exceeded this quarter");
    await dialog.getByRole("button", { name: "ยืนยันปฏิเสธ" }).click();
    await expect(dialog).toHaveCount(0);
    await expect(status(page)).toHaveText("REJECTED");
    await expect(page.getByText("Budget exceeded this quarter")).toBeVisible();

    await signInAs(page, USERS.somchai);
    await page.goto(`/requests/${pending.id}`);
    await expect(page.getByRole("heading", { name: "เหตุผลการปฏิเสธ" })).toBeVisible();
    await expect(page.getByText("Budget exceeded this quarter")).toBeVisible();
  });

  test("AT-26 owner cancels DRAFT and PENDING only after confirming", async ({ page, request }) => {
    const draftRequest = await arrange(request, "DRAFT", `${tag("at26")} draft`);
    const pendingRequest = await arrange(request, "PENDING", `${tag("at26")} pending`);
    await signInAs(page, USERS.somchai);

    await page.goto(`/requests/${draftRequest.id}`);
    page.once("dialog", (dialog) => dialog.dismiss());
    await actions(page).getByRole("button", { name: "ยกเลิกคำขอ" }).click();
    await expect(status(page)).toHaveText("DRAFT");

    for (const target of [draftRequest, pendingRequest]) {
      await page.goto(`/requests/${target.id}`);
      page.once("dialog", (dialog) => {
        expect(dialog.message()).toContain("ยืนยันยกเลิกคำขอนี้");
        return dialog.accept();
      });
      await actions(page).getByRole("button", { name: "ยกเลิกคำขอ" }).click();
      await expect(status(page)).toHaveText("CANCELLED");
    }
  });

  test("AT-27 terminal requests are view-only for owner and approver", async ({ page, request }) => {
    const marker = tag("at27");
    const terminal = [
      await arrange(request, "APPROVED", `${marker} approved`),
      await arrange(request, "REJECTED", `${marker} rejected`),
      await arrange(request, "CANCELLED", `${marker} cancelled`),
    ];
    for (const user of [USERS.somchai, USERS.approver]) {
      await signInAs(page, user);
      for (const target of terminal) {
        await page.goto(`/requests/${target.id}`);
        await expect(status(page)).toHaveText(target.status);
        await expect(actions(page)).toHaveCount(0);
        await expect(page.getByRole("link", { name: "แก้ไข Draft" })).toHaveCount(0);
      }
    }
  });

  test("AT-28 approver can view a DRAFT but has no actions on it", async ({ page, request }) => {
    const created = await arrange(request, "DRAFT", `${tag("at28")} draft`);
    await signInAs(page, USERS.approver);
    await page.goto(`/requests/${created.id}`);

    await expect(page.getByRole("heading", { name: created.title })).toBeVisible();
    await expect(actions(page)).toHaveCount(0);
    await expect(page.getByRole("link", { name: "แก้ไข Draft" })).toHaveCount(0);
  });

  test("AT-29 a decision made in another tab wins; the second decision gets a conflict", async ({ context, request }) => {
    const pending = await arrange(request, "PENDING", `${tag("at29")} race`);
    const tabA = await context.newPage();
    await signInAs(tabA, USERS.approver);
    await tabA.goto(`/requests/${pending.id}`);
    const tabB = await context.newPage();
    await tabB.goto(`/requests/${pending.id}`);
    await expect(actions(tabB).getByRole("button", { name: "ปฏิเสธ" })).toBeVisible();

    await actions(tabA).getByRole("button", { name: "อนุมัติ" }).click();
    await expect(status(tabA)).toHaveText("APPROVED");

    await actions(tabB).getByRole("button", { name: "ปฏิเสธ" }).click();
    const dialog = tabB.getByRole("dialog", { name: "ปฏิเสธคำขอ" });
    await dialog.getByLabel("เหตุผลการปฏิเสธ").fill("Too late");
    await dialog.getByRole("button", { name: "ยืนยันปฏิเสธ" }).click();

    await expect(tabB.getByText("คำขอนี้ถูกเปลี่ยนจากที่อื่นแล้ว")).toBeVisible();
    await expect(tabB.getByRole("button", { name: "โหลดข้อมูลล่าสุด" })).toBeVisible();
    expect((await api(request).get(USERS.approver, pending.id)).status).toBe("APPROVED");
  });

  test("AT-30 rapid repeated clicks send one action and disable every button meanwhile", async ({ page, request }) => {
    const created = await arrange(request, "DRAFT", `${tag("at30")} rapid`);
    let submits = 0;
    await page.route("**/submit", async (route) => {
      submits += 1;
      await new Promise((resolve) => setTimeout(resolve, 800));
      await route.continue();
    });
    await signInAs(page, USERS.somchai);
    await page.goto(`/requests/${created.id}`);

    await actions(page).getByRole("button", { name: "ส่งคำขอ" }).dblclick();
    for (const button of await actions(page).getByRole("button").all()) await expect(button).toBeDisabled();
    await expect(status(page)).toHaveText("PENDING");

    expect(submits).toBe(1);
    expect((await api(request).get(USERS.somchai, created.id)).version).toBe(1);
  });
});
