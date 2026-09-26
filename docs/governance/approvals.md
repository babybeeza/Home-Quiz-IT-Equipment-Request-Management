# Human approval gates

ทุก phase ใช้สถานะ Draft → In review → Changes requested หรือ Approved งานเปลี่ยน phase ได้เมื่อคนที่รับผิดชอบอนุมัติ artifact revision ที่ตรวจแล้ว AI สร้างหลักฐานและช่วย review ได้ แต่ไม่เป็นผู้อนุมัติงานของตนเอง

| Gate | Required approver | Artifact | Current status |
| --- | --- | --- | --- |
| Requirements | Product owner | requirements, scope, acceptance criteria, assumptions | Approved; REQ-14 baseline approved 2026-09-26 |
| Design | Technical owner | architecture, ADRs, contract, data/test plan | TASK-010 approved on `77b56241`; REQ-14 ADR-008 approved on `5a3b2e1` |
| Implement | Code reviewer | code diff, implementation tests and task evidence | TASK-010 approved on `77b56241`; REQ-14 approved on `5a3b2e1` |
| Verify | QA / acceptance owner | traceability, functional/reliability/performance evidence | TASK-010 approved on `77b56241`; REQ-14 approved with limits on `5a3b2e1` |
| Delivery | Release owner | release revision, runbook, rollback and limitations | `77b56241` approved with limits; REQ-14 candidate `5a3b2e1` approved with limits, merged, and redeployed to local `home-quiz-release` from `5597476` |

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

## REQ-14 theme review

On 2026-09-26 the user requested Implement after supplying the Spark Deck palette and planning TASK-014/TASK-015. [ADR-008](../architecture/decisions/ADR-008-spark-theme.md), [TASK-014 evidence](../quality/evidence/TASK-014-design.md) and [TASK-015 evidence](../quality/evidence/TASK-015.md) were presented in draft PR #12 at application revision `5a3b2e107e30ba90f1b36e5ea1b2f587c955c4c9`. The handoff explicitly listed Design, Implement, Verify and Delivery as pending; the user replied “approved.” The user acts as technical owner, code reviewer, QA/acceptance owner and release owner for this take-home project. This accepts the stated limits: backend unit/integration and manual AT-44/AT-46 were not run, exhaustive rendered-pixel audit was not run, and the original deck file was not supplied. The approval applies to that application revision; this documentation-only record does not change UI behavior. Formal REQ-14 Requirements baseline approval was not among the four listed gates. The earlier REQ-13 approvals remain scoped to `77b56241`.

| Phase | Decision | Approver | Date | Revision / evidence | Notes |
| --- | --- | --- | --- | --- | --- |
| Design | Approved | Project owner acting as technical owner | 2026-09-26 | `5a3b2e1`; [ADR-008](../architecture/decisions/ADR-008-spark-theme.md), [design evidence](../quality/evidence/TASK-014-design.md) | Accepts semantic token mapping, contrast pairs and named derived colors |
| Implement | Approved | Project owner acting as code reviewer | 2026-09-26 | `5a3b2e1`; [TASK-015 evidence](../quality/evidence/TASK-015.md) | Accepts CSS-only implementation and recorded checks |
| Verify | Approved with stated limitations | Project owner acting as QA / acceptance owner | 2026-09-26 | `5a3b2e1`; [TASK-015 evidence](../quality/evidence/TASK-015.md) | 32 unit and 50 browser tests passed; listed unrun checks accepted |
| Delivery | Approved for reviewed candidate with stated limitations | Project owner acting as release owner | 2026-09-26 | `5a3b2e1`; [PR #12](https://github.com/babybeeza/Home-Quiz-IT-Equipment-Request-Management/pull/12) | Candidate handoff approved; this does not assert merge, deployment or change to the separate running release stack |

Post-merge integration, 2026-09-26: [PR #12](https://github.com/babybeeza/Home-Quiz-IT-Equipment-Request-Management/pull/12) merged into `main` at `a69a9506f3b42472752e135e6c375ab971a1ffd7`. [Post-merge evidence](../quality/evidence/TASK-015-postmerge.md) confirms that approved application revision `5a3b2e1` and approval record `9056657` are ancestors of merged `main`, with no further theme source change. This records integration, not a new approval or deployment. The earlier `home-quiz-release` stack still serves the prior release at default ports.

## REQ-14 Requirements baseline and local release redeploy

On 2026-09-26 the user answered “Requirements baseline ของ REQ-14: อนุมัติ” and “Release stack re deploy” after being told that the REQ-14 baseline was not among the four approved gates and that the default-port `home-quiz-release` stack still served the pre-theme release. The user acts as product owner and release owner for this take-home project. The baseline approval covers [REQ-14 Discover](../product/req-14-theme-discovery.md) as merged in `main` `55974760a06db2f038ce7159592830ce36199f69`.

| Phase | Decision | Approver | Date | Revision / evidence | Notes |
| --- | --- | --- | --- | --- | --- |
| Requirements | Approved | Project owner acting as product owner | 2026-09-26 | `5597476`; [REQ-14 Discover](../product/req-14-theme-discovery.md) | Approves the REQ-14 baseline recorded as not yet approved in the earlier REQ-14 entry |
| Delivery | Redeploy requested and performed | Project owner acting as release owner | 2026-09-26 | `5597476`; [release deploy evidence](../quality/evidence/TASK-015-release-deploy.md) | Local Docker stack only; frontend and backend images rebuilt, database and Redis volumes kept. Not a cloud deployment |

## TASK-016 framework error statuses

On 2026-09-26 the user answered “อนุมัติ” to the TASK-016 handoff asking for approval of design decisions D1–D3 (one `ErrorResponse`-based path, the listed codes, `Allow` header and OpenAPI description).

| Phase | Decision | Approver | Date | Revision / evidence | Notes |
| --- | --- | --- | --- | --- | --- |
| Design | Approved | Project owner acting as technical owner | 2026-09-26 | [TASK-016](../delivery/tasks/TASK-016-framework-error-status.md) D1–D3 | Implement, Verify and Delivery pending |

On 2026-09-26, after the R1/R2 fixes and their evidence were presented and [PR #16](https://github.com/babybeeza/Home-Quiz-IT-Equipment-Request-Management/pull/16) merged into `main` at `c7cadf3778350badecf5ff9e348b40a0089c9a9e`, the user answered “อนุมัติ gate Implement, Verify และ Delivery ของ TASK-016”. The approval covers the merged revision, whose application content matches reviewed head `5372128`. It accepts the stated limits: frontend not rerun after the backend-only R1/R2 fix, R3 (pre-existing, unverified) open, and the local `home-quiz-release` stack not redeployed with TASK-016.

| Phase | Decision | Approver | Date | Revision / evidence | Notes |
| --- | --- | --- | --- | --- | --- |
| Implement | Approved | Project owner acting as code reviewer | 2026-09-26 | `c7cadf3`; [TASK-016 evidence](../quality/evidence/TASK-016.md) | Includes the D1 mechanism deviation and the R1 refinement that 406 has no body |
| Verify | Approved with stated limitations | Project owner acting as QA / acceptance owner | 2026-09-26 | `c7cadf3`; backend 135/135, Playwright 50/50, live 404/405/406/415 | Frontend 32/32 from the pre-fix revision; no frontend change since |
| Delivery | Approved with stated limitations | Project owner acting as release owner | 2026-09-26 | `c7cadf3` on GitHub `main` | Repository handoff only; local release stack not redeployed; R3 open |

Local release redeploy, 2026-09-26: at the user's request, `home-quiz-release` was rebuilt from `main` `5392a55` so the running stack includes TASK-016. See [TASK-016 evidence](../quality/evidence/TASK-016.md). This records an operation, not a new approval.

## TASK-017 non-JSON Accept on mutations

On 2026-09-26, after the [TASK-017 evidence](../quality/evidence/TASK-017.md) confirmed R3 and recommended class-level `produces = application/json`, the user answered “อนุมัติ”.

| Phase | Decision | Approver | Date | Revision / evidence | Notes |
| --- | --- | --- | --- | --- | --- |
| Design | Approved | Project owner acting as technical owner | 2026-09-26 | [TASK-017 evidence](../quality/evidence/TASK-017.md) recommendation | Covers `EquipmentRequestController` and `ReferenceDataController`, plus regression tests at MockMvc and Testcontainers level; Implement, Verify and Delivery pending |

On 2026-09-26, after the TASK-017 fix evidence was presented and [PR #20](https://github.com/babybeeza/Home-Quiz-IT-Equipment-Request-Management/pull/20) merged into `main` at `cae008f99cf3009cc19e8bf56c0c40ad58d43adb`, the user answered “อนุมัติ” to the handoff that listed the TASK-017 Implement, Verify and Delivery gates. The approval covers the merged revision, whose application content matches reviewed head `1adcd71`. It accepts the stated limits: frontend unit/build not rerun (no frontend change) and the local `home-quiz-release` stack not redeployed.

| Phase | Decision | Approver | Date | Revision / evidence | Notes |
| --- | --- | --- | --- | --- | --- |
| Implement | Approved | Project owner acting as code reviewer | 2026-09-26 | `cae008f`; [TASK-017 evidence](../quality/evidence/TASK-017.md) | Class-level `produces` and regression tests |
| Verify | Approved with stated limitations | Project owner acting as QA / acceptance owner | 2026-09-26 | `cae008f`; backend 142/142, Playwright 50/50, live rerun | Frontend unit/build NOT RUN (no change) |
| Delivery | Approved with stated limitations | Project owner acting as release owner | 2026-09-26 | `cae008f` on GitHub `main` | Repository handoff; local release stack not redeployed |
