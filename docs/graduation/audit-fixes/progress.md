# Audit Fixes Progress

## 2026-06-30

- Started P0/P1 audit-fix implementation.
- Ran `git status --short`; existing M1-M6 worktree changes are present and must be preserved.
- Created this tracking directory for the audit-fixes round.
- Added test-only JWT config in `src/test/resources/application.yml`.
- Updated `LoginInterceptor` to reject missing/disabled users with HTTP 403.
- Updated protected MockMvc tests to mock enabled users where required.
- Added disabled-user and self-like regression coverage in `ArticleControllerTest`.
- Updated `ArticleService.update()` to allow edits only for `DRAFT` and `REJECTED` articles.
- Added service tests for published article update rejection, allowed draft/rejected update, and non-author update rejection.
- Added Flyway V8 pending-report unique key and service-level duplicate-key conversion.
- Added database uniqueness regression coverage in `ContentReportServiceTest`.
- Focused test attempt 1 failed before test execution because test `application.yml` shadowed the main datasource config; expanded test config to include datasource, Redis, RabbitMQ, Flyway, and JWT test settings.
- Focused test attempt 2 passed:
  `mvn "-Dtest=ArticleControllerTest,ArticleServiceTest,ContentReportServiceTest" test`
  -> `Tests run: 44, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`.
  Flyway migrated local schema from V7 to V8 successfully.
- Full backend test passed:
  `mvn test`
  -> `Tests run: 118, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`.
- `git diff --check` passed with only existing Windows LF-to-CRLF warnings.
- Follow-up P2 fixes:
  - Wrapped `ArticleController.update/delete` in local `RuntimeException` handling so service business messages are returned through `Result.fail`.
  - Narrowed `LoginInterceptor`'s 401 catch block to JWT parsing only, so user lookup/status failures are no longer mislabeled as invalid tokens.
  - Added controller regression tests for update/delete service business errors.
- Follow-up P2 validation:
  `mvn "-Dtest=ArticleControllerTest" test`
  -> `Tests run: 8, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`.
  `mvn test`
  -> `Tests run: 120, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`.
  `git diff --check` passed with only existing Windows LF-to-CRLF warnings.
