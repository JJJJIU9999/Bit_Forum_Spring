# Phase 2 React Frontend Task Plan

## Goal

Build a small React + Vite frontend demo for the Bit Forum Spring Boot backend, while keeping the learning path explicit and incremental.

This directory is the frontend-specific planning area. Do not use `docs/phase2-plan` for frontend progress.

## Current Branch

```text
phase-2-react-frontend
```

## Scope

Frontend demo features:

- Register and login.
- Store and attach JWT token for protected requests.
- View article list and article detail.
- Publish, update, and delete own articles where useful for demo.
- Like, unlike, comment, and view hot articles.
- Use admin token to access article, comment, and user management pages.

Out of scope for this learning phase:

- Complex state management libraries.
- Full UI component library.
- Production authentication hardening.
- Advanced routing guards beyond a simple demo flow.
- Replacing backend permission checks with frontend-only checks.

## Phases

| Phase | Status | Target | Verification |
| --- | --- | --- | --- |
| 1. React + Vite skeleton | Complete | `frontend` project can run and show a local page | Browser opens `http://localhost:5173/` |
| 2. Explain frontend structure | Complete | Understand `package.json`, `index.html`, `main.jsx`, `App.jsx`, CSS | User can describe page load flow |
| 3. Axios request layer | Complete | Add `request.js` and API modules | Frontend build passes |
| 4. Login and register pages | Complete | Register, login, save token | Browser login succeeded and token was saved |
| 5. Article list/detail/publish | Complete | Show articles, detail, publish article | New article appears after publish |
| 6. Like/comment/hot articles | Complete | Complete user interaction demo | Like, comment, hot list work |
| 7. Admin pages | Complete | Manage articles, comments, users | Admin token works, normal user is rejected |
| 8. README/demo notes | Complete | Document frontend startup and demo flow | README has concise frontend instructions |

## Decisions

- Put frontend code in `frontend/` under the Spring Boot project root.
- Use React + Vite for the frontend demo.
- Keep the first version simple: pages and API modules before reusable abstractions.
- Use `localStorage` for demo token storage, with a note that this is not a complete production security model.
- Keep backend authority as the source of truth. Frontend button hiding is only user experience.

## Risks

| Risk | Impact | Mitigation |
| --- | --- | --- |
| Mixing backend and frontend planning docs | Hard to recover learning context | Use only this directory for frontend progress |
| Overbuilding UI too early | Slows learning and hides data flow | Build one small feature at a time |
| Treating frontend checks as permissions | Security misunderstanding | Always point back to backend interceptors |
| Cross-origin request failure | Frontend cannot call backend | Configure Vite proxy or backend CORS when needed |

## Next Step

Frontend learning route is complete. Next step is to review Git changes and prepare a clean commit.
