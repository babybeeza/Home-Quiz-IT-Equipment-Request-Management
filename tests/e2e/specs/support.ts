import { expect, type APIRequestContext, type Page } from "@playwright/test";

export const API_URL = process.env.E2E_API_URL ?? "http://host.docker.internal:8080/api/v1";

export const USERS = {
  somchai: { userId: "employee-001", role: "EMPLOYEE", label: "สมชาย — Employee" },
  somying: { userId: "employee-002", role: "EMPLOYEE", label: "สมหญิง — Employee" },
  approver: { userId: "approver-001", role: "APPROVER", label: "หัวหน้าฝ่าย — Approver" },
} as const;
export type User = (typeof USERS)[keyof typeof USERS];

/** Unique marker so each spec finds only its own requests in a shared database. */
export function tag(prefix: string) {
  return `${prefix}-${Date.now().toString(36)}${Math.floor(Math.random() * 1e4)}`;
}

/** Date in Asia/Bangkok, the business timezone the backend validates against. */
export function bangkokDate(offsetDays = 0) {
  return new Date(Date.now() + 7 * 3_600_000 + offsetDays * 86_400_000).toISOString().slice(0, 10);
}

/**
 * Selects the demo identity the same way the header selector persists it, then reloads.
 * Setting storage avoids racing React hydration; AT-04 exercises the selector itself.
 */
export async function signInAs(page: Page, user: User) {
  await page.goto("/requests");
  await page.evaluate((userId) => window.localStorage.setItem("equipment-request-actor", userId), user.userId);
  await page.reload();
  await expect(page.getByLabel("ผู้ใช้งานตัวอย่าง")).toHaveValue(user.userId);
}

export type Draft = {
  employeeName: string;
  employeeEmail: string;
  department: string;
  title: string;
  purpose: string;
  requiredDate: string;
  additionalNote?: string | null;
  items: { equipmentType: string; quantity: number; specification?: string | null }[];
};

export function draft(title: string, overrides: Partial<Draft> = {}): Draft {
  return {
    employeeName: "Somchai Developer",
    employeeEmail: "somchai@example.com",
    department: "Software Engineering",
    title,
    purpose: "Use for developing the customer onboarding application",
    requiredDate: bangkokDate(30),
    items: [{ equipmentType: "NOTEBOOK", quantity: 1, specification: "16 GB RAM" }],
    ...overrides,
  };
}

function headers(user: User) {
  return { "X-User-Id": user.userId, "X-Role": user.role, "Content-Type": "application/json" };
}

/** Thin API client used to arrange preconditions quickly; assertions go through the UI unless the case is API-level. */
export function api(request: APIRequestContext) {
  const call = async (method: string, path: string, user: User, data?: unknown) =>
    request.fetch(`${API_URL}${path}`, { method, headers: headers(user), data: data === undefined ? undefined : JSON.stringify(data) });
  const ok = async (method: string, path: string, user: User, data?: unknown) => {
    const response = await call(method, path, user, data);
    expect(response.ok(), `${method} ${path} → ${response.status()} ${await response.text()}`).toBeTruthy();
    return response.json();
  };
  return {
    call,
    create: (user: User, body: Draft) => ok("POST", "/equipment-requests", user, body),
    get: (user: User, id: string) => ok("GET", `/equipment-requests/${id}`, user),
    update: (user: User, id: string, body: Draft, expectedVersion: number) =>
      ok("PUT", `/equipment-requests/${id}`, user, { ...body, expectedVersion }),
    action: (user: User, id: string, action: string, expectedVersion: number, reason?: string) =>
      ok("POST", `/equipment-requests/${id}/${action}`, user, reason === undefined ? { expectedVersion } : { expectedVersion, reason }),
    list: (user: User, query = "") => ok("GET", `/equipment-requests${query}`, user),
  };
}

/** Creates a request owned by `owner` and drives it to the requested status through the real API. */
export async function arrange(request: APIRequestContext, status: "DRAFT" | "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED",
  title: string, owner: User = USERS.somchai, overrides: Partial<Draft> = {}) {
  const client = api(request);
  let current = await client.create(owner, draft(title, overrides));
  if (status === "CANCELLED") return client.action(owner, current.id, "cancel", current.version);
  if (status === "DRAFT") return current;
  current = await client.action(owner, current.id, "submit", current.version);
  if (status === "APPROVED") return client.action(USERS.approver, current.id, "approve", current.version);
  if (status === "REJECTED") return client.action(USERS.approver, current.id, "reject", current.version, "E2E rejection reason");
  return current;
}

export async function fillValidForm(page: Page, title: string) {
  await page.getByLabel("ชื่อพนักงาน").fill("Somchai Developer");
  await page.getByLabel("อีเมล").fill("somchai@example.com");
  await page.getByLabel("แผนก").fill("Software Engineering");
  await page.getByLabel("หัวข้อคำขอ").fill(title);
  await page.getByLabel("วัตถุประสงค์").fill("Use for developing the customer onboarding application");
  await page.getByLabel("วันที่ต้องการใช้").fill(bangkokDate(30));
}

export const UUID_PATH = /\/requests\/[0-9a-f-]{36}$/;
