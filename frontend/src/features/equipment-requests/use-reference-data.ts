"use client";

import { useQuery } from "@tanstack/react-query";
import { getReferenceData } from "./api";
import { useIdentity } from "./identity";

/**
 * Department suggestions from the Caffeine-cached reference endpoint (ADR-006). Suggestions are optional:
 * while loading or on failure the list is simply empty and inputs stay free text.
 */
export function useDepartmentSuggestions(): string[] {
  const { actor } = useIdentity();
  const query = useQuery({
    queryKey: ["reference-data", actor.userId, actor.role],
    queryFn: ({ signal }) => getReferenceData(actor, signal),
    staleTime: Infinity,
  });
  const departments = query.data?.departments;
  return Array.isArray(departments) ? departments.map((department) => department.name) : [];
}
