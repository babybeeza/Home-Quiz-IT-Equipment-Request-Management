import { expect, test } from "@playwright/test";
import { arrange, signInAs, tag, USERS } from "./support";

test.describe("A. Identity and roles", () => {
  test("AT-01 Employee sees the create button and only their own requests", async ({ page, request }) => {
    const marker = tag("at01");
    await arrange(request, "DRAFT", `${marker} own`, USERS.somchai);
    await arrange(request, "DRAFT", `${marker} other`, USERS.somying);

    await signInAs(page, USERS.somchai);
    await expect(page.getByRole("link", { name: "สร้างคำขอใหม่" })).toBeVisible();
    await page.getByRole("searchbox").fill(marker);

    await expect(page.getByRole("cell", { name: `${marker} own` })).toBeVisible();
    await expect(page.getByRole("cell", { name: `${marker} other` })).toHaveCount(0);
  });

  test("AT-02 Approver has no create button and sees every user's requests", async ({ page, request }) => {
    const marker = tag("at02");
    await arrange(request, "DRAFT", `${marker} somchai`, USERS.somchai);
    await arrange(request, "DRAFT", `${marker} somying`, USERS.somying);

    await signInAs(page, USERS.approver);
    await expect(page.getByRole("link", { name: "สร้างคำขอใหม่" })).toHaveCount(0);
    await page.getByRole("searchbox").fill(marker);

    await expect(page.getByRole("cell", { name: `${marker} somchai` })).toBeVisible();
    await expect(page.getByRole("cell", { name: `${marker} somying` })).toBeVisible();
  });

  test("AT-03 another Employee cannot open someone else's request by URL", async ({ page, request }) => {
    const owned = await arrange(request, "DRAFT", `${tag("at03")} private`, USERS.somchai);

    await signInAs(page, USERS.somying);
    await page.goto(`/requests/${owned.id}`);

    await expect(page.getByRole("main").getByRole("alert")).toHaveText("คุณไม่มีสิทธิ์ดูคำขอนี้");
    await expect(page.getByText(owned.title)).toHaveCount(0);
  });

  test("AT-04 switching user from a detail page returns to the list without stale data", async ({ page, request }) => {
    const owned = await arrange(request, "DRAFT", `${tag("at04")} somchai only`, USERS.somchai);
    await signInAs(page, USERS.somchai);
    await page.goto(`/requests/${owned.id}`);
    await expect(page.getByRole("heading", { name: owned.title })).toBeVisible();

    await page.getByLabel("ผู้ใช้งานตัวอย่าง").selectOption(USERS.somying.userId);

    await page.waitForURL(/\/requests$/);
    await page.getByRole("searchbox").fill(owned.title);
    await expect(page.getByText("ไม่พบคำขอที่ตรงกับเงื่อนไข")).toBeVisible();
  });
});
