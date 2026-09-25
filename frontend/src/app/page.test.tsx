import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import RequestsPage from "./requests/page";

describe("RequestsPage", () => {
  it("renders the application heading", () => {
    render(<RequestsPage />);
    expect(screen.getByRole("heading", { name: "คำขออุปกรณ์ IT" })).toBeInTheDocument();
  });
});
