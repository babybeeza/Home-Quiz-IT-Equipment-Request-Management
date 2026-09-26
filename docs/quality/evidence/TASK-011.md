# Evidence: TASK-011 assignment download token

Date / operator: 2026-09-25 / Codex
Revision: working tree based on `8a09c4d`; original assignment HTML unchanged
Environment: Windows PowerShell; outbound HTTPS from the workspace
Requirement: REQ-13 delivery hygiene / H-1

The saved-from URL in line 2 of the authoritative assignment HTML was parsed in memory. Its host was `firebasestorage.googleapis.com`, its path used the object endpoint, and its query had `alt` and `token` keys. The URL and token values were not printed or copied into this evidence.

| Check | Method | Exit code | Result | Observation |
| --- | --- | --- | --- | --- |
| URL structure | Read line 2, extract and parse the HTTPS URL in memory; print only host and query-key names | 0 | PASS | Firebase Storage object URL; token parameter present; no markup suffix in parsed URL |
| First HTTP attempt | PowerShell `System.Net.Http.HttpClient` without loading its assembly | 1 | NOT RUN | This PowerShell session did not auto-load `System.Net.Http`; no HTTP request was made |
| Direct download check | Load `System.Net.Http`, then `HttpClient.GetAsync(parsedUri, ResponseHeadersRead)`; dispose response without reading file content | 0 | PASS for observation | HTTP 403, JSON response, 73-byte content length |
| Repeat download check | `HttpClient.GetAsync(parsedUri)` and inspect only HTTP status plus JSON error code | 0 | PASS for observation | HTTP 403, JSON error code 403 |
| Source storage permissions and token revocation | Storage-owner console or equivalent source-side controls | N/A | NOT RUN | No connected source-storage administration access. A 403 here does not establish global revocation or intended sharing policy |
| Published repository exposure | In an independent clone of `codex/req13-candidate-20260926` at `236a2b3`, check only that the authoritative HTML exists and line 2 contains a `token` query key; do not print the URL or value | 0 | PASS for observation | The clone was fetched over HTTPS with Git credential helpers disabled. The HTML and token text are obtainable from this repository; token validity and source-side permissions remain unknown |
| Source-owner disposition | User's 2026-09-26 response to the TASK-011 status question | N/A | CONFIRMED BY OWNER | “I confirmed it is revoked.” No token value or storage-admin output was provided; prior intended access policy was not stated |
| Post-confirmation saved-link check | Parse the existing HTML line 2 in memory; `HttpClient.GetAsync` with `ResponseHeadersRead`; print only HTTP status | 0 | PASS for observation | HTTP 403 from this workspace on 2026-09-26; no response body or URL printed |

The exact committed URL did not grant access from this workspace at the time of these checks. The source-storage owner later confirmed that the token is revoked; the post-confirmation request again returned HTTP 403. This records owner attestation plus external reachability, not an independent inspection of storage controls. No external settings, repository history or assignment content were changed by Codex.

## Deliver and learn, 2026-09-26

The independent handoff clone shows that people who can fetch the published repository can obtain the original HTML and its saved URL. The earlier HTTP 403 observations alone did not prove revocation; the subsequent source-owner confirmation supplies the decision. Residual published history contains the now-revoked URL text. The release owner can assess that residual limitation at Delivery. Editing the current HTML alone would not remove the value from Git history, and the authoritative assignment is not changed by this task.
