"use client";

import { useEffect, useId, useRef, useState, type FormEvent } from "react";
import { useIdentity } from "./identity";
import type { EquipmentRequest, RequestActionName } from "./types";
import { availableActions, useRequestAction, type ActionError } from "./use-request-action";

const labels: Record<RequestActionName, string> = {
  submit: "ส่งคำขอ",
  cancel: "ยกเลิกคำขอ",
  approve: "อนุมัติ",
  reject: "ปฏิเสธ",
};

export function RequestActions({ request }: { request: EquipmentRequest }) {
  const { actor } = useIdentity();
  const { run, pendingAction, error, hasConflict, announcement, reloadLatest } = useRequestAction(request);
  const [rejectOpen, setRejectOpen] = useState(false);
  const rejectButton = useRef<HTMLButtonElement>(null);
  const actions = availableActions(actor.role, request.status);

  const trigger = (action: RequestActionName) => {
    if (action === "reject") {
      setRejectOpen(true);
      return;
    }
    if (action === "cancel" && !window.confirm("ยืนยันยกเลิกคำขอนี้? การยกเลิกย้อนกลับไม่ได้")) return;
    void run(action);
  };

  const closeReject = () => {
    setRejectOpen(false);
    rejectButton.current?.focus();
  };

  return (
    <>
      <p role="status" className="visually-hidden">{announcement}</p>
      {hasConflict && (
        <div role="alert" className="alert conflict">
          คำขอนี้ถูกเปลี่ยนจากที่อื่นแล้ว ยังไม่มีการเปลี่ยนแปลงจากคุณ
          <button type="button" onClick={reloadLatest}>โหลดข้อมูลล่าสุด</button>
        </div>
      )}
      {error && !rejectOpen && <ErrorAlert error={error} />}
      {actions.length > 0 && (
        <div className="request-actions" aria-label="การดำเนินการกับคำขอ" role="group">
          {actions.map((action) => (
            <button
              key={action}
              ref={action === "reject" ? rejectButton : undefined}
              type="button"
              className={action === "cancel" || action === "reject" ? "danger" : undefined}
              disabled={pendingAction !== null}
              onClick={() => trigger(action)}
            >
              {pendingAction === action ? "กำลังดำเนินการ…" : labels[action]}
            </button>
          ))}
        </div>
      )}
      {rejectOpen && (
        <RejectDialog
          pending={pendingAction === "reject"}
          error={error}
          onCancel={closeReject}
          onConfirm={async (reason) => {
            if (await run("reject", reason)) closeReject();
          }}
        />
      )}
    </>
  );
}

function RejectDialog({ pending, error, onCancel, onConfirm }: {
  pending: boolean;
  error: ActionError | null;
  onCancel: () => void;
  onConfirm: (reason: string) => void;
}) {
  const titleId = useId();
  const reasonId = useId();
  const errorId = useId();
  const textarea = useRef<HTMLTextAreaElement>(null);
  const [reason, setReason] = useState("");
  const [clientError, setClientError] = useState<string | null>(null);

  useEffect(() => { textarea.current?.focus(); }, []);

  const submit = (event: FormEvent) => {
    event.preventDefault();
    const trimmed = reason.trim();
    if (!trimmed) return setClientError("กรุณาระบุเหตุผลการปฏิเสธ");
    if (trimmed.length > 500) return setClientError("เหตุผลต้องไม่เกิน 500 ตัวอักษร");
    setClientError(null);
    onConfirm(trimmed);
  };

  const message = clientError ?? error?.message ?? null;

  return (
    <dialog open className="reject-dialog" aria-modal="true" aria-labelledby={titleId}
      onKeyDown={(event) => { if (event.key === "Escape" && !pending) onCancel(); }}>
      <form onSubmit={submit} noValidate>
        <h2 id={titleId}>ปฏิเสธคำขอ</h2>
        <label htmlFor={reasonId}>เหตุผลการปฏิเสธ</label>
        <textarea
          id={reasonId}
          ref={textarea}
          rows={4}
          maxLength={500}
          value={reason}
          aria-invalid={message ? true : undefined}
          aria-describedby={message ? errorId : undefined}
          onChange={(event) => setReason(event.target.value)}
        />
        {message && <p id={errorId} role="alert" className="field-error">{message}</p>}
        <div className="form-actions">
          <button type="button" className="secondary" onClick={onCancel} disabled={pending}>ปิด</button>
          <button type="submit" className="danger" disabled={pending}>{pending ? "กำลังดำเนินการ…" : "ยืนยันปฏิเสธ"}</button>
        </div>
      </form>
    </dialog>
  );
}

function ErrorAlert({ error }: { error: ActionError }) {
  const details = Object.values(error.fieldErrors);
  return (
    <div role="alert" className="alert error">
      <div>
        {error.message}
        {details.length > 0 && <ul>{details.map((detail) => <li key={detail}>{detail}</li>)}</ul>}
      </div>
    </div>
  );
}
