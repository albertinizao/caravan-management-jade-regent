# Caravan Campaign Instance

**Status:** Draft  
**Target:** GestionCaravana  
**Type:** Feature specification

## 1. Summary

The application must allow the user to create and select a **caravan instance**: a persistent gameplay campaign context where all caravan-related rules, entities, and progression are managed from a single active instance.

In this domain, a caravan is not just a vehicle or a group of travelers. It is the **campaign container** for the complete flow: wagons, travelers, beasts, feats, resources, discontent, travel state, and future gameplay interactions.

## 2. Problem Statement

The user needs a clear way to choose **which caravan is currently being managed**. Without an active caravan context, the rest of the gameplay model is ambiguous because:

- statistics belong to a specific caravan instance,
- travelers and beasts are attached to a specific caravan,
- feats and resources must be evaluated against one active campaign,
- UI actions need a stable working context.

## 3. Goals

1. Allow creating a caravan campaign instance.
2. Allow selecting one caravan as the current working context.
3. Expose the full caravan gameplay surface from that context.
4. Initialize the caravan with the rule-based default state.
5. Support future growth without coupling the domain to UI or persistence details.

## 4. Non-Goals

This first specification does **not** define:

- combat resolution rules,
- detailed wagon combat implementation,
- complete traveler recruitment workflows,
- full beast stat blocks,
- leveling balance beyond the caravan rules already documented,
- multiplayer synchronization,
- cloud synchronization.

Those can be added in later specs once the caravan aggregate exists.

## 5. User Stories

### US-1: Create a caravan

As a user, I want to create a caravan campaign so that I can start managing a new game instance.

### US-2: Select the active caravan

As a user, I want to select one caravan as the current working instance so that every later action applies to the correct campaign.

### US-3: Access the full caravan flow

As a user, I want the selected caravan to expose all its sections from one place so that I can manage the complete campaign without jumping between unrelated screens.

### US-4: Resume a caravan later

As a user, I want my caravan selection to persist so that I can continue working on the same campaign after returning to the app.

## 6. Functional Requirements

### 6.1 Caravan creation

- The system must let the user create a new caravan instance.
- The user must provide at least a caravan name.
- The created caravan must receive a unique identifier.
- The created caravan must be stored persistently.
- The created caravan must be selectable immediately after creation.

### 6.2 Caravan selection

- The system must present a list of existing caravans.
- The user must be able to mark one caravan as the active caravan.
- The application must remember the active caravan for the session and restore it on reload when possible.
- If the saved active caravan no longer exists, the system must fall back to the caravan list.

### 6.3 Default caravan state

When created, the caravan must start with the rule-based initial state:

- Level: `1`
- Main stats:
  - Offense: `1`
  - Defense: `1`
  - Mobility: `1`
  - Morale: `1`
- Unassigned main stat points: `3`
- Discontent: `0`
- No wagons yet
- No travelers yet
- No beasts yet
- No feats selected yet

The caravan must be ready to receive all later components without requiring re-creation.

### 6.4 Full caravan working context

Once a caravan is selected, the application must expose the complete management surface for that caravan:

- Overview
- Statistics
- Wagons
- Travelers
- Beasts
- Feats
- Resources / inventory
- Travel and survival state
- Notes or log entries if supported by the UI

The selected caravan must act as the scope for all related actions.

### 6.5 Persistence and recovery

- Caravan data must survive application restarts.
- The current selection must be recoverable when the app is reopened.
- The system must allow multiple caravans to exist concurrently.

## 7. Domain Model

### 7.1 Aggregate root

`CaravanCampaign`

The caravan campaign is the aggregate root and the unit of consistency.

### 7.2 Core attributes

- `id`
- `name`
- `description` optional
- `level`
- `mainStats`
- `derivedStats`
- `discontent`
- `status`
- `createdAt`
- `updatedAt`

### 7.3 Associated components

The campaign owns or references:

- wagons,
- travelers,
- beasts of burden,
- feats,
- cargo/resources,
- travel state,
- daily consumption state,
- survival flags,
- history/log entries.

### 7.4 State model

Suggested lifecycle:

- `ACTIVE`
- `ARCHIVED`

The feature only requires `ACTIVE` and `ARCHIVED` for now. A draft state is not necessary unless later UX needs it.

## 8. UX / Flow

### 8.1 First visit

1. The user opens the app.
2. If there is no active caravan, the user sees the caravan list.
3. The user can create a new caravan or select an existing one.

### 8.2 Creating a caravan

1. The user opens the create flow.
2. The user enters a name.
3. The system creates the caravan with the default state.
4. The new caravan becomes selectable immediately.
5. The system may optionally set it as the active caravan.

### 8.3 Working on a caravan

1. The user selects a caravan.
2. The UI enters the caravan workspace.
3. Every caravan-related screen uses that caravan as context.
4. The user can move between sections without losing context.

## 9. API Expectations

The implementation should be structured so that the backend can support at least the following use cases:

- create caravan
- list caravans
- fetch caravan details
- select or persist active caravan
- update caravan state as future features are added

Suggested endpoints, if HTTP exposure is needed:

- `POST /api/caravans`
- `GET /api/caravans`
- `GET /api/caravans/{id}`
- `PATCH /api/caravans/{id}`
- `PUT /api/session/active-caravan`

The exact endpoint naming can change, but the use cases must remain available.

## 10. Acceptance Criteria

### Creation

- A user can create a caravan with a name.
- The created caravan is stored and appears in the list.
- The caravan starts with the default rule-based values.

### Selection

- A user can select a caravan from the list.
- The active caravan is preserved across navigation.
- Reloading the app restores the selected caravan when it still exists.

### Full flow

- The selected caravan exposes the full management surface.
- Caravan-specific actions always apply to the selected caravan, not to a global anonymous state.

### Data integrity

- Multiple caravans can coexist without overwriting each other.
- Selecting one caravan does not mutate the others.

## 11. Risks and Open Questions

1. Should the active caravan be stored only in session state, or also in persistent user settings?
2. Should caravan creation require a party level up front, or should it default to level 1 only?
3. Should archived caravans remain editable or become read-only?
4. Do we need an explicit campaign home screen, or should the caravan list act as the home screen?

## 12. Implementation Notes

- Keep the caravan as a domain aggregate, not as a UI-only concept.
- Keep the active selection as application/session state, not domain state.
- Use hexagonal boundaries so persistence and HTTP concerns do not leak into the domain.
- Add architecture boundary tests when the new packages are introduced.

## 13. Related Source Material

- `docs/Reglas_de_Caravana.md` — gameplay rules and caravan mechanics
- `docs/architecture.md` — repository architecture guidelines
- `README.md` — stack and repository structure

