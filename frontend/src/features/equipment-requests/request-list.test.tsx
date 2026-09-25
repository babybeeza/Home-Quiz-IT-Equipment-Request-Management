import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, cleanup, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { IdentityProvider } from "./identity";
import { RequestList } from "./request-list";
import { stubApi } from "./test-api";
import type { EquipmentRequestPage, EquipmentRequestSummary } from "./types";

// In-memory URL so router.push re-renders useSearchParams consumers like Next.js does.
const nav = vi.hoisted(() => ({
  search: new URLSearchParams(),
  listeners: new Set<() => void>(),
  push: vi.fn(),
}));

vi.mock("next/navigation", async () => {
  const { useSyncExternalStore } = await vi.importActual<typeof import("react")>("react");
  return {
    usePathname: () => "/requests",
    useRouter: () => ({ push: nav.push }),
    useSearchParams: () => useSyncExternalStore(
      (listener: () => void) => { nav.listeners.add(listener); return () => { nav.listeners.delete(listener); }; },
      () => nav.search,
    ),
  };
});

function navigate(query: string) {
  nav.search = new URLSearchParams(query);
  nav.listeners.forEach((listener) => listener());
}

function row(overrides: Partial<EquipmentRequestSummary> = {}): EquipmentRequestSummary {
  return {
    id: "0e0bda22-525e-4291-bdb2-87444e1c5c60",
    requestNumber: "REQ-2026-000001",
    title: "Notebook for new project",
    employeeName: "Somchai Developer",
    department: "Software Engineering",
    requiredDate: "2099-10-15",
    totalItems: 3,
    status: "PENDING",
    version: 1,
    createdAt: "2026-09-25T03:00:00Z",
    updatedAt: "2026-09-25T03:00:00Z",
    ...overrides,
  };
}

function page(content: EquipmentRequestSummary[], extra: Partial<EquipmentRequestPage> = {}): EquipmentRequestPage {
  return { content, page: 0, size: 10, totalElements: content.length, totalPages: content.length ? 1 : 0, ...extra };
}

function respond(body: EquipmentRequestPage) {
  return Promise.resolve({ ok: true, json: async () => body } as Response);
}

function requestedUrls(fetchMock: ReturnType<typeof vi.fn>) {
  return fetchMock.mock.calls.map(([url]) => new URL(url as string));
}

function renderList() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <IdentityProvider>
        <RequestList />
      </IdentityProvider>
    </QueryClientProvider>,
  );
}

describe("RequestList", () => {
  beforeEach(() => {
    window.localStorage.clear();
    vi.restoreAllMocks();
    navigate("");
    nav.push.mockReset();
    nav.push.mockImplementation((href: string) => act(() => navigate(href.split("?")[1] ?? "")));
  });

  afterEach(() => cleanup());

  it("renders every list column, a detail link and pagination bounds", async () => {
    stubApi(vi.fn(() => respond(page([row()], { totalElements: 25, totalPages: 3 }))));
    renderList();

    expect(screen.getByRole("status")).toHaveTextContent("กำลังโหลดรายการ…");
    const table = await screen.findByRole("table");
    ["เลขที่คำขอ", "หัวข้อ", "ผู้ขอ", "แผนก", "วันที่ต้องการใช้", "จำนวนรวม", "สถานะ", "สร้างเมื่อ"].forEach((name) =>
      expect(within(table).getByRole("columnheader", { name })).toBeInTheDocument());
    const cells = within(table).getAllByRole("cell").map((cell) => cell.textContent);
    expect(cells.slice(0, 7)).toEqual(["REQ-2026-000001", "Notebook for new project", "Somchai Developer", "Software Engineering", "2099-10-15", "3", "PENDING"]);
    expect(within(table).getByRole("link", { name: "REQ-2026-000001" })).toHaveAttribute("href", "/requests/0e0bda22-525e-4291-bdb2-87444e1c5c60");
    expect(screen.getByText("หน้า 1 จาก 3 · ทั้งหมด 25 รายการ")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "ก่อนหน้า" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "ถัดไป" })).toBeEnabled();
  });

  it("restores every parameter from the URL and falls back to defaults for invalid values", async () => {
    const fetchMock = vi.fn(() => respond(page([row()])));
    stubApi(fetchMock);
    navigate("keyword=mon&status=PENDING&department=Finance&page=2&sort=createdAt,asc");
    renderList();

    await waitFor(() => expect(fetchMock).toHaveBeenCalled());
    const url = requestedUrls(fetchMock)[0];
    expect(Object.fromEntries(url.searchParams)).toEqual({
      keyword: "mon", status: "PENDING", department: "Finance", page: "2", sort: "createdAt,asc",
    });
    expect(screen.getByRole("searchbox")).toHaveValue("mon");
    expect(screen.getByLabelText("สถานะ")).toHaveValue("PENDING");

    cleanup();
    fetchMock.mockClear();
    navigate("page=-3&status=OPEN&sort=title,asc");
    renderList();
    await waitFor(() => expect(fetchMock).toHaveBeenCalled());
    expect(requestedUrls(fetchMock)[0].search).toBe("");
  });

  it("returns to the first page when a filter changes and keeps filters while paging", async () => {
    stubApi(vi.fn(() => respond(page([row()], { page: 2, totalElements: 40, totalPages: 4 }))));
    navigate("status=PENDING&page=2");
    renderList();
    await screen.findByRole("table");

    fireEvent.click(screen.getByRole("button", { name: "ถัดไป" }));
    expect(nav.push).toHaveBeenLastCalledWith("/requests?status=PENDING&page=3", { scroll: false });

    fireEvent.change(screen.getByLabelText("สถานะ"), { target: { value: "APPROVED" } });
    expect(nav.push).toHaveBeenLastCalledWith("/requests?status=APPROVED", { scroll: false });
  });

  it("debounces the keyword into one request and cancels the timer on unmount", async () => {
    const fetchMock = vi.fn(() => respond(page([row()])));
    stubApi(fetchMock);
    const view = renderList();
    await screen.findByRole("table");

    const search = screen.getByRole("searchbox");
    ["m", "mo", "mon"].forEach((value) => fireEvent.change(search, { target: { value } }));
    expect(nav.push).not.toHaveBeenCalled();

    await waitFor(() => expect(nav.push).toHaveBeenCalledTimes(1));
    expect(nav.push).toHaveBeenCalledWith("/requests?keyword=mon", { scroll: false });
    await waitFor(() => expect(requestedUrls(fetchMock).filter((url) => url.searchParams.get("keyword") === "mon")).toHaveLength(1));
    expect(requestedUrls(fetchMock).some((url) => ["m", "mo"].includes(url.searchParams.get("keyword") ?? ""))).toBe(false);

    fireEvent.change(search, { target: { value: "monitor" } });
    view.unmount();
    await new Promise((resolve) => setTimeout(resolve, 400));
    expect(nav.push).toHaveBeenCalledTimes(1);
  });

  it("never lets a slow response for an old keyword replace newer results", async () => {
    let resolveOld!: () => void;
    const fetchMock = vi.fn((url: string) => {
      const keyword = new URL(url).searchParams.get("keyword");
      if (keyword === "old") {
        return new Promise<Response>((resolve) => {
          resolveOld = () => resolve({ ok: true, json: async () => page([row({ id: "old-id", title: "Old result" })]) } as Response);
        });
      }
      return respond(page([row({ id: "new-id", title: keyword === "new" ? "New result" : "Initial" })]));
    });
    stubApi(fetchMock);
    navigate("keyword=old");
    renderList();
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));

    act(() => navigate("keyword=new"));
    expect(await screen.findByText("New result")).toBeInTheDocument();
    await act(async () => { resolveOld(); });

    expect(screen.getByText("New result")).toBeInTheDocument();
    expect(screen.queryByText("Old result")).not.toBeInTheDocument();
  });

  it("offers to clear filters when nothing matches and shows an accessible error", async () => {
    const fetchMock = vi.fn(() => respond(page([])));
    stubApi(fetchMock);
    navigate("keyword=zzz&status=DRAFT");
    renderList();

    expect(await screen.findByText("ไม่พบคำขอที่ตรงกับเงื่อนไข")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "ล้างตัวกรอง" }));
    expect(nav.push).toHaveBeenLastCalledWith("/requests", { scroll: false });

    cleanup();
    stubApi(vi.fn().mockResolvedValue({ ok: false, status: 500, json: async () => ({}) } as Response));
    renderList();
    expect(await screen.findByRole("alert")).toHaveTextContent("โหลดรายการไม่สำเร็จ");
    expect(screen.getByRole("button", { name: "ลองใหม่" })).toBeInTheDocument();
  });
});
