"use client";

import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { listQueryString } from "./api";
import type { ListParams, ListSort, RequestStatus } from "./types";

const statuses: RequestStatus[] = ["DRAFT", "PENDING", "APPROVED", "REJECTED", "CANCELLED"];
const sorts: ListSort[] = ["createdAt,desc", "createdAt,asc"];

/** Reads list parameters from the URL; any invalid value falls back to its default. */
export function parseListParams(search: URLSearchParams): ListParams {
  const status = search.get("status") ?? "";
  const sort = search.get("sort") ?? "";
  const page = Number(search.get("page") ?? "0");
  return {
    keyword: (search.get("keyword") ?? "").trim().slice(0, 150),
    status: statuses.includes(status as RequestStatus) ? (status as RequestStatus) : "",
    department: (search.get("department") ?? "").trim().slice(0, 100),
    page: Number.isInteger(page) && page >= 0 ? page : 0,
    sort: sorts.includes(sort as ListSort) ? (sort as ListSort) : "createdAt,desc",
  };
}

/** The URL is the only store for list state (ADR-005); changing anything but the page returns to page 0. */
export function useRequestSearch() {
  const search = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();
  const params = useMemo(() => parseListParams(new URLSearchParams(search.toString())), [search]);

  const update = useCallback((patch: Partial<ListParams>) => {
    const next: ListParams = { ...params, ...patch, page: patch.page ?? 0 };
    if (listQueryString(next) === listQueryString(params)) return;
    router.push(`${pathname}${listQueryString(next)}`, { scroll: false });
  }, [params, pathname, router]);

  return { params, update };
}

/**
 * Local draft for a debounced text filter. Typing commits the trimmed value after `delay` ms;
 * an outside URL change (back/forward, clear filters) replaces the draft, but our own commit does not.
 */
export function useDebouncedUrlField(urlValue: string, commit: (value: string) => void, delay = 300) {
  const [draft, setDraft] = useState(urlValue);
  const [lastSeenUrlValue, setLastSeenUrlValue] = useState(urlValue);
  const timer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
  const latestCommit = useRef(commit);

  useEffect(() => { latestCommit.current = commit; });
  useEffect(() => () => clearTimeout(timer.current), []);

  if (urlValue !== lastSeenUrlValue) {
    setLastSeenUrlValue(urlValue);
    setDraft(urlValue);
  }

  const change = (value: string) => {
    setDraft(value);
    clearTimeout(timer.current);
    timer.current = setTimeout(() => {
      const trimmed = value.trim();
      setLastSeenUrlValue(trimmed);
      latestCommit.current(trimmed);
    }, delay);
  };

  return [draft, change] as const;
}
