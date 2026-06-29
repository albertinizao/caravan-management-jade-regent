# GestionCaravana

Monorepo skeleton for a hexagonal backend and a lightweight frontend.

## Stack

- Backend: Java 25, Spring Boot 4.1.0, Maven
- Frontend: Node 22, Vue 3, Vite, TypeScript

## Architecture

- Domain logic stays inside `src/main/java/com/gestioncaravana/domain`
- Use cases live in `src/main/java/com/gestioncaravana/application`
- Inbound adapters live in `src/main/java/com/gestioncaravana/adapter/in`
- Outbound adapters live in `src/main/java/com/gestioncaravana/adapter/out`
- Frontend app shell lives in `frontend/src`

## Repository Layout

```text
.
├─ AGENTS.md
├─ README.md
├─ docs/
│  └─ architecture.md
├─ src/
│  ├─ main/java/com/gestioncaravana/
│  │  ├─ domain/
│  │  ├─ application/
│  │  │  ├─ port/in/
│  │  │  ├─ port/out/
│  │  │  └─ usecase/
│  │  └─ adapter/
│  │     ├─ in/web/
│  │     └─ out/
│  └─ main/resources/application.yml
└─ frontend/
   ├─ index.html
   ├─ package.json
   ├─ src/
   └─ vite.config.ts
```

## Commands

### Backend

- `.\mvnw.cmd test`
- `.\mvnw.cmd spring-boot:run`

### Frontend

- `cd frontend && npm install`
- `cd frontend && npm run dev`
- `cd frontend && npm run build`
- `cd frontend && npm run typecheck`

## Architectural Choice

Spring Boot 4.1.0 is selected to keep Java 25 as the target runtime.
