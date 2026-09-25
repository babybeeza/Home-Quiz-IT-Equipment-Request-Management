import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { IdentityProvider } from "@/features/equipment-requests/identity";
import RequestsPage from "./requests/page";

vi.mock("next/navigation", () => ({
  usePathname: () => "/requests",
  useRouter: () => ({ push: vi.fn() }),
  useSearchParams: () => new URLSearchParams(),
}));

describe("RequestsPage", () => {
  it("renders the application heading around the request list", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 }),
    } as Response));

    render(
      <QueryClientProvider client={new QueryClient()}>
        <IdentityProvider><RequestsPage /></IdentityProvider>
      </QueryClientProvider>,
    );

    expect(await screen.findByRole("heading", { name: "คำขออุปกรณ์ IT" })).toBeInTheDocument();
    expect(await screen.findByText("ยังไม่มีคำขอ")).toBeInTheDocument();
  });
});
