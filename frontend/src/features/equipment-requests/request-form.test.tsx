import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { equipmentRequestQueryKey } from "./api";
import { demoActors, IdentityProvider } from "./identity";
import { stubApi, testReferenceData } from "./test-api";
import { RequestForm } from "./request-form";
import type { ApiError, EquipmentRequest } from "./types";

const push = vi.fn();
vi.mock("next/navigation", () => ({ useRouter: () => ({ push }) }));

const savedRequest: EquipmentRequest = {
  id: "0e0bda22-525e-4291-bdb2-87444e1c5c60",
  requestNumber: "REQ-2026-000001",
  employeeName: "Somchai Developer",
  employeeEmail: "somchai@example.com",
  department: "Software Engineering",
  title: "Notebook for new project",
  purpose: "Develop the customer onboarding application",
  requiredDate: "2099-10-15",
  additionalNote: null,
  status: "DRAFT",
  rejectionReason: null,
  version: 0,
  items: [],
  totalItems: 0,
  createdAt: "2026-09-25T03:00:00Z",
  updatedAt: "2026-09-25T03:00:00Z",
};

function renderForm(props: { mode?: "create" | "edit"; request?: EquipmentRequest } = {}) {
  const client = new QueryClient({ defaultOptions: { mutations: { retry: false } } });
  const tree = (request?: EquipmentRequest) => (
    <QueryClientProvider client={client}>
      <IdentityProvider>
        <RequestForm mode={props.mode ?? "create"} request={request} />
      </IdentityProvider>
    </QueryClientProvider>
  );
  const view = render(tree(props.request));
  return { ...view, client, rerenderWith: (request: EquipmentRequest) => view.rerender(tree(request)) };
}

function fillValidForm() {
  fireEvent.change(screen.getByLabelText("ชื่อพนักงาน"), { target: { value: "Somchai Developer" } });
  fireEvent.change(screen.getByLabelText("อีเมล"), { target: { value: "somchai@example.com" } });
  fireEvent.change(screen.getByLabelText("แผนก"), { target: { value: "Software Engineering" } });
  fireEvent.change(screen.getByLabelText("หัวข้อคำขอ"), { target: { value: "Notebook for new project" } });
  fireEvent.change(screen.getByLabelText("วัตถุประสงค์"), { target: { value: "Develop the customer onboarding application" } });
  fireEvent.change(screen.getByLabelText("วันที่ต้องการใช้"), { target: { value: "2099-10-15" } });
}

describe("RequestForm", () => {
  beforeEach(() => {
    window.localStorage.clear();
    push.mockReset();
    vi.restoreAllMocks();
    stubApi(vi.fn()); // tests that call the request API install their own handler
  });

  afterEach(() => cleanup());

  it("suggests departments from reference data while keeping the field free text", async () => {
    renderForm();
    const department = screen.getByLabelText("แผนก");
    const listId = department.getAttribute("list");

    await waitFor(() => {
      const options = [...document.querySelectorAll(`datalist[id="${listId}"] option`)].map((option) => option.getAttribute("value"));
      expect(options).toEqual(testReferenceData.departments.map((item) => item.name));
    });
    fireEvent.change(department, { target: { value: "Brand New Team" } });
    expect(department).toHaveValue("Brand New Team");
  });

  it("stays usable without suggestions when reference data fails", async () => {
    const reference = vi.fn(() => Promise.resolve({ ok: false, status: 500, json: async () => ({}) } as Response));
    stubApi(vi.fn(), reference);
    renderForm();

    await waitFor(() => expect(reference).toHaveBeenCalled());
    const department = screen.getByLabelText("แผนก");
    expect(document.querySelectorAll(`datalist[id="${department.getAttribute("list")}"] option`)).toHaveLength(0);
    fireEvent.change(department, { target: { value: "Finance" } });
    expect(department).toHaveValue("Finance");
  });

  it("adds and removes dynamic item rows and protects a dirty form", async () => {
    const addListener = vi.spyOn(window, "addEventListener");
    renderForm();
    expect(screen.getByText("Draft สามารถบันทึกโดยยังไม่มีรายการอุปกรณ์ได้")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "+ เพิ่มรายการ" }));
    expect(screen.getByLabelText("ประเภท")).toBeInTheDocument();
    expect(screen.getByLabelText("จำนวน")).toHaveValue(1);
    await waitFor(() => expect(addListener).toHaveBeenCalledWith("beforeunload", expect.any(Function)));

    fireEvent.click(screen.getByRole("button", { name: "ลบ" }));
    expect(screen.queryByLabelText("ประเภท")).not.toBeInTheDocument();
  });

  it("shows client validation errors without calling the API", async () => {
    const fetchMock = vi.fn();
    stubApi(fetchMock);
    renderForm();

    fireEvent.click(screen.getByRole("button", { name: "บันทึก Draft" }));

    expect(await screen.findByText("กรุณาระบุชื่ออย่างน้อย 2 ตัวอักษร")).toBeInTheDocument();
    expect(screen.getByText("รูปแบบอีเมลไม่ถูกต้อง")).toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("prevents duplicate save and navigates after the successful response", async () => {
    const removeListener = vi.spyOn(window, "removeEventListener");
    let resolveFetch!: (value: Response) => void;
    const fetchMock = vi.fn(() => new Promise<Response>((resolve) => { resolveFetch = resolve; }));
    stubApi(fetchMock);
    renderForm();
    fillValidForm();

    const save = screen.getByRole("button", { name: "บันทึก Draft" });
    fireEvent.click(save);
    fireEvent.click(save);
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));
    expect(save).toBeDisabled();

    resolveFetch({ ok: true, json: async () => savedRequest } as Response);
    await waitFor(() => expect(push).toHaveBeenCalledWith(`/requests/${savedRequest.id}`));
    expect(screen.getByLabelText("ชื่อพนักงาน")).toHaveValue(savedRequest.employeeName);
    await waitFor(() => expect(removeListener).toHaveBeenCalledWith("beforeunload", expect.any(Function)));
  });

  it("maps server field errors and preserves entered values", async () => {
    const apiError: ApiError = {
      timestamp: "2026-09-25T03:00:00Z",
      status: 400,
      code: "VALIDATION_ERROR",
      message: "Request validation failed",
      path: "/api/v1/equipment-requests",
      fieldErrors: {
        employeeEmail: "Email is already invalid on the server",
        "items[0].quantity": "Quantity was rejected by the server",
      },
    };
    stubApi(vi.fn().mockResolvedValue({ ok: false, status: 400, json: async () => apiError } as Response));
    renderForm();
    fillValidForm();
    fireEvent.click(screen.getByRole("button", { name: "+ เพิ่มรายการ" }));

    fireEvent.click(screen.getByRole("button", { name: "บันทึก Draft" }));

    expect(await screen.findByText("Email is already invalid on the server")).toBeInTheDocument();
    expect(screen.getByText("Quantity was rejected by the server")).toBeInTheDocument();
    expect(screen.getByLabelText("หัวข้อคำขอ")).toHaveValue("Notebook for new project");
  });

  it("keeps edits and offers reload when the version is stale", async () => {
    const conflict: ApiError = {
      timestamp: "2026-09-25T03:00:00Z",
      status: 409,
      code: "REQUEST_VERSION_CONFLICT",
      message: "This request has been updated by another user",
      path: `/api/v1/equipment-requests/${savedRequest.id}`,
      fieldErrors: {},
    };
    stubApi(vi.fn().mockResolvedValue({ ok: false, status: 409, json: async () => conflict } as Response));
    renderForm({ mode: "edit", request: savedRequest });
    fireEvent.change(screen.getByLabelText("หัวข้อคำขอ"), { target: { value: "Locally edited notebook title" } });

    fireEvent.click(screen.getByRole("button", { name: "บันทึก Draft" }));

    expect(await screen.findByRole("button", { name: "โหลดข้อมูลล่าสุด" })).toBeInTheDocument();
    expect(screen.getByLabelText("หัวข้อคำขอ")).toHaveValue("Locally edited notebook title");
    expect(push).not.toHaveBeenCalled();
  });

  it("stores the saved request in the detail cache before navigating", async () => {
    stubApi(vi.fn().mockResolvedValue({ ok: true, json: async () => savedRequest } as Response));
    const { client } = renderForm();
    fillValidForm();

    fireEvent.click(screen.getByRole("button", { name: "บันทึก Draft" }));

    await waitFor(() => expect(push).toHaveBeenCalledWith(`/requests/${savedRequest.id}`));
    const cached = client.getQueryData(equipmentRequestQueryKey(demoActors[0], savedRequest.id));
    expect(cached).toEqual(savedRequest);
  });

  it("keeps in-progress edits and the loaded version when newer data arrives", async () => {
    const fetchMock = vi.fn().mockResolvedValue({ ok: true, json: async () => ({ ...savedRequest, version: 1 }) } as Response);
    stubApi(fetchMock);
    const { rerenderWith } = renderForm({ mode: "edit", request: savedRequest });
    fireEvent.change(screen.getByLabelText("หัวข้อคำขอ"), { target: { value: "Locally edited notebook title" } });

    rerenderWith({ ...savedRequest, title: "Changed by another session", version: 5 });

    expect(screen.getByLabelText("หัวข้อคำขอ")).toHaveValue("Locally edited notebook title");
    fireEvent.click(screen.getByRole("button", { name: "บันทึก Draft" }));
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));
    const [, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(JSON.parse(init.body as string)).toMatchObject({ expectedVersion: 0, title: "Locally edited notebook title" });
  });
});

