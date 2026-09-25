import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { equipmentRequestQueryKey } from "./api";
import { demoActors, IdentityProvider } from "./identity";
import { RequestActions } from "./request-actions";
import type { ApiError, EquipmentRequest, RequestStatus } from "./types";

vi.mock("next/navigation", () => ({ useRouter: () => ({ push: vi.fn() }) }));

const employee = demoActors[0];
const approver = demoActors[2];

function request(status: RequestStatus, version = 3): EquipmentRequest {
  return {
    id: "0e0bda22-525e-4291-bdb2-87444e1c5c60",
    requestNumber: "REQ-2026-000001",
    employeeName: "Somchai Developer",
    employeeEmail: "somchai@example.com",
    department: "Software Engineering",
    title: "Notebook for new project",
    purpose: "Develop the customer onboarding application",
    requiredDate: "2099-10-15",
    additionalNote: null,
    status,
    rejectionReason: null,
    version,
    items: [{ id: "item-1", equipmentType: "NOTEBOOK", quantity: 1, specification: null }],
    totalItems: 1,
    createdAt: "2026-09-25T03:00:00Z",
    updatedAt: "2026-09-25T03:00:00Z",
  };
}

function renderActions(actorId: string, current: EquipmentRequest) {
  window.localStorage.setItem("equipment-request-actor", actorId);
  const client = new QueryClient({ defaultOptions: { mutations: { retry: false } } });
  render(
    <QueryClientProvider client={client}>
      <IdentityProvider>
        <RequestActions request={current} />
      </IdentityProvider>
    </QueryClientProvider>,
  );
  return client;
}

function apiError(status: number, code: string, fieldErrors: Record<string, string> = {}): Response {
  const body: ApiError = { timestamp: "2026-09-25T03:00:00Z", status, code, message: code, path: "/", fieldErrors };
  return { ok: false, status, json: async () => body } as Response;
}

function actionButtons() {
  const group = screen.queryByRole("group", { name: "การดำเนินการกับคำขอ" });
  return group ? within(group).getAllByRole("button").map((button) => button.textContent) : [];
}

describe("RequestActions", () => {
  beforeEach(() => {
    window.localStorage.clear();
    vi.restoreAllMocks();
  });

  afterEach(() => cleanup());

  it.each([
    [employee.userId, "DRAFT", ["ส่งคำขอ", "ยกเลิกคำขอ"]],
    [employee.userId, "PENDING", ["ยกเลิกคำขอ"]],
    [employee.userId, "APPROVED", []],
    [employee.userId, "REJECTED", []],
    [employee.userId, "CANCELLED", []],
    [approver.userId, "DRAFT", []],
    [approver.userId, "PENDING", ["อนุมัติ", "ปฏิเสธ"]],
    [approver.userId, "APPROVED", []],
    [approver.userId, "REJECTED", []],
    [approver.userId, "CANCELLED", []],
  ] as const)("%s sees only the allowed actions for %s", (actorId, status, expected) => {
    renderActions(actorId, request(status));
    expect(actionButtons()).toEqual(expected);
  });

  it("sends the displayed version once, disables every action while pending and stores the result", async () => {
    let resolveFetch!: (value: Response) => void;
    const fetchMock = vi.fn(() => new Promise<Response>((resolve) => { resolveFetch = resolve; }));
    vi.stubGlobal("fetch", fetchMock);
    const client = renderActions(employee.userId, request("DRAFT", 7));

    const submit = screen.getByRole("button", { name: "ส่งคำขอ" });
    fireEvent.click(submit);
    fireEvent.click(submit);

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));
    const [url, init] = fetchMock.mock.calls[0] as unknown as [string, RequestInit];
    expect(url).toMatch(/\/equipment-requests\/.+\/submit$/);
    expect(JSON.parse(init.body as string)).toEqual({ expectedVersion: 7 });
    expect(screen.getByRole("button", { name: "กำลังดำเนินการ…" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "ยกเลิกคำขอ" })).toBeDisabled();

    const pending = { ...request("PENDING", 8) };
    resolveFetch({ ok: true, json: async () => pending } as Response);
    expect(await screen.findByText("ส่งคำขอเพื่อพิจารณาแล้ว")).toBeInTheDocument();
    expect(client.getQueryData(equipmentRequestQueryKey(employee, pending.id))).toEqual(pending);
  });

  it("shows a reload action on 409 without retrying", async () => {
    const fetchMock = vi.fn().mockResolvedValue(apiError(409, "REQUEST_VERSION_CONFLICT"));
    vi.stubGlobal("fetch", fetchMock);
    renderActions(approver.userId, request("PENDING"));

    fireEvent.click(screen.getByRole("button", { name: "อนุมัติ" }));

    expect(await screen.findByRole("button", { name: "โหลดข้อมูลล่าสุด" })).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(screen.getByRole("button", { name: "อนุมัติ" })).toBeEnabled();
  });

  it("explains a 422 business rule failure", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(apiError(422, "ITEMS_REQUIRED", { items: "At least one item" })));
    renderActions(employee.userId, request("DRAFT"));

    fireEvent.click(screen.getByRole("button", { name: "ส่งคำขอ" }));

    expect(await screen.findByText("ต้องมีรายการอุปกรณ์อย่างน้อย 1 รายการก่อนส่งคำขอ")).toBeInTheDocument();
  });

  it("asks for confirmation before cancelling", () => {
    const fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);
    vi.spyOn(window, "confirm").mockReturnValue(false);
    renderActions(employee.userId, request("PENDING"));

    fireEvent.click(screen.getByRole("button", { name: "ยกเลิกคำขอ" }));

    expect(window.confirm).toHaveBeenCalled();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("blocks a blank reject reason, keeps the reason on failure and closes on success", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(apiError(500, "INTERNAL_ERROR"))
      .mockResolvedValueOnce({ ok: true, json: async () => ({ ...request("REJECTED", 4), rejectionReason: "Over budget" }) } as Response);
    vi.stubGlobal("fetch", fetchMock);
    renderActions(approver.userId, request("PENDING"));

    fireEvent.click(screen.getByRole("button", { name: "ปฏิเสธ" }));
    const dialog = screen.getByRole("dialog", { name: "ปฏิเสธคำขอ" });
    const reason = within(dialog).getByLabelText("เหตุผลการปฏิเสธ");
    expect(reason).toHaveFocus();

    fireEvent.change(reason, { target: { value: "   " } });
    fireEvent.click(within(dialog).getByRole("button", { name: "ยืนยันปฏิเสธ" }));
    expect(await within(dialog).findByText("กรุณาระบุเหตุผลการปฏิเสธ")).toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalled();

    fireEvent.change(reason, { target: { value: "  Over budget  " } });
    fireEvent.click(within(dialog).getByRole("button", { name: "ยืนยันปฏิเสธ" }));
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));
    expect(JSON.parse((fetchMock.mock.calls[0][1] as RequestInit).body as string)).toEqual({ expectedVersion: 3, reason: "Over budget" });
    expect(await within(dialog).findByRole("alert")).toBeInTheDocument();
    expect(reason).toHaveValue("  Over budget  ");

    fireEvent.click(within(dialog).getByRole("button", { name: "ยืนยันปฏิเสธ" }));
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    expect(screen.getByText("ปฏิเสธคำขอแล้ว")).toBeInTheDocument();
  });
});
