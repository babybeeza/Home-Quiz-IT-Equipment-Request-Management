"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useRef, useState } from "react";
import { EquipmentRequestApiError, equipmentRequestQueryKey, performRequestAction } from "./api";
import { useIdentity } from "./identity";
import type { ActorRole, EquipmentRequest, RequestActionName, RequestStatus } from "./types";

/** Mirrors the ui-flow action table. Hiding is a convenience; the backend stays authoritative. */
export function availableActions(role: ActorRole, status: RequestStatus): RequestActionName[] {
  if (role === "EMPLOYEE") {
    if (status === "DRAFT") return ["submit", "cancel"];
    if (status === "PENDING") return ["cancel"];
    return [];
  }
  return status === "PENDING" ? ["approve", "reject"] : [];
}

const successMessages: Record<RequestActionName, string> = {
  submit: "ส่งคำขอเพื่อพิจารณาแล้ว",
  cancel: "ยกเลิกคำขอแล้ว",
  approve: "อนุมัติคำขอแล้ว",
  reject: "ปฏิเสธคำขอแล้ว",
};

const errorMessages: Record<string, string> = {
  ITEMS_REQUIRED: "ต้องมีรายการอุปกรณ์อย่างน้อย 1 รายการก่อนส่งคำขอ",
  BUSINESS_RULE_VIOLATION: "ข้อมูลคำขอไม่ผ่านเงื่อนไขการส่ง กรุณาแก้ไข Draft ก่อน",
  REJECTION_REASON_REQUIRED: "กรุณาระบุเหตุผลการปฏิเสธ",
  VALIDATION_ERROR: "ข้อมูลไม่ถูกต้อง",
  ACCESS_DENIED: "คุณไม่มีสิทธิ์ทำรายการนี้",
  REQUEST_NOT_FOUND: "ไม่พบคำขอ",
};

export type ActionError = { message: string; fieldErrors: Record<string, string> };

export function useRequestAction(request: EquipmentRequest) {
  const { actor } = useIdentity();
  const queryClient = useQueryClient();
  const [error, setError] = useState<ActionError | null>(null);
  const [hasConflict, setHasConflict] = useState(false);
  const [announcement, setAnnouncement] = useState("");
  const lock = useRef(false);
  const queryKey = equipmentRequestQueryKey(actor, request.id);

  const mutation = useMutation({
    mutationFn: ({ action, reason }: { action: RequestActionName; reason?: string }) =>
      performRequestAction(actor, request.id, action, {
        expectedVersion: request.version,
        ...(action === "reject" ? { reason } : {}),
      }),
    onSuccess: (saved, { action }) => {
      queryClient.setQueryData(queryKey, saved);
      setAnnouncement(successMessages[action]);
    },
    onError: (failure) => {
      if (failure instanceof EquipmentRequestApiError) {
        // Both 409 codes mean the displayed request is out of date; the user decides whether to reload.
        if (failure.details.status === 409) {
          setHasConflict(true);
          return;
        }
        setError({
          message: errorMessages[failure.details.code] ?? failure.details.message,
          fieldErrors: failure.details.fieldErrors,
        });
        return;
      }
      setError({ message: "ไม่สามารถเชื่อมต่อกับระบบได้ กรุณาลองใหม่", fieldErrors: {} });
    },
    onSettled: () => { lock.current = false; },
  });

  /** Resolves true only when the server accepted the action; a second call while pending is ignored. */
  const run = async (action: RequestActionName, reason?: string) => {
    if (lock.current || mutation.isPending) return false;
    lock.current = true;
    setError(null);
    setHasConflict(false);
    setAnnouncement("");
    try {
      await mutation.mutateAsync({ action, reason });
      return true;
    } catch {
      return false;
    }
  };

  const reloadLatest = () => {
    setHasConflict(false);
    void queryClient.invalidateQueries({ queryKey });
  };

  return {
    run,
    pendingAction: mutation.isPending ? mutation.variables?.action ?? null : null,
    error,
    hasConflict,
    announcement,
    reloadLatest,
  };
}
