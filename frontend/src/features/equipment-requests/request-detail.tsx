"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { equipmentRequestQueryKey, getEquipmentRequest, EquipmentRequestApiError } from "./api";
import { useIdentity } from "./identity";
import { RequestActions } from "./request-actions";

export function RequestDetail({ id }: { id: string }) {
  const { actor } = useIdentity();
  const query = useQuery({
    queryKey: equipmentRequestQueryKey(actor, id),
    queryFn: ({ signal }) => getEquipmentRequest(actor, id, signal),
  });

  if (query.isPending) return <main className="page-shell"><p role="status">กำลังโหลดคำขอ…</p></main>;
  if (query.isError) {
    const error = query.error instanceof EquipmentRequestApiError ? query.error.details : null;
    return <main className="page-shell"><div role="alert" className="alert error">{error?.status === 404 ? "ไม่พบคำขอ" : error?.status === 403 ? "คุณไม่มีสิทธิ์ดูคำขอนี้" : "โหลดข้อมูลไม่สำเร็จ"}</div></main>;
  }

  const request = query.data;
  const canEdit = actor.role === "EMPLOYEE" && request.status === "DRAFT";
  return (
    <main className="page-shell">
      <div className="page-heading">
        <div><p className="eyebrow">{request.requestNumber}</p><h1>{request.title}</h1></div>
        <span className={`status-pill status-${request.status.toLowerCase()}`}>{request.status}</span>
      </div>
      <RequestActions request={request} />
      <section className="detail-card">
        <dl className="details-grid">
          <Detail label="ผู้ขอ" value={request.employeeName} />
          <Detail label="อีเมล" value={request.employeeEmail} />
          <Detail label="แผนก" value={request.department} />
          <Detail label="วันที่ต้องการใช้" value={request.requiredDate} />
        </dl>
        <h2>วัตถุประสงค์</h2><p>{request.purpose}</p>
        {request.additionalNote && <><h2>หมายเหตุ</h2><p>{request.additionalNote}</p></>}
        {request.status === "REJECTED" && request.rejectionReason && <><h2>เหตุผลการปฏิเสธ</h2><p>{request.rejectionReason}</p></>}
        <h2>รายการอุปกรณ์ ({request.totalItems} ชิ้น)</h2>
        {request.items.length === 0 ? <p className="empty-note">ยังไม่มีรายการอุปกรณ์</p> : (
          <ul className="item-list">{request.items.map((item) => <li key={item.id}><strong>{item.equipmentType}</strong><span>{item.quantity} ชิ้น</span><span>{item.specification || "—"}</span></li>)}</ul>
        )}
      </section>
      <div className="form-actions"><Link className="button secondary" href="/requests">กลับ</Link>{canEdit && <Link className="button" href={`/requests/${request.id}/edit`}>แก้ไข Draft</Link>}</div>
    </main>
  );
}

function Detail({ label, value }: { label: string; value: string }) {
  return <div><dt>{label}</dt><dd>{value}</dd></div>;
}

