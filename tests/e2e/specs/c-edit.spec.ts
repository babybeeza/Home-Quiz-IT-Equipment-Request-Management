import { expect, test } from "@playwright/test";
import { api, draft, signInAs, tag, USERS, UUID_PATH } from "./support";

test.describe("C. Editing and unsaved-changes protection", () => {
  test("AT-15 editing the title and removing an item is saved and becomes the new baseline", async ({ page, request }) => {
    const original = await api(request).create(USERS.somchai, draft(`${tag("at15")} before`, {
      items: [{ equipmentType: "NOTEBOOK", quantity: 1 }, { equipmentType: "MOUSE", quantity: 2 }],
    }));
    const edited = `${tag("at15")} after`;
    await signInAs(page, USERS.somchai);
    await page.goto(`/requests/${original.id}`);

    await page.getByRole("link", { name: "แก้ไข Draft" }).click();
    await page.getByLabel("หัวข้อคำขอ").fill(edited);
    await page.getByRole("button", { name: "ลบ" }).nth(1).click();
    await page.getByRole("button", { name: "บันทึก Draft" }).click();

    await expect(page).toHaveURL(UUID_PATH);
    await expect(page.getByRole("heading", { name: edited })).toBeVisible();
    await expect(page.locator(".item-list li")).toHaveCount(1);
    await page.getByRole("link", { name: "แก้ไข Draft" }).click();
    await expect(page.getByLabel("หัวข้อคำขอ")).toHaveValue(edited);
  });

  test("AT-16 changing only an item quantity is saved", async ({ page, request }) => {
    const original = await api(request).create(USERS.somchai, draft(`${tag("at16")} quantity`));
    await signInAs(page, USERS.somchai);
    await page.goto(`/requests/${original.id}/edit`);

    await page.getByLabel("จำนวน").fill("4");
    await page.getByRole("button", { name: "บันทึก Draft" }).click();

    await expect(page.getByRole("heading", { name: "รายการอุปกรณ์ (4 ชิ้น)" })).toBeVisible();
    expect((await api(request).get(USERS.somchai, original.id)).version).toBe(1);
  });

  test("AT-17 leaving a dirty form through the cancel button asks first", async ({ page, request }) => {
    const original = await api(request).create(USERS.somchai, draft(`${tag("at17")} dirty`));
    await signInAs(page, USERS.somchai);
    await page.goto(`/requests/${original.id}/edit`);
    await page.getByLabel("หัวข้อคำขอ").fill("Unsaved change here");

    // The confirm blocks the click until handled, so the handler must be registered before clicking.
    let message = "";
    page.once("dialog", async (dialog) => {
      message = dialog.message();
      await dialog.dismiss();
    });
    await page.getByRole("button", { name: "ยกเลิก" }).click();
    expect(message).toBe("มีข้อมูลที่ยังไม่ได้บันทึก ต้องการออกจากหน้านี้หรือไม่?");

    await expect(page).toHaveURL(/\/edit$/);
    await expect(page.getByLabel("หัวข้อคำขอ")).toHaveValue("Unsaved change here");
  });

  test("AT-18 closing the tab asks only when the form has unsaved changes", async ({ context, request }) => {
    const original = await api(request).create(USERS.somchai, draft(`${tag("at18")} unload`));
    const countPrompts = (tab: import("@playwright/test").Page) => {
      const seen = { count: 0 };
      tab.on("dialog", async (dialog) => {
        if (dialog.type() === "beforeunload") seen.count += 1;
        await dialog.dismiss(); // dismissing keeps the page open
      });
      return seen;
    };

    const clean = await context.newPage();
    await signInAs(clean, USERS.somchai);
    await clean.goto(`/requests/${original.id}/edit`);
    await clean.getByLabel("หัวข้อคำขอ").click(); // browsers only prompt after a user gesture
    const cleanPrompts = countPrompts(clean);
    await clean.close({ runBeforeUnload: true });
    await expect.poll(() => clean.isClosed()).toBe(true);
    expect(cleanPrompts.count).toBe(0);

    const dirty = await context.newPage();
    await dirty.goto(`/requests/${original.id}/edit`);
    await dirty.getByLabel("หัวข้อคำขอ").fill("Unsaved change here");
    const dirtyPrompts = countPrompts(dirty);
    await dirty.close({ runBeforeUnload: true });
    await expect.poll(() => dirtyPrompts.count).toBe(1);
    expect(dirty.isClosed()).toBe(false);
  });

  test("AT-19 AT-20 a stale save keeps the user's input, never overwrites, and reload shows the latest", async ({ context, request }) => {
    const original = await api(request).create(USERS.somchai, draft(`${tag("at19")} original`));
    const tabA = await context.newPage();
    await signInAs(tabA, USERS.somchai);
    await tabA.goto(`/requests/${original.id}/edit`);
    await expect(tabA.getByLabel("หัวข้อคำขอ")).toHaveValue(original.title);

    const tabB = await context.newPage();
    await tabB.goto(`/requests/${original.id}/edit`);
    await tabB.getByLabel("หัวข้อคำขอ").fill(`${original.title} saved by B`);
    await tabB.getByRole("button", { name: "บันทึก Draft" }).click();
    await expect(tabB).toHaveURL(UUID_PATH);

    await tabA.getByLabel("หัวข้อคำขอ").fill(`${original.title} typed in A`);
    await tabA.getByRole("button", { name: "บันทึก Draft" }).click();

    await expect(tabA.getByText("ข้อมูลนี้ถูกแก้ไขจากที่อื่นแล้ว ระบบยังเก็บค่าที่คุณกรอกไว้")).toBeVisible();
    await expect(tabA.getByLabel("หัวข้อคำขอ")).toHaveValue(`${original.title} typed in A`);
    expect((await api(request).get(USERS.somchai, original.id)).title).toBe(`${original.title} saved by B`);

    // AT-20: the explicit reload shows B's data and saving works again.
    tabA.on("dialog", (dialog) => dialog.accept()); // leaving the dirty form is intended here
    await tabA.getByRole("button", { name: "โหลดข้อมูลล่าสุด" }).click();
    await expect(tabA.getByLabel("หัวข้อคำขอ")).toHaveValue(`${original.title} saved by B`);
    await tabA.getByLabel("หัวข้อคำขอ").fill(`${original.title} final from A`);
    await tabA.getByRole("button", { name: "บันทึก Draft" }).click();
    await expect(tabA.getByRole("heading", { name: `${original.title} final from A` })).toBeVisible();
  });
});
