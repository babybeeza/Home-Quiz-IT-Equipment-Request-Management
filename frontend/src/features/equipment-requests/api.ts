import type {
  Actor,
  ApiError,
  EquipmentRequest,
  EquipmentRequestInput,
  EquipmentRequestPage,
  ListParams,
  ReferenceData,
  RequestActionName,
} from "./types";

const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api/v1";

export class EquipmentRequestApiError extends Error {
  constructor(public readonly details: ApiError) {
    super(details.message);
  }
}

async function request<T>(path: string, actor: Actor, init?: RequestInit): Promise<T> {
  const response = await fetch(`${apiBaseUrl}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      "X-User-Id": actor.userId,
      "X-Role": actor.role,
      ...init?.headers,
    },
  });

  if (!response.ok) {
    const details = (await response.json().catch(() => ({
      timestamp: new Date().toISOString(),
      status: response.status,
      code: "INTERNAL_ERROR",
      message: "ไม่สามารถดำเนินการได้",
      path,
      fieldErrors: {},
    }))) as ApiError;
    throw new EquipmentRequestApiError(details);
  }
  return response.json() as Promise<T>;
}

export function createEquipmentRequest(actor: Actor, input: EquipmentRequestInput) {
  return request<EquipmentRequest>("/equipment-requests", actor, {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function equipmentRequestQueryKey(actor: Actor, id: string) {
  return ["equipment-request", actor.userId, actor.role, id] as const;
}

export function getEquipmentRequest(actor: Actor, id: string, signal?: AbortSignal) {
  return request<EquipmentRequest>(`/equipment-requests/${id}`, actor, { signal });
}

export function updateEquipmentRequest(
  actor: Actor,
  id: string,
  input: EquipmentRequestInput & { expectedVersion: number },
) {
  return request<EquipmentRequest>(`/equipment-requests/${id}`, actor, {
    method: "PUT",
    body: JSON.stringify(input),
  });
}


export function performRequestAction(
  actor: Actor,
  id: string,
  action: RequestActionName,
  body: { expectedVersion: number; reason?: string },
) {
  return request<EquipmentRequest>(`/equipment-requests/${id}/${action}`, actor, {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export function equipmentRequestListKey(actor: Actor, params: ListParams) {
  return ["equipment-requests", actor.userId, actor.role, params] as const;
}

/** Serializes list parameters, omitting defaults so URLs stay short. Shared by the URL state and the API call. */
export function listQueryString(params: ListParams) {
  const query = new URLSearchParams();
  if (params.keyword) query.set("keyword", params.keyword);
  if (params.status) query.set("status", params.status);
  if (params.department) query.set("department", params.department);
  if (params.page > 0) query.set("page", String(params.page));
  if (params.sort !== "createdAt,desc") query.set("sort", params.sort);
  const text = query.toString();
  return text ? `?${text}` : "";
}

export function searchEquipmentRequests(actor: Actor, params: ListParams, signal?: AbortSignal) {
  return request<EquipmentRequestPage>(`/equipment-requests${listQueryString(params)}`, actor, { signal });
}

export function getReferenceData(actor: Actor, signal?: AbortSignal) {
  return request<ReferenceData>("/reference-data", actor, { signal });
}
