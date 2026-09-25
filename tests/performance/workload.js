// ADR-007 workload for the equipment-request API (TASK-007).
// Mix per iteration: 25% Employee list, 15% Approver filtered list, 40% detail, 20% full workflow.
// Thresholds are fixed by ADR-007 and must not be changed after results.
//
// Env: BASE_URL (default http://host.docker.internal:8080/api/v1), VUS, DURATION, MODE=load|warmup
import { check, fail, sleep } from "k6";
import crypto from "k6/crypto";
import http from "k6/http";
import { Counter, Rate } from "k6/metrics";

const BASE_URL = __ENV.BASE_URL || "http://host.docker.internal:8080/api/v1";
const MODE = __ENV.MODE || "load";

const unexpectedErrors = new Rate("unexpected_errors");
const expectedConflicts = new Counter("expected_conflicts");

// Mirrors tests/performance/seed-search-dataset.sql.
const SEED_NAMES = ["Somchai", "Suda", "Kanokwan", "Anan", "Pim", "Niran", "Malee", "Thanakorn", "Wipa", "Chai"];
const DETAIL_SEEDS = 200;
const APPROVER = { userId: "approver-001", role: "APPROVER" };
const FILTERS = [
  { status: "PENDING" },
  { status: "APPROVED", keyword: "monitor" },
  { status: "REJECTED", department: "Finance" },
  { keyword: "จอภาพ" },
  { department: "software engineering", status: "DRAFT" },
];

export const options = MODE === "warmup"
  ? { vus: 1, iterations: DETAIL_SEEDS, summaryTrendStats: ["med", "p(95)"] }
  : {
    vus: Number(__ENV.VUS || 1),
    duration: __ENV.DURATION || "30s",
    summaryTrendStats: ["avg", "min", "med", "p(90)", "p(95)", "p(99)", "max"],
    thresholds: {
      http_req_duration: ["p(95)<500", "p(99)<1000"],
      unexpected_errors: ["rate<0.01"],
      checks: ["rate==1.0"],
    },
  };

/** Postgres md5(text)::uuid, so detail IDs match the seed without a lookup. */
function seedRequestId(n) {
  const hex = crypto.md5(`seed-request-${n}`, "hex");
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}

function headers(actor, json = false) {
  const result = { "X-User-Id": actor.userId, "X-Role": actor.role };
  if (json) result["Content-Type"] = "application/json";
  return result;
}

/** Records every response in unexpected_errors unless its status is one the step expects. */
function send(method, path, actor, body, name, expected = [200]) {
  const response = http.request(method, `${BASE_URL}${path}`, body === undefined ? null : JSON.stringify(body), {
    headers: headers(actor, body !== undefined),
    tags: { name },
  });
  unexpectedErrors.add(!expected.includes(response.status), { name });
  return response;
}

function json(response) {
  try {
    return response.json();
  } catch (_) {
    return null;
  }
}

function listEmployee() {
  const k = Math.floor(Math.random() * 10) + 1;
  const actor = { userId: `seed-employee-${String(k).padStart(2, "0")}`, role: "EMPLOYEE" };
  const page = json(send("GET", "/equipment-requests", actor, undefined, "GET list (employee)"));
  check(page, {
    "employee list: first page": (p) => p !== null && p.page === 0,
    "employee list: exactly own 120": (p) => p !== null && p.totalElements === 120,
    "employee list: only own rows": (p) => p !== null && p.content.every((row) => row.employeeName.startsWith(`${SEED_NAMES[k - 1]} Seed`)),
  });
}

function listApprover() {
  const filter = FILTERS[Math.floor(Math.random() * FILTERS.length)];
  const query = Object.entries(filter).map(([key, value]) => `${key}=${encodeURIComponent(value)}`).join("&");
  const page = json(send("GET", `/equipment-requests?${query}`, APPROVER, undefined, "GET list (approver filtered)"));
  check(page, {
    "approver list: status filter holds": (p) => p !== null && (!filter.status || p.content.every((row) => row.status === filter.status)),
    "approver list: department filter holds": (p) => p !== null
      && (!filter.department || p.content.every((row) => row.department.toLowerCase() === filter.department.toLowerCase())),
    "approver list: keyword filter holds": (p) => p !== null && (!filter.keyword || p.content.every((row) =>
      [row.requestNumber, row.title, row.employeeName].some((field) => field.toLowerCase().includes(filter.keyword.toLowerCase())))),
    "approver list: page metadata consistent": (p) => p !== null && p.totalPages === Math.ceil(p.totalElements / p.size),
  });
}

function detail(n) {
  const id = seedRequestId(n);
  const body = json(send("GET", `/equipment-requests/${id}`, APPROVER, undefined, "GET detail"));
  check(body, {
    "detail: correct id": (b) => b !== null && b.id === id,
    "detail: seeded version": (b) => b !== null && b.version === n % 4,
  });
}

function bangkokDate(daysAhead) {
  const shifted = new Date(Date.now() + 7 * 3600 * 1000 + daysAhead * 86400 * 1000);
  return shifted.toISOString().slice(0, 10);
}

function workflow() {
  const writer = { userId: `seed-writer-${String(Math.floor(Math.random() * 10) + 1).padStart(2, "0")}`, role: "EMPLOYEE" };
  const draft = {
    employeeName: "k6 Writer",
    employeeEmail: "k6.writer@example.com",
    department: "Operations",
    title: "k6 workflow request",
    purpose: "Performance workload workflow iteration",
    requiredDate: bangkokDate(30),
    items: [{ equipmentType: "MONITOR", quantity: 1 }],
  };

  const created = json(send("POST", "/equipment-requests", writer, draft, "POST create", [201]));
  if (!check(created, { "workflow: created DRAFT v0": (b) => b !== null && b.status === "DRAFT" && b.version === 0 })) return;
  const path = `/equipment-requests/${created.id}`;

  const edited = json(send("PUT", path, writer, { ...draft, title: "k6 workflow request (edited)", expectedVersion: 0 }, "PUT edit"));
  if (!check(edited, { "workflow: edited v1": (b) => b !== null && b.version === 1 })) return;

  const submitted = json(send("POST", `${path}/submit`, writer, { expectedVersion: 1 }, "POST submit"));
  if (!check(submitted, { "workflow: submitted PENDING v2": (b) => b !== null && b.status === "PENDING" && b.version === 2 })) return;

  const approve = __ITER % 2 === 0;
  const decided = approve
    ? json(send("POST", `${path}/approve`, APPROVER, { expectedVersion: 2 }, "POST approve"))
    : json(send("POST", `${path}/reject`, APPROVER, { expectedVersion: 2, reason: "k6 rejection reason" }, "POST reject"));
  const finalStatus = approve ? "APPROVED" : "REJECTED";
  if (!check(decided, { "workflow: decided v3": (b) => b !== null && b.status === finalStatus && b.version === 3 })) return;

  if (__ITER % 10 === 0) {
    // Deliberate stale action: counted in expected_conflicts, never in unexpected_errors.
    const stale = send("POST", `${path}/approve`, APPROVER, { expectedVersion: 2 }, "POST approve (stale, expected 409)", [409]);
    const conflict = check(json(stale), { "workflow: stale approve is 409 version conflict": (b) => b !== null && b.code === "REQUEST_VERSION_CONFLICT" });
    if (conflict) expectedConflicts.add(1);
  }

  const reread = json(send("GET", path, APPROVER, undefined, "GET detail (workflow)"));
  check(reread, {
    "workflow: detail shows final state": (b) => b !== null && b.status === finalStatus && b.version === 3
      && (approve ? b.rejectionReason === null : b.rejectionReason === "k6 rejection reason"),
  });
}

export default function () {
  if (MODE === "warmup") {
    detail(__ITER + 1); // reads every detail ID once so the Redis cache is warm before the measured run
    return;
  }
  const roll = Math.random();
  if (roll < 0.25) listEmployee();
  else if (roll < 0.40) listApprover();
  else if (roll < 0.80) detail(Math.floor(Math.random() * DETAIL_SEEDS) + 1);
  else workflow();
  sleep(0.1 + Math.random() * 0.4);
}

export function setup() {
  const probe = http.get(`${BASE_URL}/equipment-requests/${seedRequestId(1)}`, { headers: headers(APPROVER) });
  if (probe.status !== 200) fail(`seed request 1 not readable (HTTP ${probe.status}); run seed-search-dataset.sql first`);
}
