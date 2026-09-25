"use client";

import Link from "next/link";
import { demoActors, useIdentity } from "./identity";

export function AppHeader() {
  const { actor, selectActor } = useIdentity();
  return (
    <header className="app-header">
      <Link href="/requests" className="brand">IT Equipment Requests</Link>
      <label className="identity-selector">
        ผู้ใช้งานตัวอย่าง
        <select value={actor.userId} onChange={(event) => selectActor(event.target.value)}>
          {demoActors.map((candidate) => (
            <option key={candidate.userId} value={candidate.userId}>{candidate.label}</option>
          ))}
        </select>
      </label>
    </header>
  );
}

