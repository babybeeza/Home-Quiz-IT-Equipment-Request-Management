"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useRef, useState, type BaseSyntheticEvent } from "react";
import { useFieldArray, useForm, type FieldPath } from "react-hook-form";
import { z } from "zod";
import {
  createEquipmentRequest,
  EquipmentRequestApiError,
  equipmentRequestQueryKey,
  updateEquipmentRequest,
} from "./api";
import { useIdentity } from "./identity";
import { equipmentTypes, type EquipmentRequest, type EquipmentRequestInput } from "./types";
import { useDirtyWarning } from "./use-dirty-warning";

function bangkokToday() {
  const parts = new Intl.DateTimeFormat("en", {
    timeZone: "Asia/Bangkok", year: "numeric", month: "2-digit", day: "2-digit",
  }).formatToParts(new Date());
  const value = Object.fromEntries(parts.map((part) => [part.type, part.value]));
  return `${value.year}-${value.month}-${value.day}`;
}

const formSchema = z.object({
  employeeName: z.string().trim().min(2, "กรุณาระบุชื่ออย่างน้อย 2 ตัวอักษร").max(100),
  employeeEmail: z.string().trim().email("รูปแบบอีเมลไม่ถูกต้อง").max(254),
  department: z.string().trim().min(1, "กรุณาระบุแผนก").max(100),
  title: z.string().trim().min(5, "หัวข้อต้องมีอย่างน้อย 5 ตัวอักษร").max(150),
  purpose: z.string().trim().min(10, "วัตถุประสงค์ต้องมีอย่างน้อย 10 ตัวอักษร").max(500),
  requiredDate: z.string().min(1, "กรุณาระบุวันที่").refine((date) => date >= bangkokToday(), "วันที่ต้องไม่เป็นอดีต"),
  additionalNote: z.string().max(500).optional(),
  items: z.array(z.object({
    equipmentType: z.enum(equipmentTypes),
    quantity: z.number().int().min(1, "จำนวนต่ำสุด 1").max(5, "จำนวนสูงสุด 5"),
    specification: z.string().max(250).optional(),
  })),
});

export type EquipmentRequestFormValues = z.infer<typeof formSchema>;

const emptyValues: EquipmentRequestFormValues = {
  employeeName: "",
  employeeEmail: "",
  department: "",
  title: "",
  purpose: "",
  requiredDate: "",
  additionalNote: "",
  items: [],
};

function valuesFromRequest(request?: EquipmentRequest): EquipmentRequestFormValues {
  if (!request) return emptyValues;
  return {
    employeeName: request.employeeName,
    employeeEmail: request.employeeEmail,
    department: request.department,
    title: request.title,
    purpose: request.purpose,
    requiredDate: request.requiredDate,
    additionalNote: request.additionalNote ?? "",
    items: request.items.map(({ equipmentType, quantity, specification }) => ({
      equipmentType, quantity, specification: specification ?? "",
    })),
  };
}

export function useEquipmentRequestForm(options: {
  mode: "create" | "edit";
  request?: EquipmentRequest;
  onSuccess: (request: EquipmentRequest) => void;
}) {
  const { actor } = useIdentity();
  const queryClient = useQueryClient();
  const [formError, setFormError] = useState<string | null>(null);
  const [hasConflict, setHasConflict] = useState(false);
  const submissionLock = useRef(false);
  // The loaded request is the edit baseline; later prop changes must not reset input or advance expectedVersion.
  const baseline = useRef(options.request);
  const form = useForm<EquipmentRequestFormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: valuesFromRequest(options.request),
  });
  const items = useFieldArray({ control: form.control, name: "items" });
  useDirtyWarning(form.formState.isDirty);

  const mutation = useMutation({
    mutationFn: async (values: EquipmentRequestFormValues) => {
      const input: EquipmentRequestInput = {
        ...values,
        additionalNote: values.additionalNote || null,
        items: values.items.map((item) => ({ ...item, specification: item.specification || null })),
      };
      if (options.mode === "edit" && baseline.current) {
        return updateEquipmentRequest(actor, baseline.current.id, { ...input, expectedVersion: baseline.current.version });
      }
      return createEquipmentRequest(actor, input);
    },
    onSuccess: (saved) => {
      baseline.current = saved;
      queryClient.setQueryData(equipmentRequestQueryKey(actor, saved.id), saved);
      form.reset(valuesFromRequest(saved));
      options.onSuccess(saved);
    },
    onError: (error) => {
      if (error instanceof EquipmentRequestApiError) {
        if (error.details.code === "REQUEST_VERSION_CONFLICT") setHasConflict(true);
        const knownPaths = new Set<string>();
        Object.entries(error.details.fieldErrors).forEach(([path, message]) => {
          if (path in emptyValues || /^items\[\d+\]\.(equipmentType|quantity|specification)$/.test(path)) {
            const formPath = path.replace(/\[(\d+)\]/g, ".$1") as FieldPath<EquipmentRequestFormValues>;
            form.setError(formPath, { message });
            knownPaths.add(path);
          }
        });
        if (knownPaths.size !== Object.keys(error.details.fieldErrors).length || Object.keys(error.details.fieldErrors).length === 0) {
          setFormError(error.details.message);
        }
      } else {
        setFormError("ไม่สามารถเชื่อมต่อกับระบบได้ กรุณาลองใหม่");
      }
    },
    onSettled: () => { submissionLock.current = false; },
  });

  const submit = (event?: BaseSyntheticEvent) => {
    if (submissionLock.current || mutation.isPending) {
      event?.preventDefault();
      return;
    }
    submissionLock.current = true;
    setFormError(null);
    setHasConflict(false);
    void form.handleSubmit(
      (values) => mutation.mutate(values),
      () => { submissionLock.current = false; },
    )(event);
  };

  return { form, items, submit, isPending: mutation.isPending, formError, hasConflict };
}

