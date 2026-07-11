# GestionCaravana

GestionCaravana is a local, self-contained caravan campaign manager.

It provides a Spring Boot backend and a Vue 3 frontend for working with the
active caravan context: create a caravan instance, select it as active, and
manage its travelers, wagons, beasts, and wagon improvements.

## Tech Stack

- Backend: Java 25, Spring Boot 4.1.0, Maven, Spring Web, Spring Data JPA, H2
- Frontend: Vue 3, Vite, TypeScript, Vue Router
- Persistence: embedded H2 database stored under `data/`

## Current Scope

The current implementation focuses on these gameplay areas:

- caravan instance creation, selection, listing, and deletion
- active caravan dashboard with summary stats
- wagon catalog browsing and wagon management
- traveler management with role and wagon assignment
- beast management with catalog-based and custom entries
- wagon improvements and derived wagon details

## Frontend Routes

- `/` — caravan dashboard
- `/travelers` — traveler management
- `/wagons` — wagon management
- `/beasts` — beast management

## Backend Architecture

The backend follows hexagonal architecture:

- `domain` — pure model and business rules
- `application/port/in` — inbound ports
- `application/port/out` — outbound ports
- `application/usecase` — use-case implementations
- `adapter/in/web` — HTTP adapters
- `adapter/out/persistence` — persistence adapters and wiring

Keep the domain free from Spring, web, JPA, and filesystem concerns.

## Repository Layout

- `src/main/java` — backend application code
- `src/test/java` — backend tests and architecture boundaries
- `frontend/` — Vue application
- `docs/` — architecture notes and supporting documentation
- `openspec/` — feature specifications
- `data/` — local H2 database files

## Run the Project

### Backend

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

The backend binds to `0.0.0.0:8080` by default, so it is reachable from the
local network through the machine IP, for example `http://192.168.1.201:8080`.

### Frontend

```powershell
cd frontend
npm install
npm run dev
```

The Vite dev server binds to `0.0.0.0:5173` by default, so the UI is reachable
from other devices on the LAN, for example `http://192.168.1.201:5173`.

### Running two instances at the same time

The application now reads the backend port and H2 file path from environment
variables, and the frontend dev proxy target and port are configurable as well.
This instance defaults to `8082/5175`.

Use separate terminals for each backend/frontend pair.

#### App1

Backend:

```powershell
$env:SERVER_PORT = "8082"
$env:APP_DATA_PATH = "./data/gestion-caravana-app1"
.\mvnw.cmd spring-boot:run
```

Frontend:

```powershell
cd frontend
$env:VITE_DEV_PORT = "5175"
$env:VITE_API_PROXY_TARGET = "http://localhost:8082"
npm run dev
```

#### App2

Backend:

```powershell
$env:SERVER_PORT = "8083"
$env:APP_DATA_PATH = "./data/gestion-caravana-app2"
.\mvnw.cmd spring-boot:run
```

Frontend:

```powershell
cd frontend
$env:VITE_DEV_PORT = "5176"
$env:VITE_API_PROXY_TARGET = "http://localhost:8083"
npm run dev
```

The H2 consoles stay attached to their respective backend ports:

- App1: `http://localhost:8082/h2-console`
- App2: `http://localhost:8083/h2-console`

### Frontend checks

```powershell
cd frontend
npm run build
npm run typecheck
```

## Quality Checks

- `.\mvnw.cmd -Dtest=ArchitectureBoundariesTest test`
- `.\mvnw.cmd -Dtest=GestionCaravanaApplicationTests test`
- `cd frontend && npm run typecheck`

## Reference Documentation

- `AGENTS.md`
- `docs/architecture.md`
- `openspec/README.md`
