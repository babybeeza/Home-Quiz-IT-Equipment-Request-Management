"use client";

import { useRouter } from "next/navigation";
import { equipmentTypes, type EquipmentRequest } from "./types";
import { useEquipmentRequestForm } from "./use-equipment-request-form";
import { confirmDirtyNavigation } from "./use-dirty-warning";

export function RequestForm({ mode, request }: { mode: "create" | "edit"; request?: EquipmentRequest }) {
  const router = useRouter();
  const { form, items, submit, isPending, formError, hasConflict } = useEquipmentRequestForm({
    mode,
    request,
    onSuccess: (saved) => router.push(`/requests/${saved.id}`),
  });
  const { register, formState: { errors, isDirty } } = form;

  return (
    <main className="page-shell">
      <div className="page-heading">
        <div><p className="eyebrow">DRAFT REQUEST</p><h1>{mode === "create" ? "สร้างคำขออุปกรณ์" : "แก้ไขคำขอ"}</h1></div>
        {request && <span className="status-pill">{request.requestNumber}</span>}
      </div>

      {formError && <div role="alert" className="alert error">{formError}</div>}
      {hasConflict && (
        <div role="alert" className="alert conflict">
          ข้อมูลนี้ถูกแก้ไขจากที่อื่นแล้ว ระบบยังเก็บค่าที่คุณกรอกไว้
          <button type="button" onClick={() => window.location.reload()}>โหลดข้อมูลล่าสุด</button>
        </div>
      )}

      <form onSubmit={submit} noValidate>
        <section className="form-card">
          <h2>ข้อมูลผู้ขอ</h2>
          <div className="form-grid">
            <Field label="ชื่อพนักงาน" error={errors.employeeName?.message}><input {...register("employeeName")} /></Field>
            <Field label="อีเมล" error={errors.employeeEmail?.message}><input type="email" {...register("employeeEmail")} /></Field>
            <Field label="แผนก" error={errors.department?.message}><input {...register("department")} /></Field>
            <Field label="หัวข้อคำขอ" error={errors.title?.message}><input {...register("title")} /></Field>
          </div>
          <Field label="วัตถุประสงค์" error={errors.purpose?.message}><textarea rows={4} {...register("purpose")} /></Field>
          <div className="form-grid">
            <Field label="วันที่ต้องการใช้" error={errors.requiredDate?.message}><input type="date" {...register("requiredDate")} /></Field>
            <Field label="หมายเหตุเพิ่มเติม" error={errors.additionalNote?.message}><textarea rows={2} {...register("additionalNote")} /></Field>
          </div>
        </section>

        <section className="form-card">
          <div className="section-heading"><h2>รายการอุปกรณ์</h2><button type="button" className="secondary" onClick={() => items.append({ equipmentType: "NOTEBOOK", quantity: 1, specification: "" })}>+ เพิ่มรายการ</button></div>
          {items.fields.length === 0 && <p className="empty-note">Draft สามารถบันทึกโดยยังไม่มีรายการอุปกรณ์ได้</p>}
          {items.fields.map((item, index) => (
            <div className="item-row" key={item.id}>
              <Field label="ประเภท" error={errors.items?.[index]?.equipmentType?.message}>
                <select {...register(`items.${index}.equipmentType`)}>{equipmentTypes.map((type) => <option key={type}>{type}</option>)}</select>
              </Field>
              <Field label="จำนวน" error={errors.items?.[index]?.quantity?.message}><input type="number" min="1" max="5" {...register(`items.${index}.quantity`, { valueAsNumber: true })} /></Field>
              <Field label="รายละเอียด" error={errors.items?.[index]?.specification?.message}><input {...register(`items.${index}.specification`)} /></Field>
              <button type="button" className="danger-link" onClick={() => items.remove(index)}>ลบ</button>
            </div>
          ))}
        </section>

        <div className="form-actions">
          <button type="button" className="secondary" onClick={() => { if (confirmDirtyNavigation(isDirty)) router.push(request ? `/requests/${request.id}` : "/requests"); }}>ยกเลิก</button>
          <button type="submit" disabled={isPending}>{isPending ? "กำลังบันทึก…" : "บันทึก Draft"}</button>
        </div>
      </form>
    </main>
  );
}

function Field({ label, error, children }: { label: string; error?: string; children: React.ReactNode }) {
  return <label className="field"><span>{label}</span>{children}{error && <small role="alert">{error}</small>}</label>;
}

