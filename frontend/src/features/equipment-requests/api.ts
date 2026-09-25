import type { Actor, ApiError, EquipmentRequest, EquipmentRequestInput, RequestActionName } from "./types";

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
