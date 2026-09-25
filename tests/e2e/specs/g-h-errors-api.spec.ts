import { expect, test } from "@playwright/test";
import { api, API_URL, arrange, bangkokDate, draft, signInAs, tag, USERS } from "./support";

test.describe("G. States and error handling", () => {
  test("AT-45 an unknown request shows not found", async ({ page }) => {
    await signInAs(page, USERS.somchai);
    await page.goto("/requests/00000000-0000-0000-0000-000000000000");
    // Scoped to main: Next.js also renders an empty role=alert route announcer.
    await expect(page.getByRole("main").getByRole("alert")).toHaveText("ไม่พบคำขอ");
  });
});

test.describe("H. Rules the backend enforces even when the UI hides them", () => {
  const ENVELOPE = ["timestamp", "status", "code", "message", "path", "fieldErrors"];

  async function expectError(response: import("@playwright/test").APIResponse, status: number, code: string) {
    expect(response.status()).toBe(status);
    const body = await response.json();
    expect(Object.keys(body).sort()).toEqual([...ENVELOPE].sort());
    expect(body.code).toBe(code);
    return body;
  }

  test("AT-47 an Employee cannot approve, even their own request", async ({ request }) => {
    const pending = await arrange(request, "PENDING", `${tag("at47")} self approve`);
    const response = await api(request).call("POST", `/equipment-requests/${pending.id}/approve`, USERS.somchai, { expectedVersion: pending.version });

    await expectError(response, 403, "ACCESS_DENIED");
    expect((await api(request).get(USERS.somchai, pending.id)).status).toBe("PENDING");
  });

  test("AT-48 editing a non-DRAFT request is a state conflict", async ({ request }) => {
    const pending = await arrange(request, "PENDING", `${tag("at48")} not draft`);
    const response = await api(request).call("PUT", `/equipment-requests/${pending.id}`, USERS.somchai,
      { ...draft(pending.title), expectedVersion: pending.version });
    await expectError(response, 409, "REQUEST_STATE_CONFLICT");
  });

  test("AT-49 a stale version is rejected and the latest data is not overwritten", async ({ request }) => {
    const created = await arrange(request, "DRAFT", `${tag("at49")} latest`);
    const updated = await api(request).update(USERS.somchai, created.id, draft(`${created.title} v1`), 0);
    const response = await api(request).call("PUT", `/equipment-requests/${created.id}`, USERS.somchai,
      { ...draft(`${created.title} stale overwrite`), expectedVersion: 0 });

    const body = await expectError(response, 409, "REQUEST_VERSION_CONFLICT");
    expect(body.message).toBe("This request has been updated by another user");
    const latest = await api(request).get(USERS.somchai, created.id);
    expect(latest.title).toBe(updated.title);
    expect(latest.version).toBe(1);
  });

  test("AT-50 past date and out-of-range quantity are field validation errors and create nothing", async ({ request }) => {
    const marker = tag("at50");
    const pastDate = await api(request).call("POST", "/equipment-requests", USERS.somchai, draft(`${marker} past`, { requiredDate: bangkokDate(-1) }));
    expect((await expectError(pastDate, 400, "VALIDATION_ERROR")).fieldErrors).toHaveProperty("requiredDate");

    const tooMany = await api(request).call("POST", "/equipment-requests", USERS.somchai,
      draft(`${marker} quantity`, { items: [{ equipmentType: "NOTEBOOK", quantity: 9 }] }));
    expect((await expectError(tooMany, 400, "VALIDATION_ERROR")).fieldErrors).toHaveProperty(["items[0].quantity"]);

    expect((await api(request).list(USERS.somchai, `?keyword=${marker}`)).totalElements).toBe(0);
  });

  test("AT-51 approving an already approved request is refused", async ({ request }) => {
    const approved = await arrange(request, "APPROVED", `${tag("at51")} twice`);
    const response = await api(request).call("POST", `/equipment-requests/${approved.id}/approve`, USERS.approver, { expectedVersion: approved.version });
    await expectError(response, 409, "REQUEST_STATE_CONFLICT");
  });

  test("AT-52 reject without a reason is a business-rule error", async ({ request }) => {
    const pending = await arrange(request, "PENDING", `${tag("at52")} no reason`);
    const response = await api(request).call("POST", `/equipment-requests/${pending.id}/reject`, USERS.approver, { expectedVersion: pending.version });
    await expectError(response, 422, "REJECTION_REASON_REQUIRED");
  });

  test("AT-53 a status sent by the client is ignored", async ({ request }) => {
    const created = await api(request).create(USERS.somchai, { ...draft(`${tag("at53")} forged`), status: "APPROVED" } as never);
    expect(created.status).toBe("DRAFT");

    const edited = await api(request).call("PUT", `/equipment-requests/${created.id}`, USERS.somchai,
      { ...draft(created.title), status: "APPROVED", expectedVersion: created.version });
    expect(edited.status()).toBe(200);
    expect((await edited.json()).status).toBe("DRAFT");
  });

  test("AT-54 an Employee list contains only their own requests", async ({ request }) => {
    const marker = tag("at54");
    await arrange(request, "DRAFT", `${marker} somchai`, USERS.somchai);
    await arrange(request, "DRAFT", `${marker} somying`, USERS.somying);

    const page = await api(request).list(USERS.somying, `?keyword=${marker}`);
    expect(page.totalElements).toBe(1);
    expect(page.content[0].title).toBe(`${marker} somying`);
  });

  test("AT-55 missing or unknown identity is a malformed request", async ({ request }) => {
    const noUser = await request.get(`${API_URL}/equipment-requests`, { headers: { "X-Role": "EMPLOYEE" } });
    await expectError(noUser, 400, "MALFORMED_REQUEST");
    const badRole = await request.get(`${API_URL}/equipment-requests`, { headers: { "X-User-Id": "employee-001", "X-Role": "ADMIN" } });
    await expectError(badRole, 400, "MALFORMED_REQUEST");
  });
});
