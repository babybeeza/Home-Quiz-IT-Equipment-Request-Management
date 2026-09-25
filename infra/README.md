# Infrastructure

Local environments are defined by the single [`compose.yaml`](../compose.yaml) (project `home-quiz`) and the two Dockerfiles:

| Profile | Services | Use |
| --- | --- | --- |
| (default) | `postgres`, `redis` | Development with the backend and frontend on the host |
| `app` | + `backend` ([Dockerfile](../backend/Dockerfile)), `frontend` ([Dockerfile](../frontend/Dockerfile)) | Whole system in Docker for manual UI testing / QA |
| `seed` | `seed` (one-shot) | Loads the 1,200-request search dataset |
| `e2e` | app + seed + `e2e` (Playwright) | Acceptance suite; run through `tests/e2e/run-e2e.sh` as the isolated project `home-quiz-e2e` |

- **Ports:** set by `POSTGRES_PORT`, `REDIS_PORT`, `BACKEND_PORT` and `FRONTEND_PORT` (see `.env.example`).
- **Frontend image:** production Next.js build. The browser calls same-origin `/api/v1`, which Next proxies to `http://backend:8080`.
- **CORS:** the backend accepts a comma-separated `FRONTEND_ORIGIN` because proxied POST/PUT still carry the browser's `Origin`.
- **No cloud target:** none is provisioned; see the [runbook](../docs/operations/runbook.md).
