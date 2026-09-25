"use client";

import { useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { createContext, useContext, useSyncExternalStore, type ReactNode } from "react";
import type { Actor } from "./types";

export const demoActors: Actor[] = [
  { userId: "employee-001", role: "EMPLOYEE", label: "สมชาย — Employee" },
  { userId: "employee-002", role: "EMPLOYEE", label: "สมหญิง — Employee" },
  { userId: "approver-001", role: "APPROVER", label: "หัวหน้าฝ่าย — Approver" },
];

type IdentityContextValue = {
  actor: Actor;
  selectActor: (userId: string) => void;
};

const IdentityContext = createContext<IdentityContextValue | null>(null);
const storageKey = "equipment-request-actor";
const identityEvent = "equipment-request-identity-change";

function subscribe(callback: () => void) {
  window.addEventListener("storage", callback);
  window.addEventListener(identityEvent, callback);
  return () => {
    window.removeEventListener("storage", callback);
    window.removeEventListener(identityEvent, callback);
  };
}

export function IdentityProvider({ children }: { children: ReactNode }) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const selectedId = useSyncExternalStore(
    subscribe,
    () => window.localStorage.getItem(storageKey) ?? demoActors[0].userId,
    () => demoActors[0].userId,
  );
  const actor = demoActors.find((candidate) => candidate.userId === selectedId) ?? demoActors[0];

  function selectActor(userId: string) {
    const selected = demoActors.find((candidate) => candidate.userId === userId);
    if (!selected || selected.userId === actor.userId) return;
    void queryClient.cancelQueries();
    queryClient.clear();
    window.localStorage.setItem(storageKey, selected.userId);
    window.dispatchEvent(new Event(identityEvent));
    router.push("/requests");
  }

  return <IdentityContext.Provider value={{ actor, selectActor }}>{children}</IdentityContext.Provider>;
}

export function useIdentity() {
  const value = useContext(IdentityContext);
  if (!value) throw new Error("useIdentity must be used inside IdentityProvider");
  return value;
}

