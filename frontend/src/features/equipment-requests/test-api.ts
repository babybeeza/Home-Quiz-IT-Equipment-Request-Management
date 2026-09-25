import { vi } from "vitest";
import type { ReferenceData } from "./types";

export const testReferenceData: ReferenceData = {
  departments: [
    { code: "SOFTWARE_ENGINEERING", name: "Software Engineering" },
    { code: "FINANCE", name: "Finance" },
  ],
  equipmentTypes: [{ type: "MONITOR", label: "จอภาพ", minQuantity: 1, maxQuantity: 5 }],
};

/**
 * Installs a global fetch that answers the reference-data endpoint itself and forwards every other call
 * to `handler`, so tests keep asserting on request-API calls only.
 */
export function stubApi<T extends (...args: never[]) => unknown>(
  handler: T,
  reference: () => Promise<Response> = () => Promise.resolve({ ok: true, json: async () => testReferenceData } as Response),
) {
  vi.stubGlobal("fetch", (url: string, init?: RequestInit) =>
    String(url).includes("/reference-data") ? reference() : (handler as unknown as (u: string, i?: RequestInit) => unknown)(url, init));
  return handler;
}
