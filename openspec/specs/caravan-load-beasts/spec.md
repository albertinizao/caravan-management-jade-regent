# Caravan Load Beast Management

**Status:** Draft  
**Target:** GestionCaravana  
**Type:** Feature specification

## 1. Summary

The application must allow the user to manage **load beasts** belonging to the active caravan.

The feature must provide:

- a catalog-based add flow for canonical beasts,
- a custom-beast creation flow,
- a dedicated list and detail view for owned beasts,
- the ability to assign a beast either as a draft animal for a wagon or as a traveler in a wagon,
- validation that prevents assigning more beasts to a wagon than its draft setup allows.

This feature operates inside the active caravan context and must stay consistent with the wagon and traveler features.

## 2. Problem Statement

The user needs a single place to register, inspect, and assign beasts that help the caravan travel.

Without this feature:

- load beasts cannot be managed from the application,
- the user cannot choose between rule-defined beasts and custom beasts,
- draft capacity must be tracked manually,
- it is easy to assign too many beasts to a wagon,
- beasts assigned as travelers are not coordinated with wagon occupancy.

## 3. Goals

1. Show the available beast catalog from the rules.
2. Allow the user to add a catalog beast to the active caravan.
3. Allow the user to create a custom beast.
4. Show a dedicated list of owned beasts for the active caravan.
5. Open a detail modal with the full beast information.
6. Allow assigning a beast to a wagon as a draft animal.
7. Allow assigning a beast to a wagon as a traveler.
8. Enforce wagon draft capacity and compatibility rules.
9. Keep beast data persistent and scoped to the active caravan.

## 4. Non-Goals

This specification does **not** define:

- beast combat, injury, healing, fatigue recovery, or training progression,
- breeding, pedigree, or family-tree management,
- detailed creature stat blocks beyond the fields needed to manage the caravan,
- seasonal simulation or temperature effects over time,
- wagon creation or wagon rule management,
- traveler role management beyond the impact of a beast being assigned as a traveler,
- market pricing simulation beyond capture and display of the beast catalog values,
- multiplayer synchronization.

## 5. User Stories

### US-1: Browse the beast catalog

As a user, I want to see the available beasts so that I can choose one that fits my caravan.

### US-2: Add a catalog beast

As a user, I want to add a beast from the catalog so that I can use a rule-defined creature without re-entering its data.

### US-3: Create a custom beast

As a user, I want to create a custom beast so that I can use a caravan-specific or homebrew creature.

### US-4: Inspect beast details

As a user, I want to open a beast detail modal so that I can review all relevant data without leaving the list.

### US-5: Assign a beast as draft animal

As a user, I want to assign a beast to a wagon as a draft animal so that it helps pull that wagon.

### US-6: Assign a beast as traveler

As a user, I want to assign a beast to a wagon as a traveler so that it can travel in the wagon instead of pulling it.

### US-7: Prevent over-assignment

As a user, I want the system to reject invalid draft assignments so that I do not exceed the wagon’s pull capacity.

## 6. Functional Requirements

### 6.1 Active caravan prerequisite

- The beast feature must operate on the currently selected caravan.
- If no caravan is active, the application must not allow beast management actions and must direct the user to select or create a caravan first.

### 6.2 Beast catalog

- The system must expose the full beast catalog defined by the game rules.
- Each catalog entry must represent a rule-defined beast, not an owned instance.
- Each catalog entry must include at least:
  - name,
  - base price,
  - trained price, when present,
  - size,
  - strength,
  - speed,
  - thermal adaptation,
  - notable trait or special note,
  - description or rules summary.
- The catalog must be read-only reference data.
- The catalog must be extensible so that new beasts can be added later without changing the UI contract.

### 6.3 Owned beast list

- The system must provide a dedicated list view for the beasts owned by the active caravan.
- The list must show one row per owned beast.
- Each row must display at least:
  - beast name,
  - origin type (`catalog` or `custom`),
  - size,
  - strength,
  - speed,
  - current assignment state,
  - assigned wagon, if any.
- The list must support:
  - searching by name,
  - filtering by origin type,
  - filtering by assignment state,
  - filtering by wagon.
- The list must show a clear empty state when there are no beasts or when the active filters match nothing.

### 6.4 Beast detail modal

- Clicking a beast must open a modal with the full beast details.
- The modal must show at least:
  - name,
  - origin type,
  - catalog code, if the beast comes from the catalog,
  - size,
  - strength,
  - speed,
  - thermal adaptation,
  - base price,
  - trained price, if any,
  - special note,
  - description,
  - custom notes, if present,
  - current assignment.
- The modal must not require page navigation.
- The modal must be dismissible without changing data.

### 6.5 Add catalog beast flow

- The beast screen must provide an action to add a beast from the catalog.
- The flow must present the catalog entries available to the active caravan.
- The user must be able to select one catalog beast and create an owned beast record from it.
- The application must persist the owned beast inside the active caravan.
- Multiple owned instances of the same catalog beast must be allowed unless the rules later define a limit.

### 6.6 Create custom beast flow

- The beast screen must provide an action to create a custom beast.
- The creation form must allow the user to enter at least:
  - name,
  - size,
  - strength,
  - speed,
  - thermal adaptation,
  - base price,
  - trained price, if applicable,
  - special note or trait summary,
  - description,
  - custom notes.
- The name must be required.
- The fields required for draft validation must be captured before save.
- The custom beast must be stored as an owned caravan beast, not as a catalog item.

### 6.7 Draft assignment to a wagon

- The detail modal must include an action to assign the beast as a draft animal.
- The action must present the wagons of the active caravan.
- The system must validate the assignment against the selected wagon’s draft requirement.
- The system must reject the assignment if the wagon cannot accept additional draft beasts according to its propulsion rule.
- The system must reject the assignment if the beast is already assigned as a draft animal or as a traveler on another wagon.
- The system must allow reassigning the beast by clearing its previous assignment in the same operation.

### 6.8 Traveler assignment to a wagon

- The detail modal must include an action to assign the beast as a traveler.
- The action must present the wagons of the active caravan.
- The system must validate the assignment against the wagon’s traveler capacity.
- When assigned as a traveler, the beast must count toward the wagon’s traveler occupancy and caravan consumption just like any other traveler-like occupant.
- A beast assigned as a traveler must not simultaneously remain assigned as a draft animal.
- The system must allow reassigning the beast by clearing its previous assignment in the same operation.

### 6.9 Draft capacity rules

- The system must enforce the draft rules defined by the wagon catalog and the game rules.
- Wagon propulsion must be treated as structured validation data, not as a free-form string in the UI.
- The validation must cover at least:
  - the maximum number of large draft beasts allowed,
  - the maximum number of medium draft beasts allowed,
  - the minimum strength required by the wagon,
  - the rule that four-legged creatures count double their strength when evaluating propulsion.
- If the final draft combination is invalid, the assignment must be rejected with a clear message.
- The UI should surface the remaining draft capacity when that can be derived from the wagon data.

### 6.10 Data integrity

- Owned beasts must be scoped to a single caravan.
- Managing beasts in one caravan must not mutate the beasts of another caravan.
- Catalog data must remain canonical and must not be copied into mutable state except where a snapshot is needed for display or audit purposes.
- A beast must have at most one active assignment at a time.
- A beast assigned as a traveler must be reflected in the traveler occupancy model of the wagon.

## 7. Domain Model

### 7.1 Aggregate ownership

The caravan campaign remains the aggregate root.

Load beasts are owned by the caravan campaign and are persisted within that scope.

### 7.2 Load beast

`LoadBeast`

Represents a beast that the caravan owns and can use either as a draft animal or as a traveler.

Suggested attributes:

- `id`
- `caravanId`
- `sourceType` (`CATALOG` or `CUSTOM`)
- `catalogBeastCode` optional
- `name`
- `size`
- `strength`
- `speed`
- `thermalAdaptation`
- `basePrice`
- `trainedPrice` optional
- `specialNote`
- `description`
- `customNotes` optional
- `assignmentType` (`NONE`, `DRAFT`, or `TRAVELER`)
- `assignedWagonId` optional
- `createdAt`
- `updatedAt`

### 7.3 Beast catalog item

`LoadBeastCatalogItem`

Represents a rule-defined beast that can be selected by the user.

Suggested attributes:

- `code`
- `name`
- `basePrice`
- `trainedPrice` optional
- `size`
- `strength`
- `speed`
- `thermalAdaptation`
- `specialNote`
- `description`
- `notes` optional

### 7.4 Draft propulsion constraint

`WagonDraftConstraint`

Represents the normalized draft rule for a wagon.

Suggested attributes:

- `maxLargeBeasts`
- `maxMediumBeasts`
- `minimumStrength`
- `strengthCountMultiplierForFourLeggedCreatures`

The UI may display a textual summary, but the backend must validate using structured data.

### 7.5 Beast assignment

`LoadBeastAssignment`

Represents the current active assignment of a beast.

Suggested attributes:

- `assignmentType`
- `wagonId`
- `assignedAt`

The assignment must be exclusive: a beast can be unassigned, assigned as draft, or assigned as traveler, but not more than one at the same time.

## 8. Beast Catalog Scope

The initial catalog must include the beasts documented in `docs/Reglas_de_Caravana.md` under the beasts and draft-animal sections.

The implementation must treat the rules document as the source of truth for:

- creature name,
- price,
- trained price when present,
- size,
- strength,
- speed,
- thermal adaptation,
- special notes,
- descriptive text.

If the rules document later expands with new beasts, the catalog must grow without changing the UI contract.

## 9. UX / Flow

### 9.1 Opening the beast screen

1. The user opens the active caravan workspace.
2. The user navigates to the beasts section.
3. The user sees the owned-beast list.

### 9.2 Adding a catalog beast

1. The user opens the catalog flow.
2. The user reviews the catalog entries.
3. The user selects one beast.
4. The user confirms creation.
5. The system creates the owned beast and updates the list.

### 9.3 Creating a custom beast

1. The user opens the custom beast flow.
2. The user enters the beast data.
3. The system validates the fields.
4. The user confirms creation.
5. The system creates the owned beast and updates the list.

### 9.4 Assigning a beast as draft animal

1. The user opens the beast detail modal.
2. The user chooses `Assign as draft animal`.
3. The system shows the active caravan wagons.
4. The user selects a wagon.
5. The system validates the draft rules and saves the assignment.

### 9.5 Assigning a beast as traveler

1. The user opens the beast detail modal.
2. The user chooses `Assign as traveler`.
3. The system shows the active caravan wagons.
4. The user selects a wagon.
5. The system validates the wagon capacity and saves the assignment.

## 10. API Expectations

The implementation should be structured so the backend can support at least the following use cases:

- list beast catalog entries,
- fetch a specific catalog beast,
- list beasts for the active caravan,
- fetch a specific owned beast,
- create a beast from the catalog,
- create a custom beast,
- update a beast assignment,
- clear a beast assignment,
- validate draft capacity before persisting.

Suggested HTTP endpoints, if HTTP exposure is needed:

- `GET /api/caravans/{caravanId}/beasts/catalog`
- `GET /api/caravans/{caravanId}/beasts/catalog/{beastCode}`
- `GET /api/caravans/{caravanId}/beasts?query=&sourceType=&assignmentType=&wagonId=`
- `GET /api/caravans/{caravanId}/beasts/{beastId}`
- `POST /api/caravans/{caravanId}/beasts/catalog/{beastCode}`
- `POST /api/caravans/{caravanId}/beasts`
- `PATCH /api/caravans/{caravanId}/beasts/{beastId}`
- `DELETE /api/caravans/{caravanId}/beasts/{beastId}/assignment`

The exact endpoint names may change, but the use cases must remain available.

## 11. Acceptance Criteria

### Catalog and custom creation

- The user can browse the beast catalog.
- The user can inspect catalog beast details before adding one.
- The user can create a custom beast.
- The created beast appears in the active caravan immediately after saving.

### Detail modal

- Clicking a beast opens a modal with full details.
- The modal shows whether the beast is catalog-based or custom.
- The modal can be closed without changing data.

### Draft assignment

- The user can assign a beast to a wagon as a draft animal.
- The system rejects assignments that exceed the wagon’s draft capacity.
- The system rejects assignments that violate the wagon’s propulsion rule.
- The assigned beast is reflected in the wagon’s draft composition.

### Traveler assignment

- The user can assign a beast to a wagon as a traveler.
- The system rejects assignments that exceed the wagon’s traveler capacity.
- The assigned beast is reflected in the wagon’s traveler occupancy.

### Persistence and integrity

- Beasts persist with the active caravan.
- Beasts from different caravans do not interfere with each other.
- A beast can have only one active assignment at a time.

## 12. Edge Cases

1. The active caravan exists but has no beasts.
2. The user opens the beast screen while no caravan is active.
3. The catalog grows and includes new beasts later.
4. A custom beast is created with invalid or incomplete draft data.
5. A wagon has reached its maximum draft beasts.
6. A beast is reassigned from draft to traveler in one action.
7. A beast is reassigned to another wagon after its previous wagon becomes invalid or unavailable.
8. The wagon selected for assignment is already full for travelers.
9. The wagon propulsion rule requires large and medium limits that the chosen mix does not satisfy.
10. The beast is already assigned and the user attempts to assign it again without clearing the previous assignment.

## 13. Risks and Open Questions

1. Should custom beasts be editable after creation, or should users create a new beast when the data changes?
2. Should the UI expose the normalized draft rule directly, or only a human-readable summary?
3. Should beast assignments be displayed inside the traveler screen as a special occupant type, or only inside the beast screen and wagon details?
4. Should the system allow beasts to be unassigned without deleting them from the caravan?
5. Should the initial release validate fatigue and temperature effects, or only the static draft capacity rule?

## 14. Implementation Notes

- Keep the beast catalog as read-only reference data.
- Keep owned beasts inside the caravan aggregate.
- Normalize draft rules into structured validation data instead of parsing them in the UI.
- Reuse the active caravan and wagon use cases instead of duplicating wagon validation logic in the beast screen.
- Treat beasts assigned as travelers as occupancy data that must remain compatible with the traveler feature.
- Add or update architecture boundary tests if new packages are introduced.

## 15. Related Source Material

- `docs/Reglas_de_Caravana.md` — beast catalog, draft-animal rules, and wagon propulsion rules
- `openspec/specs/caravan-instance/spec.md` — active caravan context
- `openspec/specs/caravan-wagons/spec.md` — wagon catalog and wagon capacity context
- `openspec/specs/caravan-travelers/spec.md` — traveler occupancy and wagon assignment context
- `docs/architecture.md` — repository architecture guidelines
