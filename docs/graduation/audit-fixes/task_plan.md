# Audit Fixes Task Plan

## Goal

Fix the P0/P1 audit items selected for this round without changing unrelated M1-M6 work or committing to Git.

## Scope

- Add test-only JWT configuration.
- Reject disabled users in login-protected endpoints.
- Prevent direct edits to published, pending, or offline articles.
- Add database-backed pending report de-duplication.
- Prevent users from liking their own articles.
- Keep author deletion of published articles unchanged.

## Progress

- [x] Planning docs created.
- [x] JWT test config added.
- [x] LoginInterceptor status check added.
- [x] Article update status rule added.
- [x] Pending report uniqueness migration added.
- [x] Self-like rule added.
- [x] Focused tests pass.
- [x] Full backend tests pass or blocker recorded.

## Validation Commands

```powershell
mvn "-Dtest=ArticleControllerTest,ArticleServiceTest,ContentReportServiceTest" test
mvn test
git diff --check
```
