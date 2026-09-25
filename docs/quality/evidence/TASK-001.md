# Evidence: TASK-001

Date / operator: 2026-09-25 / Codex implementation; approved by project owner
Revision or working tree snapshot: `8c6de53` on `main`
Environment: Windows, Docker 29.5.2 / Compose 5.1.3, host Temurin Java 25.0.3, Java 21 target, Node container 24.15.0
Requirement IDs: REQ-08, REQ-09, REQ-13

| Check | Exact command / manual steps | Exit code | Result | Observations |
| --- | --- | --- | --- | --- |
| Frontend clean install | `docker run --rm -v "${PWD}:/workspace" -w /workspace/frontend node:24.15.0-alpine npm ci` | 0 | PASS | 437 packages, 0 vulnerabilities |
| Frontend lint | same Node container: `npm run lint` | 0 | PASS | ESLint completed with max warnings 0 |
| Frontend typecheck | same Node container: `npm run typecheck` | 0 | PASS | TypeScript no-emit completed |
| Frontend test discovery | same Node container: `npm test` | 0 | PASS | 1 file / 1 foundation test passed |
| Frontend production build | same Node container: `npm run build` | 0 | PASS | Next.js 16.3.6 compiled and prerendered `/` |
| Backend clean build/test | `backend\\mvnw.cmd --batch-mode clean package` | 0 | PASS | 1 JUnit 5 + MockK foundation test passed; executable jar created |
| Compose validation | `docker compose config --quiet` | 0 | PASS | Configuration valid |
| PostgreSQL / Redis startup | `docker compose up -d --wait` | 0 | PASS | Both containers reported healthy |
| Service checks | `pg_isready -U equipment_app -d equipment_requests`; `redis-cli ping` | 0 | PASS | PostgreSQL accepting connections; Redis returned PONG |
| Backend startup | `backend\\mvnw.cmd --batch-mode spring-boot:run` | 0 at startup | PASS | Spring Boot 4.1.1 started, Flyway/JPA connected to PostgreSQL, readiness accepted traffic; GET `/` returned expected 404 because business endpoints are not in TASK-001 |
| Markdown links / Compose structure | repository Markdown link scan excluding generated directories; `docker compose config --quiet` | 0 | PASS | 34 Markdown files checked, relative links resolved |
| Frontend/backend Docker image build | `docker build` | N/A | NOT RUN | Host/container builds above verify manifests; image builds remain an optional follow-up before delivery |

## Review findings

- Latest TypeScript 7 and ESLint 10 produced invalid peer dependency trees with the selected Next.js lint stack. ADR-001 pins TypeScript 5.9.3 and ESLint 9.39.5, for which `npm ls`, lint and build succeed. ESLint 9 prints an upstream support warning as of the execution date; upgrading requires a Next.js plugin graph that supports ESLint 10 without overrides.
- A first parallel `npm ci` collided with a Markdown scan traversing `node_modules` on the Windows bind mount. The final clean install and all checks ran sequentially and passed; generated directories are excluded from the final scan.
- Foundation tests only prove runner discovery. Feature-level minimum tests from REQ-11 remain planned for TASK-003 onward.

## Outstanding checks

- Implement gate approved by the project owner on 2026-09-25; TASK-002 may proceed.
- Docker image builds are not required for TASK-001 acceptance and were not run; build them before delivery if the Dockerfiles become a supported delivery path.
- Commit hash is captured by Git history for the reviewed baseline.
