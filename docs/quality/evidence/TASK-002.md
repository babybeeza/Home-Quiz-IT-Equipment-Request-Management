# Evidence: TASK-002 implementation

Date / operator: 2026-09-25 / Codex
Implement approval: Approved by project owner acting as code reviewer on 2026-09-25
Approved Design revision: `3f8cbb8`
Environment: Windows, host Temurin Java 25.0.3 targeting Java 21, PostgreSQL 17.11 in Docker
Requirement IDs: REQ-01, REQ-02, REQ-03, REQ-05, REQ-06, REQ-07, REQ-09

| Check | Exact command / method | Result | Observation |
| --- | --- | --- | --- |
| OpenAPI validation | `docker run --rm -v "${PWD}:/local" openapitools/openapi-generator-cli:v7.16.0 validate -i /local/contracts/openapi.yaml` | PASS | No validation issues detected |
| Backend clean package | `backend\\mvnw.cmd --batch-mode clean package` | PASS | Executable jar created; 13 tests passed |
| Domain tests after final email-limit alignment | `backend\\mvnw.cmd --batch-mode test` | PASS | 13 tests, 0 failures/errors/skips |
| Transition rules | `RequestStatusTest` | PASS | All 5 allowed transitions, active invalid transitions, every terminal/action combination and DRAFT-only edit covered |
| Access policy | `RequestAccessPolicyTest` | PASS | Employee owner/non-owner and Approver operations covered |
| Validation | `EquipmentRequestValidatorTest` with fixed Asia/Bangkok clock | PASS | Draft/submit, today/yesterday, nested field paths, exact/over limits and reject reason covered |
| Fresh migration | `docker compose up -d --wait`; `backend\\mvnw.cmd --batch-mode spring-boot:run` | PASS | Flyway migrated empty public schema to V1 on PostgreSQL 17.11 |
| Extension/index catalog | `psql` queries against `pg_extension` and `pg_indexes` | PASS | pg_trgm present; 10 PK/unique/filter/FK/trigram indexes listed |
| Database constraint + statement rollback | One data-modifying CTE inserted a parent and quantity=6 child | PASS | quantity check failed; parent count remained 0, proving statement rollback |
| Migration restart | Stop/start backend then query `flyway_schema_history` | PASS | history remained `1:1:true`; no duplicate migration |

## Scope notes

- TASK-002 implements the domain policy/validation baseline and database schema. JPA entities, repositories, application transactions and HTTP controllers are intentionally deferred to TASK-003/004 vertical slices.
- The SQL rollback check proves the database statement is atomic. Application transaction rollback and two-transaction optimistic locking require the entities/repository implemented in TASK-003.
- Cache implementation remains TASK-006. The database-authoritative boundary is defined but not claimed as implemented.

## Approval decision

The project owner explicitly approved the Implement gate after reviewing the domain code, tests, V1 migration and this evidence. TASK-003 may begin; project-level Verify and Delivery remain pending.
