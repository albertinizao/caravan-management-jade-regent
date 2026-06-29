# Agent Instructions

## Package Manager
Use **Maven** for the backend and **npm** for the frontend.

- Backend: `.\mvnw.cmd test`, `.\mvnw.cmd spring-boot:run`
- Frontend: `cd frontend && npm install`, `npm run dev`, `npm run build`, `npm run typecheck`

## File-Scoped Commands
| Task | Command |
|------|---------|
| Backend test class | `.\mvnw.cmd -Dtest=ArchitectureBoundariesTest test` |
| Backend app test | `.\mvnw.cmd -Dtest=GestionCaravanaApplicationTests test` |
| Frontend typecheck | `cd frontend && npm run typecheck` |

## Architecture
- Backend follows hexagonal architecture.
- Keep `domain` free of Spring, web, JPA, and filesystem imports.
- Put inbound ports in `application/port/in`.
- Put outbound ports in `application/port/out`.
- Put use-case implementations in `application/usecase`.
- Put HTTP adapters in `adapter/in/web`.
- Put persistence and wiring adapters in `adapter/out/*`.
- Keep ArchUnit rules in tests whenever a new package boundary appears.
- Frontend uses Vue 3 + Vite + TypeScript.
- Shared UI lives in `frontend/src/components`.
- Route views live in `frontend/src/views`.
- API clients live in `frontend/src/services`.
- Shared types live in `frontend/src/types`.

## Commit Attribution
Do not add `Co-Authored-By` or any AI attribution. Use conventional commits only.

## References
- `README.md`
- `docs/architecture.md`
