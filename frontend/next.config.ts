import type { NextConfig } from "next";

// Docker UI-test stack only (TASK-009): proxy same-origin /api/v1 to the backend service.
// Unset in local development, where the browser calls NEXT_PUBLIC_API_BASE_URL directly.
const apiProxyTarget = process.env.API_PROXY_TARGET;

const nextConfig: NextConfig = {
  reactStrictMode: true,
  async rewrites() {
    return apiProxyTarget ? [{ source: "/api/v1/:path*", destination: `${apiProxyTarget}/api/v1/:path*` }] : [];
  },
};

export default nextConfig;
