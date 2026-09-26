"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { useId } from "react";
import { equipmentRequestListKey, searchEquipmentRequests } from "./api";
import { useIdentity } from "./identity";
import type { RequestStatus } from "./types";
import { DepartmentOptions } from "./department-options";
import { useDebouncedUrlField, useRequestSearch } from "./use-request-search";
import { useDepartmentSuggestions } from "./use-reference-data";
import { RequestActions } from "./request-actions";

const statusOptions: RequestStatus[] = ["DRAFT", "PENDING", "APPROVED", "REJECTED", "CANCELLED"];
const createdAtFormat = new Intl.DateTimeFormat("th-TH", { dateStyle: "medium", timeStyle: "short", timeZone: "Asia/Bangkok" });

export function RequestList() {
  const { actor } = useIdentity();
  const { params, update } = useRequestSearch();
  const ids = useId();
  const departmentSuggestions = useDepartmentSuggestions();
  const [keyword, setKeyword] = useDebouncedUrlField(params.keyword, (value) => update({ keyword: value }));
  const [department, setDepartment] = useDebouncedUrlField(params.department, (value) => update({ department: value }));

  const query = useQuery({
    queryKey: equipmentRequestListKey(actor, params),
    queryFn: ({ signal }) => searchEquipmentRequests(actor, params, signal),
    // Keep the previous page on screen while paging, but never another actor's rows.
    placeholderData: (previous, previousQuery) =>
      previousQuery?.queryKey[1] === actor.userId && previousQuery.queryKey[2] === actor.role ? previous : undefined,
  });

  const hasFilters = Boolean(params.keyword || params.status || params.department);
  const clearFilters = () => update({ keyword: "", status: "", department: "" });

  return (
    <main className="page-shell">
      <div className="page-heading">
        <div><p className="eyebrow">IT EQUIPMENT REQUESTS</p><h1>คำขออุปกรณ์ IT</h1></div>
        {actor.role === "EMPLOYEE" && <Link className="button" href="/requests/new">สร้างคำขอใหม่</Link>}
      </div>

      <section className="filter-bar" aria-label="ค้นหาและกรองคำขอ">
        <div>
          <label htmlFor={`${ids}-keyword`}>ค้นหา</label>
          <input id={`${ids}-keyword`} type="search" value={keyword} maxLength={150} placeholder="เลขที่คำขอ หัวข้อ หรือชื่อผู้ขอ"
            onChange={(event) => setKeyword(event.target.value)} />
        </div>
        <div>
          <label htmlFor={`${ids}-status`}>สถานะ</label>
          <select id={`${ids}-status`} value={params.status} onChange={(event) => update({ status: event.target.value as RequestStatus | "" })}>
            <option value="">ทุกสถานะ</option>
            {statusOptions.map((status) => <option key={status} value={status}>{status}</option>)}
          </select>
        </div>
        <div>
          <label htmlFor={`${ids}-department`}>แผนก</label>
          <input id={`${ids}-department`} list={`${ids}-departments`} autoComplete="off" value={department} maxLength={100} placeholder="เช่น Finance"
            onChange={(event) => setDepartment(event.target.value)} />
          <DepartmentOptions id={`${ids}-departments`} names={departmentSuggestions} />
        </div>
        <div>
          <label htmlFor={`${ids}-sort`}>เรียงตามวันที่สร้าง</label>
          <select id={`${ids}-sort`} value={params.sort} onChange={(event) => update({ sort: event.target.value as typeof params.sort })}>
            <option value="createdAt,desc">ใหม่ → เก่า</option>
            <option value="createdAt,asc">เก่า → ใหม่</option>
          </select>
        </div>
      </section>

      {query.isPending ? (
        <p role="status">กำลังโหลดรายการ…</p>
      ) : query.isError ? (
        <div role="alert" className="alert error">
          โหลดรายการไม่สำเร็จ
          <button type="button" onClick={() => void query.refetch()}>ลองใหม่</button>
        </div>
      ) : query.data.content.length === 0 ? (
        <div className="empty-state" role="status">
          <p>{query.data.totalElements > 0 ? "ไม่มีรายการในหน้านี้" : hasFilters ? "ไม่พบคำขอที่ตรงกับเงื่อนไข" : "ยังไม่มีคำขอ"}</p>
          {query.data.totalElements > 0
            ? <button type="button" onClick={() => update({ page: 0 })}>กลับหน้าแรก</button>
            : hasFilters && <button type="button" onClick={clearFilters}>ล้างตัวกรอง</button>}
        </div>
      ) : (
        <>
          {query.isPlaceholderData && <p role="status" className="loading-note">กำลังโหลด…</p>}
          <div className="table-wrap" aria-busy={query.isFetching}>
            <table className="request-table">
              <thead>
                <tr>
                  <th scope="col">เลขที่คำขอ</th><th scope="col">หัวข้อ</th><th scope="col">ผู้ขอ</th><th scope="col">แผนก</th>
                  <th scope="col">วันที่ต้องการใช้</th><th scope="col">จำนวนรวม</th><th scope="col">สถานะ</th><th scope="col">สร้างเมื่อ</th><th scope="col">การดำเนินการ</th>
                </tr>
              </thead>
              <tbody>
                {query.data.content.map((row) => (
                  <tr key={`${row.id}-${row.version}`}>
                    <td><Link href={`/requests/${row.id}`}>{row.requestNumber}</Link></td>
                    <td>{row.title}</td>
                    <td>{row.employeeName}</td>
                    <td>{row.department}</td>
                    <td>{row.requiredDate}</td>
                    <td>{row.totalItems}</td>
                    <td><span className={`status-pill status-${row.status.toLowerCase()}`}>{row.status}</span></td>
                    <td>{createdAtFormat.format(new Date(row.createdAt))}</td>
                    <td className="row-actions-cell">
                      {actor.role === "EMPLOYEE" && row.status === "DRAFT" &&
                        <Link className="button secondary" href={`/requests/${row.id}/edit`}>แก้ไข Draft</Link>}
                      <RequestActions request={row} inline disabled={query.isFetching} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <nav className="pagination" aria-label="เปลี่ยนหน้า">
            <button type="button" className="secondary" disabled={params.page === 0} onClick={() => update({ page: params.page - 1 })}>ก่อนหน้า</button>
            <span>หน้า {params.page + 1} จาก {Math.max(query.data.totalPages, 1)} · ทั้งหมด {query.data.totalElements} รายการ</span>
            <button type="button" className="secondary" disabled={params.page + 1 >= query.data.totalPages} onClick={() => update({ page: params.page + 1 })}>ถัดไป</button>
          </nav>
        </>
      )}
    </main>
  );
}
