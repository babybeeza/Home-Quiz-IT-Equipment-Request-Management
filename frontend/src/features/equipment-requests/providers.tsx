"use client";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useState, type ReactNode } from "react";
import { IdentityProvider } from "./identity";

export function ApplicationProviders({ children }: { children: ReactNode }) {
  const [queryClient] = useState(() => new QueryClient({
    defaultOptions: { queries: { retry: false, staleTime: 15_000 } },
  }));
  return (
    <QueryClientProvider client={queryClient}>
      <IdentityProvider>{children}</IdentityProvider>
    </QueryClientProvider>
  );
}

