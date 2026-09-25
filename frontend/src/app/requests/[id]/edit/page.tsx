"use client";

import { useQuery } from "@tanstack/react-query";
import { useParams } from "next/navigation";
import { equipmentRequestQueryKey, getEquipmentRequest } from "@/features/equipment-requests/api";
import { useIdentity } from "@/features/equipment-requests/identity";
import { RequestForm } from "@/features/equipment-requests/request-form";

export default function EditRequestPage() {
  const { id } = useParams<{ id: string }>();
  const { actor } = useIdentity();
  // The form pins its loaded version; background refetches must not replace an in-progress edit.
  const query = useQuery({
    queryKey: equipmentRequestQueryKey(actor, id),
    queryFn: ({ signal }) => getEquipmentRequest(actor, id, signal),
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
  });
  if (query.isPending) return <main className="page-shell"><p role="status">กำลังโหลดแบบฟอร์ม…</p></main>;
  if (query.isError) return <main className="page-shell"><div role="alert" className="alert error">ไม่สามารถเปิดแบบฟอร์มแก้ไขได้</div></main>;
  return <RequestForm mode="edit" request={query.data} />;
}
