import { expect, test } from "@playwright/test";
import { api, bangkokDate, fillValidForm, signInAs, tag, USERS, UUID_PATH } from "./support";

test.describe("B. Creating a draft and form validation", () => {
  test.beforeEach(async ({ page }) => {
    await signInAs(page, USERS.somchai);
    await page.goto("/requests/new");
  });

  const save = (page: import("@playwright/test").Page) => page.getByRole("button", { name: "บันทึก Draft" });

  test("AT-05 empty submit shows every required-field error and saves nothing", async ({ page }) => {
    await save(page).click();

    for (const message of [
      "กรุณาระบุชื่ออย่างน้อย 2 ตัวอักษร", "รูปแบบอีเมลไม่ถูกต้อง", "กรุณาระบุแผนก",
      "หัวข้อต้องมีอย่างน้อย 5 ตัวอักษร", "วัตถุประสงค์ต้องมีอย่างน้อย 10 ตัวอักษร", "กรุณาระบุวันที่",
    ]) {
      await expect(page.getByText(message)).toBeVisible();
    }
    await expect(page).toHaveURL(/\/requests\/new$/);
  });

  test("AT-06 values just below the minimum lengths are rejected", async ({ page }) => {
    await fillValidForm(page, "Four");
    await page.getByLabel("ชื่อพนักงาน").fill("ก");
    await page.getByLabel("อีเมล").fill("not-an-email");
    await page.getByLabel("วัตถุประสงค์").fill("123456789");
    await save(page).click();

    await expect(page.getByText("กรุณาระบุชื่ออย่างน้อย 2 ตัวอักษร")).toBeVisible();
    await expect(page.getByText("รูปแบบอีเมลไม่ถูกต้อง")).toBeVisible();
    await expect(page.getByText("หัวข้อต้องมีอย่างน้อย 5 ตัวอักษร")).toBeVisible();
    await expect(page.getByText("วัตถุประสงค์ต้องมีอย่างน้อย 10 ตัวอักษร")).toBeVisible();
    await expect(page).toHaveURL(/\/requests\/new$/);
  });

  test("AT-07 a past date is rejected and today is accepted", async ({ page }) => {
    await fillValidForm(page, `${tag("at07")} date check`);
    await page.getByLabel("วันที่ต้องการใช้").fill(bangkokDate(-1));
    await save(page).click();
    await expect(page.getByText("วันที่ต้องไม่เป็นอดีต")).toBeVisible();

    await page.getByLabel("วันที่ต้องการใช้").fill(bangkokDate(0));
    await save(page).click();
    await expect(page).toHaveURL(UUID_PATH);
  });

  test("AT-08 item rows are added and exactly the chosen row is removed", async ({ page }) => {
    const add = page.getByRole("button", { name: "+ เพิ่มรายการ" });
    for (const spec of ["A-first", "B-middle", "C-last"]) {
      await add.click();
      await page.getByLabel("รายละเอียด").last().fill(spec);
    }

    await page.getByRole("button", { name: "ลบ" }).nth(1).click();

    const specs = page.getByLabel("รายละเอียด");
    await expect(specs).toHaveCount(2);
    await expect(specs.nth(0)).toHaveValue("A-first");
    await expect(specs.nth(1)).toHaveValue("C-last");
  });

  test("AT-09 quantity must be between 1 and 5", async ({ page }) => {
    await page.getByRole("button", { name: "+ เพิ่มรายการ" }).click();
    const quantity = page.getByLabel("จำนวน");

    await quantity.fill("0");
    await save(page).click();
    await expect(page.getByText("จำนวนต่ำสุด 1")).toBeVisible();

    await quantity.fill("6");
    await save(page).click();
    await expect(page.getByText("จำนวนสูงสุด 5")).toBeVisible();
  });

  test("AT-10 specification over 250 and note over 500 characters block saving", async ({ page }) => {
    await fillValidForm(page, `${tag("at10")} length`);
    await page.getByRole("button", { name: "+ เพิ่มรายการ" }).click();
    await page.getByLabel("รายละเอียด").fill("s".repeat(251));
    await page.getByLabel("หมายเหตุเพิ่มเติม").fill("n".repeat(501));
    await save(page).click();

    await expect(page.locator(".item-row small[role=alert]")).toBeVisible();
    await expect(page.getByLabel("หมายเหตุเพิ่มเติม").locator("xpath=following-sibling::small")).toBeVisible();
    await expect(page).toHaveURL(/\/requests\/new$/);
  });

  test("AT-11 a valid draft without items is saved as DRAFT with a request number", async ({ page }) => {
    const title = `${tag("at11")} no items`;
    await fillValidForm(page, title);
    await save(page).click();

    await expect(page).toHaveURL(UUID_PATH);
    await expect(page.getByRole("heading", { name: title })).toBeVisible();
    await expect(page.locator(".status-pill").first()).toHaveText("DRAFT");
    await expect(page.locator(".eyebrow").first()).toHaveText(/^REQ-\d{4}-\d{6,}$/);
    await expect(page.getByText("ยังไม่มีรายการอุปกรณ์")).toBeVisible();
  });

  test("AT-12 totalItems is the sum of item quantities", async ({ page }) => {
    await fillValidForm(page, `${tag("at12")} totals`);
    const add = page.getByRole("button", { name: "+ เพิ่มรายการ" });
    await add.click();
    await page.getByLabel("จำนวน").nth(0).fill("2");
    await add.click();
    await page.getByLabel("ประเภท").nth(1).selectOption("MONITOR");
    await page.getByLabel("จำนวน").nth(1).fill("3");
    await save(page).click();

    await expect(page).toHaveURL(UUID_PATH);
    await expect(page.getByRole("heading", { name: "รายการอุปกรณ์ (5 ชิ้น)" })).toBeVisible();
    await expect(page.locator(".item-list li")).toHaveCount(2);
  });

  test("AT-13 double click on save creates exactly one request", async ({ page, request }) => {
    const title = `${tag("at13")} double click`;
    await fillValidForm(page, title);
    let creates = 0;
    // Slow the create call so the second click lands while the first is still pending.
    await page.route("**/api/v1/equipment-requests", async (route) => {
      if (route.request().method() === "POST") {
        creates += 1;
        await new Promise((resolve) => setTimeout(resolve, 800));
      }
      await route.continue();
    });

    await save(page).dblclick();
    await expect(page.getByRole("button", { name: "กำลังบันทึก…" })).toBeDisabled();
    await expect(page).toHaveURL(UUID_PATH);

    expect(creates).toBe(1);
    const found = await api(request).list(USERS.somchai, `?keyword=${encodeURIComponent(title)}`);
    expect(found.totalElements).toBe(1);
  });

  test("AT-14 department offers suggestions but stays free text", async ({ page }) => {
    const department = page.getByLabel("แผนก");
    const listId = await department.getAttribute("list");
    const options = page.locator(`datalist[id="${listId}"] option`);

    await expect(options).toHaveCount(6);
    await expect(options.first()).toHaveAttribute("value", "Software Engineering");
    await department.fill("Brand New Team");
    await expect(department).toHaveValue("Brand New Team");
  });
});
