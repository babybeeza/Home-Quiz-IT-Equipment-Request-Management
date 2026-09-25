import type { Metadata } from "next";
import "./styles.css";

export const metadata: Metadata = {
  title: "IT Equipment Requests",
  description: "Request and approve IT equipment",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="th">
      <body>{children}</body>
    </html>
  );
}
