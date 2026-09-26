# Human approval gates

ทุก phase ใช้สถานะ Draft → In review → Changes requested หรือ Approved งานเปลี่ยน phase ได้เมื่อคนที่รับผิดชอบอนุมัติ artifact revision ที่ตรวจแล้ว AI สร้างหลักฐานและช่วย review ได้ แต่ไม่เป็นผู้อนุมัติงานของตนเอง

| Gate | Required approver | Artifact | Current status |
| --- | --- | --- | --- |
| Requirements | Product owner | requirements, scope, acceptance criteria, assumptions | Approved |
| Design | Technical owner | architecture, ADRs, contract, data/test plan | Approved for TASK-010 on `77b56241` |
| Implement | Code reviewer | code diff, implementation tests and task evidence | Approved for TASK-010 on `77b56241` |
| Verify | QA / acceptance owner | traceability, functional/reliability/performance evidence | Approved for TASK-010 on `77b56241` |
| Delivery | Release owner | release revision, runbook, rollback and limitations | Approved for `77b56241` with recorded limitations |

## Approval record

| Phase | Decision | Approver | Date | Revision / evidence | Notes |
| --- | --- | --- | --- | --- | --- |
| Requirements | Approved to proceed | Project owner (user) | 2026-09-25 | User request: “เริ่ม implement ตาม plan” | Approves implementation against the repository requirements and stated plan; unresolved assumptions must still be resolved in TASK-002 |
| Design | Approved to proceed | Project owner acting as technical owner | 2026-09-25 | [implementation plan](../delivery/implementation-plan.md) | TASK-level ADRs and contracts still require review when produced |
| Implement | Approved | Project owner acting as code reviewer | 2026-09-25 | [TASK-001 evidence](../quality/evidence/TASK-001.md) | User explicitly approved TASK-001; later implementation tasks still require review |
| Verify | Pending | QA / acceptance owner | — | — | Cannot approve before implementation review |
| Delivery | Pending | Release owner | — | — | Cannot approve before verification |
| Requirements | Reopened for review | Project owner (user) | 2026-09-25 | [Discover analysis](../product/discovery.md) | User explicitly selected Discover; Q-01 through Q-12 require approval before Design resumes |
| Requirements | Approved | Project owner (user) | 2026-09-25 | [Discover analysis](../product/discovery.md) | User approved requirement baseline and proposed defaults Q-01 through Q-12 |
| Design | Approved | Project owner acting as technical owner | 2026-09-25 | [ADR-002](../architecture/decisions/ADR-002-request-contract-and-domain.md), [OpenAPI](../../contracts/openapi.yaml), [data model](../architecture/data-model.md), [UI flow](../architecture/ui-flow.md) | User explicitly approved the TASK-002 Design package |
| Implement | Approved | Project owner acting as code reviewer | 2026-09-25 | [TASK-002 evidence](../quality/evidence/TASK-002.md) | User explicitly approved the TASK-002 domain, validation, tests and migration baseline |
| Design | Approved | Project owner acting as technical owner | 2026-09-25 | [ADR-003](../architecture/decisions/ADR-003-draft-vertical-slice.md), [TASK-003 test design](../quality/TASK-003-test-design.md) | User explicitly approved the TASK-003 draft vertical-slice design |
| Implement | Approved | Project owner acting as code reviewer | 2026-09-25 | [TASK-003 evidence](../quality/evidence/TASK-003.md) | User explicitly approved TASK-003 after review rounds 1–2, including the ADR-003 layer-wording amendment (no behavior or contract change) |
| Design | Approved | Project owner acting as technical owner | 2026-09-25 | [ADR-004](../architecture/decisions/ADR-004-approval-workflow.md), [TASK-004 test design](../quality/TASK-004-test-design.md) | User explicitly approved the TASK-004 approval-workflow design including decisions 1–5 in [design evidence](../quality/evidence/TASK-004-design.md) |
| Implement | Approved | Project owner acting as code reviewer | 2026-09-25 | [TASK-004 evidence](../quality/evidence/TASK-004.md) | User explicitly approved the TASK-004 approval-workflow implementation, tests and evidence |
| Design | Approved | Project owner acting as technical owner | 2026-09-25 | [ADR-005](../architecture/decisions/ADR-005-search-list.md), [TASK-005 test design](../quality/TASK-005-test-design.md) | User explicitly approved the TASK-005 search/list design including decisions 1–5 in [design evidence](../quality/evidence/TASK-005-design.md) |
| Implement | Approved | Project owner acting as code reviewer | 2026-09-25 | [TASK-005 evidence](../quality/evidence/TASK-005.md) | User explicitly approved the TASK-005 search/list implementation, tests, query-plan evidence and the documented debounce deviation |
| Design | Approved | Project owner acting as technical owner | 2026-09-25 | [ADR-006](../architecture/decisions/ADR-006-caching.md), [TASK-006 test design](../quality/TASK-006-test-design.md) | User explicitly approved the TASK-006 caching design including decisions 1–6 (reference-data endpoint, V2 departments migration, actuator) in [design evidence](../quality/evidence/TASK-006-design.md) |
| Implement | Approved | Project owner acting as code reviewer | 2026-09-25 | [TASK-006 evidence](../quality/evidence/TASK-006.md) | User explicitly approved the TASK-006 caching implementation, tests, two-instance evidence and the documented outage-latency and health limitations |
| Design | Approved | Project owner acting as technical owner | 2026-09-25 | [ADR-007](../architecture/decisions/ADR-007-performance-and-delivery.md), [TASK-007 test design](../quality/TASK-007-test-design.md) | User explicitly approved the k6 thresholds, workload, runs and delivery verification before any run, resolving A-06 |
| Implement | Approved | Project owner acting as code reviewer | 2026-09-25 | [TASK-007 evidence](../quality/evidence/TASK-007.md), [performance report](../../tests/performance/results/TASK-007-report.md) | User approved TASK-007 without deciding G-1 (list row actions) or H-1 (token in assignment HTML); both remain open for the Verify/Delivery owners |
| Design | Approved | Project owner acting as technical owner | 2026-09-25 | [TASK-008 packet](../delivery/tasks/TASK-008-e2e-acceptance.md) | User approved the Playwright tooling decisions together with the implementation |
| Implement | Approved | Project owner acting as code reviewer | 2026-09-25 | [TASK-008 evidence](../quality/evidence/TASK-008.md) | User approved the acceptance automation (49/49 tests, 53 of 55 AT cases); Verify still requires the QA / acceptance owner |
| Design | Approved | Project owner acting as technical owner | 2026-09-25 | [TASK-009 packet](../delivery/tasks/TASK-009-docker-ui-test.md) | User approved the single compose.yaml (project home-quiz), Docker images, same-origin proxy and multi-origin CORS |
| Implement | Approved | Project owner acting as code reviewer | 2026-09-25 | [TASK-009 evidence](../quality/evidence/TASK-009.md) | User approved the Docker UI-test stack (Playwright 49/49 in containers), including the disclosed Dockerfile overwrite and the old-project volumes left in place |
| Verify | Approved | Project owner acting as QA / acceptance owner | 2026-09-25 | [acceptance test cases](../quality/acceptance-test-cases.md), [TASK-008](../quality/evidence/TASK-008.md) and [TASK-009](../quality/evidence/TASK-009.md) evidence | User stated "QA Approved" for TASK-001 to TASK-009. No manual pass counts or G-1 decision were supplied; G-1 and H-1 remain open |
| Design / Implement / Verify | In review | Pending human reviewers | 2026-09-25 | [TASK-010](../delivery/tasks/TASK-010-list-row-actions.md), [ADR-005 amendment](../architecture/decisions/ADR-005-search-list.md), [TASK-010 evidence](../quality/evidence/TASK-010.md) | List-row behavior changed after the earlier approval; AT-33 now passes automated acceptance. New revision awaits review; this entry is not an approval |

หากแก้ artifact ที่ approved แล้วอย่างมีนัยสำคัญ ให้เปลี่ยน gate นั้นกลับเป็น In review และบันทึก approval ใหม่พร้อม revision/date ผู้อนุมัติคนเดียวรับหลายบทบาทได้ใน take-home project แต่ต้องระบุบทบาททุกครั้ง

Delivery review on 2026-09-25 found that the recorded Verify approval did not include an AT-33 P1 result or an explicit owner decision for its known list-row deviation. The historical approval entry above remains unchanged; [TASK-012 audit](../quality/evidence/TASK-012-delivery-audit.md) records the mismatch. TASK-010 implements the source criterion, subject to new Design/Implement/Verify review. Delivery remains Pending until the release owner reviews that result, H-1 handling and the final revision.

H-1 disposition, 2026-09-26: the source-storage owner (user) confirmed that the saved assignment download token is revoked. A subsequent workspace request to the saved link returned HTTP 403; [TASK-011 evidence](../quality/evidence/TASK-011.md) contains status-only observations. The original URL text remains in published Git history. This owner statement resolves the source-side status question but is not a Design, Implement, Verify or Delivery approval. The release owner still assesses the residual history and final candidate.

PR #10 merged on 2026-09-26 at `main` revision `96acc6411133ecccd3261c53473dc59f2ba1d1fc` by the repository owner. GitHub reported no submitted PR review or CI checks. This records integration only; TASK-010 Design/Implement/Verify and release-owner Delivery decisions remain pending. See the [post-merge TASK-013 evidence](../quality/evidence/TASK-013.md).

## Approval for merged REQ-13 revision

On 2026-09-26, after the post-merge [TASK-013 evidence](../quality/evidence/TASK-013.md) and limitations were presented, the user explicitly answered “Approve all listed gates” for merged `main` revision `77b56241a9eef817c62e34d4e949bfc6ecfdc9f7`. The approval question named TASK-010 Design, Implement and Verify, plus Delivery, and disclosed that manual AT-44/AT-46 and application rollback/data restore were not run and revoked token text remains in Git history. The user acts as technical owner, code reviewer, QA/acceptance owner and release owner for this take-home project. This decision is recorded for that revision; the documentation-only approval record that follows does not change application behavior.

| Phase | Decision | Approver | Date | Revision / evidence | Notes |
| --- | --- | --- | --- | --- | --- |
| Design | Approved | Project owner acting as technical owner | 2026-09-26 | `77b56241`; [ADR-005](../architecture/decisions/ADR-005-search-list.md), [TASK-010](../delivery/tasks/TASK-010-list-row-actions.md) | Accepts list-row action design and the Employee-owner-only PENDING Cancel interpretation |
| Implement | Approved | Project owner acting as code reviewer | 2026-09-26 | `77b56241`; [TASK-010 evidence](../quality/evidence/TASK-010.md) | Accepts merged UI implementation and recorded tests |
| Verify | Approved | Project owner acting as QA / acceptance owner | 2026-09-26 | `77b56241`; [acceptance cases](../quality/acceptance-test-cases.md), [TASK-013 evidence](../quality/evidence/TASK-013.md) | AT-33 and all 43 P1 cases have automated coverage; manual P2 AT-44/AT-46 remain NOT RUN |
| Delivery | Approved with stated limitations | Project owner acting as release owner | 2026-09-26 | `77b56241`; [TASK-013 evidence](../quality/evidence/TASK-013.md), [runbook](../operations/runbook.md) | Accepts GitHub `main` handoff, unrun manual P2 and rollback/data restore, and revoked token text remaining in Git history |
