import type { Metadata } from "next";
import { AppHeader } from "@/features/equipment-requests/app-header";
import { ApplicationProviders } from "@/features/equipment-requests/providers";
import "./styles.css";

export const metadata: Metadata = {
  title: "IT Equipment Requests",
  description: "Request and approve IT equipment",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="th">
      <body><ApplicationProviders><AppHeader />{children}</ApplicationProviders></body>
    </html>
  );
}
