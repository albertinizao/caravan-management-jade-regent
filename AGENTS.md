# Agent Instructions

## Project Snapshot
- Monorepo for **GestionCaravana**.
- Backend: **Java 25**, **Spring Boot 4.1.0**, **Maven**.
- Frontend: **Node 22.x**, **Vue 3**, **Vite**, **TypeScript**.
- Persistence: **local H2 file database** under `data/`.
- Current product area: caravan campaign management with an active caravan dashboard plus views for **Travelers**, **Wagons**, and **Beasts**.

## Package Manager
Use **Maven** for the backend and **npm** for the frontend.

- Backend:
  - `.\mvnw.cmd test`
  - `.\mvnw.cmd spring-boot:run`
- Frontend:
  - `cd frontend && npm install`
  - `cd frontend && npm run dev`
  - `cd frontend && npm run build`
  - `cd frontend && npm run typecheck`

## File-Scoped Commands
| Task | Command |
|------|---------|
| Backend architecture boundary test | `.\mvnw.cmd -Dtest=ArchitectureBoundariesTest test` |
| Backend app smoke test | `.\mvnw.cmd -Dtest=GestionCaravanaApplicationTests test` |
| Frontend typecheck | `cd frontend && npm run typecheck` |

## Architecture
- Backend follows **hexagonal architecture**.
- Keep `domain` free of Spring, web, JPA, filesystem, and infrastructure imports.
- Put inbound ports in `application/port/in`.
- Put outbound ports in `application/port/out`.
- Put use-case implementations in `application/usecase`.
- Put HTTP adapters in `adapter/in/web`.
- Put persistence and wiring adapters in `adapter/out/*`.
- Keep ArchUnit rules in tests whenever a new package boundary appears.
- Frontend uses **Vue 3 + Vite + TypeScript**.
- Route views live in `frontend/src/views`.
- Shared UI and composables live in `frontend/src/components` and `frontend/src/composables`.
- API clients live in `frontend/src/services`.
- Shared types live in `frontend/src/types`.

## Current Backend Shape
- Core domain concepts currently present:
  - caravans and active caravan selection
  - travelers, traveler roles, contracts, wagon assignment
  - wagons, wagon limits, wagon improvements, wagon catalog
  - beasts, beast assignments, beast catalog
  - caravan stats, campaign status, and related catalogs
- HTTP endpoints live in `adapter/in/web`.
- Persistence adapters live in `adapter/out/persistence`.
- Default persistence is local H2 stored under `data/`; do not introduce an external database unless the requirement changes.

## Current Frontend Shape
- Routes currently present:
  - `/` → caravan dashboard
  - `/travelers`
  - `/wagons`
  - `/beasts`
- The shell uses a shared top navigation and a global toast composable.
- The dashboard is the entry point for creating, selecting, and deleting caravan instances and for surfacing the currently active campaign context.

## Commit Attribution
Do not add `Co-Authored-By` or any AI attribution. Use conventional commits only.

## References
- `README.md`
- `docs/architecture.md`
- `openspec/README.md`
