# Architecture

## Goal

Provide a clean hexagonal skeleton that can grow without coupling domain code to Spring or the frontend stack.

## Backend Layers

### Domain

- Pure model and business rules
- No Spring annotations
- No web, persistence, or filesystem dependencies

### Application

- Use-case orchestration
- Inbound and outbound ports
- Mapping between domain objects and boundary DTOs

### Adapters

- `adapter/in/web`: HTTP controllers and request/response mapping
- `adapter/out/*`: persistence, external services, configuration, and other integrations

### Persistence

- Use embedded H2 as the default persistence engine.
- Store the database file under `./data` so the application remains self-contained.
- No external database service is required for the current local-network deployment.

## Frontend Layout

- `src/views`: route-level screens
- `src/components`: shared UI pieces
- `src/services`: API clients and HTTP helpers
- `src/types`: shared TypeScript contracts

## Decision

- Spring Boot 4.1.0 is used to align with the Java 25 target.
- H2 is used because the app runs locally and should not depend on external infrastructure.
- Vue 3 + Vite + TypeScript is used for the frontend because the reference projects already use that stack cleanly.
