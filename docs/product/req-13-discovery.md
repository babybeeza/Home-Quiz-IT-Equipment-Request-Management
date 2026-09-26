# REQ-13 Discover: delivery package

Status: Source clarification prepared 2026-09-26; existing REQ-13 baseline remains approved
Source: original [assignment](../../Home-Quiz-IT-Equipment-Request-Management_revise_1.html), §10 Submission, especially lines 736–800

## Facts from the assignment

The submission is a Git repository that another person can install, test and run. Its required contents are frontend and backend source, `README.md`, a database migration script **or** schema, and automated tests. The README must cover:

| Area | Required information |
| --- | --- |
| Environment | Prerequisites and Node.js/JDK versions |
| Run | Install/start frontend and backend; prepare the database |
| Test & API | Run tests and find API documentation |
| Decisions | Architecture decisions and state-management approach |
| Scope | Assumptions and known limitations |

An API collection such as Postman/Bruno and Docker Compose are explicitly bonus deliverables. The example repository tree is illustrative; it does not require a particular file or folder name. The assignment's §6 Database Expectations separately says production migration cannot rely on `ddl-auto=create`; Flyway/Liquibase is bonus or a Senior-level requirement. REQ-13 does not demand cloud deployment, a public repository, a particular Git host, or an API collection.

## Observable acceptance criteria

1. A reviewer receives an identified Git revision containing the frontend/backend source, README, migration or schema, and automated tests. The revision and access method must be clear enough for the reviewer to obtain it.
2. From a clean checkout and the documented prerequisites, the reviewer can follow the README to prepare the database, install and start both applications, and run the documented tests. Failures or environment dependencies are reported rather than counted as passes.
3. The README states all five information areas above and links to current API documentation, decisions, assumptions and limitations. Commands match the repository's manifests and wrappers.
4. Optional API collection and Docker Compose are reported separately from required items. A missing bonus does not fail REQ-13.

These criteria concern a reviewable handoff. They do not imply that the current uncommitted working tree is a release revision or that prior test results cover later changes.

## Current repository observations (not source requirements)

| Item | Observation / evidence boundary |
| --- | --- |
| Source and README | `frontend/`, `backend/` and root `README.md` are present. The README has Environment, Run, Test, API, Decisions, Assumptions and Known limitations sections. |
| Database artifact | Flyway V1 and V2 migration files are present; the clean-start result is recorded in [TASK-007 evidence](../quality/evidence/TASK-007.md). |
| Automated tests | Frontend/backend tests and Playwright are present. TASK-010 records a later frontend check and 50/50 browser run in [its evidence](../quality/evidence/TASK-010.md); backend results belong to the earlier TASK-009 revision. |
| API and bonuses | [OpenAPI](../../contracts/openapi.yaml) is present; the README says no API collection. [Compose](../../compose.yaml) is present and its Docker UI path has [TASK-009 evidence](../quality/evidence/TASK-009.md). |
| Release handoff | Current HEAD was `8a09c4d` when checked on 2026-09-26, with uncommitted changes. A GitHub `origin` remote is configured, but it has not been designated or access-checked as the reviewer handoff. No final release revision or release owner decision is recorded. Delivery remains pending. |

This Discover pass ran on Windows PowerShell on 2026-09-26. It did not modify the assignment HTML.

| Check | Command / method | Exit code | Result |
| --- | --- | --- | --- |
| Artifact inventory | `Test-Path` for the two component READMEs, Flyway V1/V2, OpenAPI, E2E README, k6 report and Compose file | 0 | All eight paths returned `True` |
| Revision, remote and working tree | `git rev-parse --short HEAD`; `git remote -v`; `git status --porcelain` | 0 | HEAD `8a09c4d`; GitHub `origin` configured; working tree has changes, so no final release revision is claimed |
| Product-document links and patch whitespace | PowerShell local-link `Test-Path` check; `git diff --check` | 0 | Local links resolved and no whitespace errors |
| Clean-checkout rehearsal and full test suite | — | N/A | **NOT RUN** for this documentation-only Discover pass; earlier results are cited above |

## Assumptions and questions

- **Source fact:** The repository must be installable, testable and runnable by someone else. The assignment does not specify the Git host, visibility or exact handoff revision.
- **Open handoff decision A-08:** A GitHub `origin` exists, but the release owner must confirm whether it is the handoff location, how the reviewer will access it, and which immutable revision to review. This is a delivery decision, not a new application feature.
- **Separate hygiene issue H-1:** The assignment HTML contains a saved-from URL with a token. [TASK-011](../delivery/tasks/TASK-011-assignment-token.md) records two HTTP 403 observations; source-owner confirmation remains pending. This is a repository hygiene finding, not an additional REQ-13 clause in the assignment.

There is no unresolved product-behavior question in §10 that blocks the existing implementation. The release handoff and H-1 require owner decisions before claiming Delivery completion.
