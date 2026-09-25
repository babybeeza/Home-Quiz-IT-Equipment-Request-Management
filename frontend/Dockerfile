# syntax=docker/dockerfile:1
# Frontend image for the Docker app/e2e profiles in compose.yaml (TASK-009): production build served by `next start`.
# The browser calls the same origin (/api/v1) and Next proxies it to API_PROXY_TARGET,
# so host browsers and the Playwright container use one URL without CORS.
FROM node:24.15.0-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci --no-audit --no-fund

FROM deps AS build
ARG NEXT_PUBLIC_API_BASE_URL=/api/v1
ARG API_PROXY_TARGET=http://backend:8080
ENV NEXT_PUBLIC_API_BASE_URL=$NEXT_PUBLIC_API_BASE_URL API_PROXY_TARGET=$API_PROXY_TARGET NEXT_TELEMETRY_DISABLED=1
COPY . .
RUN npm run build

FROM node:24.15.0-alpine AS runtime
ARG API_PROXY_TARGET=http://backend:8080
ENV NODE_ENV=production API_PROXY_TARGET=$API_PROXY_TARGET NEXT_TELEMETRY_DISABLED=1
WORKDIR /app
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci --omit=dev --no-audit --no-fund
COPY --from=build /app/.next ./.next
COPY --from=build /app/next.config.ts ./next.config.ts
USER node
EXPOSE 3000
HEALTHCHECK --interval=5s --timeout=3s --retries=30 CMD wget -qO /dev/null http://localhost:3000/requests || exit 1
CMD ["npx", "next", "start", "-p", "3000"]
