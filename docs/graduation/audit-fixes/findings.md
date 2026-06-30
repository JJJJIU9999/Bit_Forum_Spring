# Audit Fixes Findings

## Baseline

- Current branch: `graduation-design`.
- Existing worktree has many uncommitted M1-M6 changes; this round must preserve them.
- `src/main/resources/application.yml` keeps `jwt.secret: ${JWT_SECRET}` with no production default.
- `LoginInterceptor` currently parses JWT and sets `userId` but does not check `User.status`.
- `AdminInterceptor` already checks both `User.status` and role, so its behavior is the model for normal login-protected endpoints.
- `ArticleService.update()` checks existence and ownership but not article status.
- `ContentReportService` checks duplicate pending reports in service code only; `V7__add_content_report.sql` has indexes but no unique constraint.
- `ArticleController.like()` calls Redis and notification service without blocking self-like.

## Decisions

- Test JWT secret belongs in test resources, not main application config.
- Published article deletion remains allowed for authors.
- P1-04 test infrastructure isolation and P1-05 rate limiting are deferred.
