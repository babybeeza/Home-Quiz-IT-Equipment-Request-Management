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

The exact committed URL did not grant access from this workspace at the time of these checks. H-1 remains open for the source-storage owner to decide whether access is intended and confirm the token's state at the source. No external settings, repository history or assignment content were changed.
