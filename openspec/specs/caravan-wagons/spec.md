# Caravan Wagon Management

**Status:** Draft  
**Target:** GestionCaravana  
**Type:** Feature specification

## 1. Summary

The application must allow the user to add **wagons** to the currently active caravan.

Before adding a wagon, the user must be able to browse the full wagon catalog and inspect the complete statistics of each wagon type.

After a wagon is added, the user must be able to open a dedicated caravan wagon view that shows:

- a table with all wagons owned by the caravan,
- the basic statistics of each wagon,
- a modal with the full details of a specific wagon when the user clicks it.

This feature operates inside the active caravan context and builds on the existing caravan instance specification.

## 2. Problem Statement

The user needs a clear, rule-driven way to expand a caravan’s infrastructure with wagons.

Without a dedicated wagon flow:

- the user cannot compare wagon types before purchase,
- caravan capacity, cost, and special benefits are hard to understand,
- wagon ownership is not visible in one place,
- detailed wagon data is fragmented or hidden in rules documentation instead of the application.

## 3. Goals

1. Let the user browse all available wagon types before adding one.
2. Show the full wagon statistics before confirmation.
3. Allow the user to add a wagon to the active caravan.
4. Provide a caravan wagon overview with a table of owned wagons.
5. Open a modal with the full details of any owned wagon.
6. Enforce caravan wagon limits and other validation rules from the domain.
7. Keep wagon data persistent and tied to the selected caravan.

## 4. Non-Goals

This specification does **not** define:

- wagon combat,
- wagon damage, repairs, or hit point loss tracking,
- wagon upgrades or customization,
- wagon removal/selling workflows,
- inventory cargo assignment rules,
- beast-of-burden management,
- traveler assignment rules beyond displaying the wagon’s capacity and related stats,
- multiplayer or synchronization features.

## 5. User Stories

### US-1: Browse the wagon catalog

As a user, I want to see all available wagon types so that I can decide which one to add to my caravan.

### US-2: Compare wagon statistics before adding

As a user, I want to inspect the full statistics of a wagon before buying it so that I understand its cost, capacity, and benefits.

### US-3: Add a wagon to the active caravan

As a user, I want to add a selected wagon type to my active caravan so that it becomes part of my caravan’s infrastructure.

### US-4: Review all owned wagons in one view

As a user, I want a dedicated caravan wagon screen with a table of all my wagons so that I can manage them from one place.

### US-5: Inspect a specific wagon in detail

As a user, I want to click a wagon in the table and open a modal with all of its details so that I can review its complete data without leaving the page.

## 6. Functional Requirements

### 6.1 Active caravan prerequisite

- The wagon feature must operate on the currently selected caravan.
- If no caravan is active, the application must not allow wagon addition and must direct the user to select or create a caravan first.

### 6.2 Wagon catalog

- The system must expose the full wagon catalog defined by the game rules.
- Each catalog entry must represent a wagon type, not an owned instance.
- The catalog must include at least the following data per wagon type:
  - name,
  - category or subcategory,
  - cost,
  - hit points,
  - hardness,
  - propulsion requirement,
  - traveler capacity,
  - cargo capacity,
  - limit,
  - consumption,
  - special benefit summary,
  - description,
  - notes, when present.

### 6.3 Preview before add

- The user must be able to inspect all statistics of a wagon type before adding it.
- The preview must be readable without opening a second page.
- The user must be able to cancel the action before confirmation.

### 6.4 Add wagon flow

- The system must allow the user to select one wagon type and add it to the active caravan.
- Adding a wagon must create a persistent owned-wagon record associated with the active caravan.
- The application must deduct the wagon cost from the caravan’s available funds, if the domain already tracks caravan treasury.
- If the caravan does not have enough funds, the addition must be rejected with a clear message.
- If the caravan has reached its maximum wagon count, the addition must be rejected.
- If the selected wagon type has a per-type limit and the caravan already reached that limit, the addition must be rejected.

### 6.5 Wagon limits

- The caravan’s maximum wagon count must follow the domain rule: `10 + caravan level`.
- Wagon-type limits must be enforced when the type defines a finite limit.
- Wagon-type entries with `None` as the limit must be treated as unlimited, subject only to the caravan-wide maximum.

### 6.6 Caravan wagon overview

- The application must provide a dedicated view that lists all wagons owned by the active caravan.
- The table must show at least the following basic information per owned wagon:
  - wagon name or type name,
  - category,
  - hit points,
  - hardness,
  - propulsion requirement,
  - traveler capacity,
  - cargo capacity,
  - consumption,
  - special benefit summary.
- The view must show an empty state when the caravan owns no wagons.
- The view must update after a wagon is added without requiring a full application restart.

### 6.7 Wagon detail modal

- Clicking a wagon row or card in the overview must open a modal.
- The modal must show the full details for the selected wagon.
- The modal must include at least all data shown in the catalog preview plus:
  - owned wagon identifier,
  - acquisition date or creation timestamp, if available,
  - caravan association context.
- The modal must be dismissible without changing the wagon data.

### 6.8 Domain data integrity

- Owned wagons must be scoped to a single caravan.
- Adding a wagon must not mutate other caravans.
- The data model must distinguish between wagon type definitions and owned wagon instances.
- Wagon statistics shown in the UI must come from the canonical wagon definition, not duplicated ad hoc in the view layer.

## 7. Domain Model

### 7.1 Wagon type

`WagonType`

Represents a catalog entry defined by the game rules.

Suggested attributes:

- `id`
- `code`
- `name`
- `category`
- `cost`
- `hitPoints`
- `hardness`
- `propulsionRequirement`
- `travelerCapacity`
- `cargoCapacity`
- `limit`
- `consumption`
- `specialBenefit`
- `description`
- `notes`

### 7.2 Owned wagon

`CaravanWagon`

Represents a wagon that has been added to a specific caravan.

Suggested attributes:

- `id`
- `caravanId`
- `wagonTypeCode`
- `displayName` optional
- `createdAt`
- `updatedAt`

The owned wagon should reference the wagon type definition instead of copying the full catalog payload into the instance.

### 7.3 Caravan relationship

The caravan aggregate owns the list of `CaravanWagon` instances.

The wagon catalog itself is read-only reference data.

## 8. Wagon Catalog Scope

The initial catalog must include the wagon types documented in `docs/Reglas_de_Caravana.md`, including the traveler, special, and utility categories.

The implementation must preserve the rule data as the source of truth for these fields:

- cost,
- hit points,
- hardness,
- propulsion,
- capacities,
- limits,
- consumption,
- benefit text,
- descriptive text.

If the rules document is later expanded with new wagon types, the catalog must be able to grow without changing the UI contract.

## 9. UX / Flow

### 9.1 Accessing the feature

1. The user opens the active caravan workspace.
2. The user navigates to the wagons section.
3. The user sees the wagon overview table.

### 9.2 Adding a wagon

1. The user opens the add-wagon flow.
2. The user browses the wagon catalog.
3. The user reviews the full statistics of a wagon type.
4. The user confirms the addition.
5. The system creates the owned wagon and updates the caravan view.

### 9.3 Inspecting a wagon

1. The user clicks a wagon in the table.
2. The system opens a modal.
3. The modal shows the wagon’s full details.
4. The user closes the modal and returns to the table.

## 10. API Expectations

The implementation should be structured so the backend can support at least the following use cases:

- list wagon catalog entries,
- fetch wagon catalog details,
- list wagons for a caravan,
- add a wagon to a caravan,
- fetch a specific owned wagon,
- validate caravan-wide wagon limits before creating the owned wagon.

Suggested HTTP endpoints, if needed:

- `GET /api/caravans/{caravanId}/wagons/catalog`
- `GET /api/caravans/{caravanId}/wagons`
- `POST /api/caravans/{caravanId}/wagons`
- `GET /api/caravans/{caravanId}/wagons/{wagonId}`

The exact endpoint names may change, but the use cases must remain available.

## 11. Acceptance Criteria

### Catalog and preview

- The user can see all available wagon types.
- The user can inspect the full statistics of a wagon type before adding it.
- The preview includes the information needed to make an informed decision.

### Add wagon

- The user can add a wagon to the active caravan.
- The new wagon appears in the caravan wagon table.
- The system rejects invalid additions when the caravan lacks funds or exceeds wagon limits.

### Caravan wagon overview

- The user can open a dedicated wagon view.
- The view shows a table of all owned wagons.
- The table displays the agreed basic statistics.

### Wagon detail modal

- Clicking a wagon opens a modal with full details.
- The modal can be closed without changing data.

### Persistence and integrity

- Added wagons persist with the caravan.
- Wagons are isolated to their caravan.
- Multiple caravans can have different wagon sets without interfering with each other.

## 12. Edge Cases

1. The active caravan exists but has no funds.
2. The active caravan reaches the maximum of `10 + level` wagons.
3. A wagon type with a finite limit is already fully owned by the caravan.
4. The wagon catalog grows with new entries over time.
5. The user opens the wagon overview while no caravan is active.
6. The user clicks a wagon that is still loading or fails to load.
7. The user attempts to add the same limited wagon type multiple times.

## 13. Risks and Open Questions

1. Should the wagon overview support filtering and sorting in the first version?
2. Should the modal be route-based for deep linking, or purely UI state?
3. Should the owned wagon support a custom display name at creation time?
4. Should a wagon cost be deducted immediately on add, or only after a confirmation step that also handles treasury validation?
5. Do we want to surface the wagon category labels exactly as in the rules document, or normalize them for the UI?

## 14. Implementation Notes

- Keep the wagon catalog as reference data, not as mutable domain state.
- Keep the owned wagon list inside the caravan aggregate.
- Do not duplicate rule text in the UI; map UI labels to canonical catalog values.
- Add or update architecture boundary tests if new packages are introduced.
- Keep the modal and table in the frontend, but source their data from backend use cases.

## 15. Related Source Material

- `docs/Reglas_de_Caravana.md` — wagon rules, stats, limits, and descriptions
- `openspec/specs/caravan-instance/spec.md` — active caravan context
- `docs/architecture.md` — repository architecture guidelines
