import { Suspense } from "react";
import { RequestList } from "@/features/equipment-requests/request-list";

export default function RequestsPage() {
  // useSearchParams in the client list requires a Suspense boundary for static rendering.
  return (
    <Suspense fallback={<main className="page-shell"><p role="status">กำลังโหลดรายการ…</p></main>}>
      <RequestList />
    </Suspense>
  );
}
