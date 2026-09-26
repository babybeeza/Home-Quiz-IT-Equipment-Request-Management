import { expect, test, type Page } from "@playwright/test";
import { api, arrange, signInAs, tag, USERS } from "./support";

const rows = (page: Page) => page.locator(".request-table tbody tr");
const pager = (page: Page) => page.getByRole("navigation", { name: "เปลี่ยนหน้า" });

/**
 * Performs `action`, waits for the list response whose query matches, then for the previous page's
 * placeholder rows ("กำลังโหลด…", kept on screen by design while paging) to be replaced.
 */
async function listUpdate(page: Page, matches: (query: URLSearchParams) => boolean, action: () => Promise<unknown>) {
  const response = page.waitForResponse((received) => {
    const url = new URL(received.url());
    return received.request().method() === "GET" && url.pathname.endsWith("/equipment-requests") && matches(url.searchParams);
  });
  await action();
  await response;
  await expect(page.locator(".loading-note")).toHaveCount(0);
}

test.describe("E. List page", () => {
  test("AT-31 AT-32 the list shows all eight data columns and links each row to its detail", async ({ page, request }) => {
    const created = await arrange(request, "PENDING", `${tag("at31")} columns`, USERS.somchai, {
      items: [{ equipmentType: "MONITOR", quantity: 2 }, { equipmentType: "MOUSE", quantity: 1 }],
    });
    await signInAs(page, USERS.somchai);
    await page.getByRole("searchbox").fill(created.title);

    for (const name of ["เลขที่คำขอ", "หัวข้อ", "ผู้ขอ", "แผนก", "วันที่ต้องการใช้", "จำนวนรวม", "สถานะ", "สร้างเมื่อ"]) {
      await expect(page.getByRole("columnheader", { name })).toBeVisible();
    }
    const row = rows(page).filter({ hasText: created.title });
    await expect(row.getByRole("cell")).toHaveText([
      created.requestNumber, created.title, created.employeeName, created.department,
      created.requiredDate, "3", "PENDING", /.+/, /ยกเลิกคำขอ/,
    ]);

    await row.getByRole("link", { name: created.requestNumber }).click();
    await expect(page).toHaveURL(new RegExp(`/requests/${created.id}$`));
  });

  test("AT-33 list rows expose role- and status-specific actions and submit from the list", async ({ page, request }) => {
    const marker = tag("at33");
    const created = await arrange(request, "DRAFT", `${marker} row actions`);
    await arrange(request, "APPROVED", `${marker} terminal`);
    await signInAs(page, USERS.somchai);
    await page.getByRole("searchbox").fill(marker);

    const row = rows(page).filter({ hasText: created.title });
    await expect(row).toHaveCount(1);
    await expect(row.getByRole("link", { name: "แก้ไข Draft" })).toBeVisible();
    await expect(row.getByRole("button", { name: "ส่งคำขอ" })).toBeVisible();
    await expect(row.getByRole("button", { name: "ยกเลิกคำขอ" })).toBeVisible();
    await expect(rows(page).filter({ hasText: `${marker} terminal` }).getByRole("button")).toHaveCount(0);

    await row.getByRole("button", { name: "ส่งคำขอ" }).click();
    await expect(row.getByRole("cell", { name: "PENDING" })).toBeVisible();
    await expect(row.getByRole("button", { name: "ส่งคำขอ" })).toHaveCount(0);
    await expect(row.getByRole("button", { name: "ยกเลิกคำขอ" })).toBeVisible();
    expect((await api(request).get(USERS.somchai, created.id)).status).toBe("PENDING");

    await signInAs(page, USERS.approver);
    await page.getByRole("searchbox").fill(created.title);
    const approverRow = rows(page).filter({ hasText: created.title });
    await expect(approverRow.getByRole("button", { name: "อนุมัติ" })).toBeVisible();
    await expect(approverRow.getByRole("button", { name: "ปฏิเสธ" })).toBeVisible();
    await expect(approverRow.getByRole("button", { name: "ยกเลิกคำขอ" })).toHaveCount(0);
  });

  test("AT-33 a stale list action cannot overwrite an Employee cancellation", async ({ page, request }) => {
    const created = await arrange(request, "PENDING", `${tag("at33-stale")} conflict`);
    await signInAs(page, USERS.approver);
    await page.getByRole("searchbox").fill(created.title);
    const row = rows(page).filter({ hasText: created.title });
    await expect(row.getByRole("button", { name: "อนุมัติ" })).toBeVisible();

    await api(request).action(USERS.somchai, created.id, "cancel", created.version);
    await row.getByRole("button", { name: "อนุมัติ" }).click();
    await expect(row.getByRole("alert")).toContainText("คำขอนี้ถูกเปลี่ยนจากที่อื่นแล้ว");
    expect((await api(request).get(USERS.approver, created.id)).status).toBe("CANCELLED");

    await row.getByRole("button", { name: "โหลดข้อมูลล่าสุด" }).click();
    await expect(row.getByRole("cell", { name: "CANCELLED" })).toBeVisible();
    await expect(row.getByRole("button", { name: "อนุมัติ" })).toHaveCount(0);
  });
});

test.describe("F. Search, filter and pagination (seed dataset)", () => {
  test.beforeAll(async ({ request }) => {
    const seeded = await api(request).list(USERS.approver, "?keyword=REQ-2026-900777");
    test.skip(seeded.totalElements !== 1, "Seed dataset missing: run tests/performance/seed-search-dataset.sql first");
  });

  test.beforeEach(async ({ page }) => {
    await signInAs(page, USERS.approver);
  });

  test("AT-34 search by part of a request number", async ({ page }) => {
    await page.getByRole("searchbox").fill("900777");
    await expect(rows(page)).toHaveCount(1);
    await expect(rows(page).first().getByRole("link")).toHaveText("REQ-2026-900777");
  });

  test("AT-35 search ignores case and matches title or employee name", async ({ page }) => {
    for (const [keyword, column] of [["MONITOR", 1], ["ergonomic", 1], ["kanokwan", 2]] as const) {
      await listUpdate(page, (query) => query.get("keyword") === keyword, () => page.getByRole("searchbox").fill(keyword));
      await expect(rows(page).first()).toBeVisible();
      for (const cell of await rows(page).locator(`td:nth-child(${column + 1})`).allTextContents()) {
        expect(cell.toLowerCase()).toContain(keyword.toLowerCase());
      }
    }
  });

  test("AT-36 keyword, status and department filter together", async ({ page }) => {
    await page.getByRole("searchbox").fill("monitor");
    await expect(page).toHaveURL(/keyword=monitor/);
    await page.getByLabel("สถานะ").selectOption("PENDING");
    await listUpdate(page, (query) => query.get("department") === "software engineering" && query.get("status") === "PENDING",
      () => page.getByLabel("แผนก").fill("software engineering"));

    await expect(rows(page).first()).toBeVisible();
    const cells = await rows(page).evaluateAll((trs) => trs.map((tr) => [...tr.querySelectorAll("td")].map((td) => td.textContent ?? "")));
    for (const cell of cells) {
      expect(cell[1].toLowerCase()).toContain("monitor");
      expect(cell[3]).toBe("Software Engineering");
      expect(cell[6]).toBe("PENDING");
    }
    await expect(pager(page)).toContainText(`ทั้งหมด ${cells.length} รายการ`);
  });

  test("AT-37 no match shows the empty state and clearing filters restores the list", async ({ page }) => {
    await page.getByRole("searchbox").fill("zzzz-no-such-request");
    await expect(page.getByText("ไม่พบคำขอที่ตรงกับเงื่อนไข")).toBeVisible();

    await page.getByRole("button", { name: "ล้างตัวกรอง" }).click();
    await expect(page).toHaveURL(/\/requests$/);
    await expect(page.getByRole("searchbox")).toHaveValue("");
    await expect(rows(page)).toHaveCount(10);
  });

  test("AT-38 pagination shows correct totals and disables buttons at the ends", async ({ page, request }) => {
    const total = (await api(request).list(USERS.approver)).totalElements as number;
    const pages = Math.ceil(total / 10);

    await expect(pager(page)).toContainText(`หน้า 1 จาก ${pages} · ทั้งหมด ${total} รายการ`);
    await expect(pager(page).getByRole("button", { name: "ก่อนหน้า" })).toBeDisabled();
    await pager(page).getByRole("button", { name: "ถัดไป" }).click();
    await expect(pager(page)).toContainText(`หน้า 2 จาก ${pages}`);
    await pager(page).getByRole("button", { name: "ก่อนหน้า" }).click();
    await expect(pager(page)).toContainText(`หน้า 1 จาก ${pages}`);

    await page.goto(`/requests?page=${pages - 1}`);
    await expect(pager(page)).toContainText(`หน้า ${pages} จาก ${pages}`);
    await expect(pager(page).getByRole("button", { name: "ถัดไป" })).toBeDisabled();
  });

  test("AT-39 sorting by created date works both ways without duplicates across pages", async ({ page }) => {
    await page.getByLabel("เรียงตามวันที่สร้าง").selectOption("createdAt,asc");
    await expect(page).toHaveURL(/sort=createdAt%2Casc|sort=createdAt,asc/);
    await expect(rows(page).first().getByRole("link")).toHaveText("REQ-2026-900001");

    const seen: string[] = [];
    for (let pageIndex = 0; pageIndex < 3; pageIndex += 1) {
      seen.push(...await rows(page).locator("td:first-child").allTextContents());
      if (pageIndex < 2) {
        await listUpdate(page, (query) => query.get("page") === String(pageIndex + 1),
          () => pager(page).getByRole("button", { name: "ถัดไป" }).click());
        await expect(pager(page)).toContainText(`หน้า ${pageIndex + 2} `);
      }
    }
    expect(new Set(seen).size).toBe(30);
    expect(seen.slice(0, 3)).toEqual(["REQ-2026-900001", "REQ-2026-900002", "REQ-2026-900003"]);

    await page.getByLabel("เรียงตามวันที่สร้าง").selectOption("createdAt,desc");
    await expect(rows(page).first().getByRole("link")).not.toHaveText("REQ-2026-900001");
  });

  test("AT-40 changing a filter returns to the first page", async ({ page }) => {
    await page.goto("/requests?page=2");
    await expect(pager(page)).toContainText("หน้า 3 ");

    await page.getByLabel("สถานะ").selectOption("APPROVED");

    await expect(page).toHaveURL(/\/requests\?status=APPROVED$/);
    await expect(pager(page)).toContainText("หน้า 1 ");
  });

  test("AT-41 filters live in the URL: new tab, back/forward and refresh restore them", async ({ page, context }) => {
    await page.getByRole("searchbox").fill("monitor");
    await expect(page).toHaveURL(/keyword=monitor/);
    await listUpdate(page, (query) => query.get("keyword") === "monitor" && query.get("status") === "PENDING",
      () => page.getByLabel("สถานะ").selectOption("PENDING"));
    await expect(page).toHaveURL(/keyword=monitor&status=PENDING/);
    const firstRow = await rows(page).first().getByRole("link").textContent();

    const copy = await context.newPage();
    await copy.goto(page.url());
    await expect(copy.getByRole("searchbox")).toHaveValue("monitor");
    await expect(copy.getByLabel("สถานะ")).toHaveValue("PENDING");
    await expect(rows(copy).first().getByRole("link")).toHaveText(firstRow ?? "");

    await page.goBack();
    await expect(page.getByLabel("สถานะ")).toHaveValue("");
    await expect(page.getByRole("searchbox")).toHaveValue("monitor");
    await page.goForward();
    await expect(page.getByLabel("สถานะ")).toHaveValue("PENDING");

    await page.reload();
    await expect(page.getByRole("searchbox")).toHaveValue("monitor");
    await expect(page.getByLabel("สถานะ")).toHaveValue("PENDING");
  });

  test("AT-42 fast typing sends one search for the final keyword", async ({ page }) => {
    const keywords: string[] = [];
    page.on("request", (sent) => {
      const url = new URL(sent.url());
      if (url.pathname.endsWith("/equipment-requests") && url.searchParams.has("keyword")) keywords.push(url.searchParams.get("keyword") ?? "");
    });

    await page.getByRole("searchbox").pressSequentially("ergo", { delay: 60 });
    await expect(page).toHaveURL(/keyword=ergo/);
    await expect(rows(page).first()).toContainText(/ergonomic/i);

    expect(keywords).toEqual(["ergo"]);
  });

  test("AT-43 an Employee cannot find other users' requests through search", async ({ page }) => {
    await signInAs(page, USERS.somchai);
    await page.getByRole("searchbox").fill("900777");
    await expect(page.getByText("ไม่พบคำขอที่ตรงกับเงื่อนไข")).toBeVisible();
  });
});
