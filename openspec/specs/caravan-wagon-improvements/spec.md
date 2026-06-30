# Caravan Wagon Improvements

**Status:** Draft  
**Target:** GestionCaravana  
**Type:** Feature specification

## 1. Summary

The application must allow the user to manage **improvements** for the currently selected wagon inside the active caravan.

From the selected wagon view, the user must be able to:

- browse the list of possible wagon improvements,
- add an improvement to the wagon,
- remove an improvement from the wagon,
- see the wagon statistics update immediately after each change.

This feature operates inside the active caravan context and builds on the existing wagon management specification.

## 2. Problem Statement

The user needs a clear and rule-driven way to customize a wagon with the available improvements defined by the caravan rules.

Without a dedicated improvement flow:

- the user cannot discover which improvements are available for the selected wagon,
- one-time and repeatable improvements are hard to manage correctly,
- prerequisite and incompatibility rules are easy to violate,
- wagon statistics can become stale after a change,
- the UI may drift from the canonical rule data.

## 3. Goals

1. Let the user view the list of wagon improvements for the selected wagon.
2. Allow the user to add an available improvement to that wagon.
3. Allow the user to remove an owned improvement from that wagon.
4. Recompute and expose the wagon statistics after every add or remove action.
5. Enforce improvement limits, prerequisites, and incompatibilities from the domain rules.
6. Keep improvement data persistent and tied to the correct wagon.
7. Keep stat derivation in the domain/application layer, not in the UI.

## 4. Non-Goals

This specification does **not** define:

- refund rules when removing an improvement,
- full caravan travel resolution,
- combat resolution for wagons,
- detailed balancing changes to the rulebook itself,
- multiplayer synchronization,
- cloud synchronization.

If refund behavior is required later, it should be added as a separate rule decision.

## 5. User Stories

### US-1: Browse wagon improvements

As a user, I want to see the list of possible improvements for the selected wagon so that I can decide which ones to add.

### US-2: Add an improvement

As a user, I want to add an allowed improvement to the selected wagon so that the wagon gains the expected benefits.

### US-3: Remove an improvement

As a user, I want to remove an owned improvement from the selected wagon so that I can revert or adjust the wagon configuration.

### US-4: See updated stats immediately

As a user, I want the wagon statistics to update after every improvement change so that I always see the current derived values.

## 6. Functional Requirements

### 6.1 Active caravan and wagon prerequisite

- The improvement feature must operate on the currently selected caravan.
- A specific wagon inside that caravan must also be selected.
- If no caravan or no wagon is active, the application must not allow improvement changes and must direct the user to select the required context first.

### 6.2 Improvement catalog

- The system must expose the full catalog of wagon improvements defined by the game rules.
- The catalog must include one entry per improvement type, not per owned instance.
- The catalog must preserve rule metadata such as:
  - name,
  - category or grouping,
  - cost expression,
  - hit point modifier,
  - hardness modifier,
  - propulsion modifier,
  - traveler capacity modifier,
  - cargo capacity modifier,
  - consumption modifier,
  - special benefit text,
  - description,
  - notes,
  - limit,
  - prerequisites,
  - incompatibilities.
- The catalog must support rule expressions that are not fixed numbers, such as multipliers, additive modifiers, and repeatable effects.

### 6.3 Availability state

- The UI must show which improvements are available, already owned, blocked by limits, blocked by prerequisites, or blocked by incompatibilities.
- If an improvement is already attached and its limit is `1 por carro`, the add action must be disabled.
- If an improvement is repeatable, the user must be able to add it again while the rule limit still allows it.
- If the catalog entry is incompatible with another owned improvement, the UI must surface the reason.

### 6.4 Add improvement flow

- The user must be able to select one improvement and add it to the selected wagon.
- Adding an improvement must create a persistent owned-improvement record associated with that wagon.
- The system must reject the addition when:
  - the improvement is not known,
  - the wagon does not exist,
  - the caravan does not exist,
  - the improvement limit for that wagon has been reached,
  - a prerequisite is missing,
  - the improvement is incompatible with another owned improvement,
  - the wagon or caravan state makes the improvement invalid according to the rules.
- If the domain tracks caravan funds and the improvement has a cost, the cost must be validated before the addition is accepted.
- The application must show a clear reason when the addition is rejected.

### 6.5 Remove improvement flow

- The user must be able to remove an owned improvement from the selected wagon.
- Removing an improvement must delete the persistent owned-improvement record.
- After removal, the wagon statistics must be recalculated immediately.
- If removing an improvement would leave another owned improvement without a required prerequisite, the removal must be blocked or require the dependent improvements to be removed first.
- Removal must not mutate the improvement catalog itself.

### 6.6 Derived stat recalculation

- Wagon statistics must be derived from the base wagon definition plus the currently owned improvements.
- After any add or remove action, the application must recalculate the derived statistics for the wagon.
- Any caravan-level statistics impacted by the wagon’s improvements must also be recalculated.
- The recalculated values must be visible in the UI without requiring a page reload.
- The backend must remain the source of truth for derived values so that the frontend does not duplicate rule logic.

### 6.7 Domain data integrity

- Owned improvements must be scoped to a single wagon.
- Adding or removing an improvement from one wagon must not mutate other wagons.
- The model must distinguish between improvement type definitions and owned improvement instances.
- The model must preserve repeatable applications separately from one-time applications.

## 7. Domain Model

### 7.1 Improvement type

`WagonImprovementType`

Represents a catalog entry defined by the game rules.

Suggested attributes:

- `code`
- `name`
- `category`
- `costExpression`
- `hitPointsModifier`
- `hardnessModifier`
- `propulsionModifier`
- `travelerCapacityModifier`
- `cargoCapacityModifier`
- `consumptionModifier`
- `specialBenefit`
- `description`
- `notes`
- `limit`
- `prerequisites`
- `incompatibilities`

### 7.2 Owned improvement

`CaravanWagonImprovement`

Represents an improvement that has been added to a specific wagon.

Suggested attributes:

- `id`
- `caravanId`
- `wagonId`
- `improvementTypeCode`
- `createdAt`
- `updatedAt`

### 7.3 Wagon relationship

The wagon aggregate or wagon ownership model owns the list of applied improvements.

The improvement catalog itself is read-only reference data.

### 7.4 Stat derivation model

The selected wagon should expose at least:

- base statistics from the wagon type,
- improvement-adjusted statistics,
- a list of owned improvements,
- any active validation warnings or blocked conditions.

The stat derivation rules must be deterministic and reproducible from persisted data.

## 8. Improvement Catalog Scope

The initial catalog must include the wagon improvements documented in `docs/Reglas_de_Caravana.md`, including:

- one-time defensive and utility improvements,
- repeatable capacity/extensibility improvements,
- propulsion-related improvements,
- weather protection improvements,
- weapon and special equipment improvements.

The implementation must preserve the rule text and modifiers as the source of truth for:

- cost,
- hit point changes,
- hardness changes,
- propulsion changes,
- capacity changes,
- consumption changes,
- benefit text,
- prerequisites,
- incompatibilities,
- limits.

If the rules document is later expanded with new improvements, the catalog must be able to grow without changing the UI contract.

## 9. UX / Flow

### 9.1 Accessing the feature

1. The user opens the active caravan workspace.
2. The user selects a wagon.
3. The UI shows the wagon details and its applied improvements.
4. The user can open the improvement list from that context.

### 9.2 Adding an improvement

1. The user opens the improvement list.
2. The user reviews the possible improvements and their availability.
3. The user selects an allowed improvement.
4. The user confirms the action.
5. The system saves the new improvement and refreshes the wagon statistics.

### 9.3 Removing an improvement

1. The user opens the selected wagon’s applied improvements.
2. The user chooses one improvement to remove.
3. The system validates that removal will not break remaining dependencies.
4. The user confirms the action.
5. The system removes the improvement and refreshes the wagon statistics.

## 10. API Expectations

The implementation should be structured so the backend can support at least the following use cases:

- list wagon improvements for a wagon,
- fetch a specific improvement catalog entry,
- list applied improvements for a wagon,
- add an improvement to a wagon,
- remove an improvement from a wagon,
- fetch the derived wagon state after changes,
- validate prerequisites and incompatibilities before creation.

Suggested HTTP endpoints, if needed:

- `GET /api/caravans/{caravanId}/wagons/{wagonId}/improvements/catalog`
- `GET /api/caravans/{caravanId}/wagons/{wagonId}/improvements`
- `POST /api/caravans/{caravanId}/wagons/{wagonId}/improvements`
- `DELETE /api/caravans/{caravanId}/wagons/{wagonId}/improvements/{improvementId}`
- `GET /api/caravans/{caravanId}/wagons/{wagonId}`

The exact endpoint names may change, but the use cases must remain available.

## 11. Acceptance Criteria

### Catalog and availability

- The user can see the list of possible improvements for the selected wagon.
- The UI indicates whether each improvement is available, already owned, blocked, or repeatable.
- The list includes the rule data needed to make an informed decision.

### Add improvement

- The user can add an allowed improvement to the selected wagon.
- The new improvement appears in the wagon’s applied-improvements list.
- The system rejects invalid additions when limits, prerequisites, incompatibilities, or other rules are violated.

### Remove improvement

- The user can remove an owned improvement from the selected wagon.
- The removed improvement no longer appears in the wagon’s applied-improvements list.
- The system blocks removal when it would invalidate dependent improvements.

### Derived stats

- Wagon statistics update immediately after add and remove actions.
- Any caravan-level statistics affected by wagon improvements are also updated.
- The updated values persist after reload.

### Persistence and integrity

- Applied improvements persist with the wagon.
- Improvements are isolated to their wagon.
- Multiple wagons can have different improvement sets without interfering with each other.

## 12. Edge Cases

1. The active caravan exists but no wagon is selected.
2. The selected wagon has no improvements yet.
3. A one-time improvement is already applied and the user tries to add it again.
4. A repeatable improvement is added multiple times until the allowed limit is reached.
5. Two improvements are incompatible with each other.
6. An improvement has a prerequisite that is not currently present.
7. Removing an improvement would break a dependency chain.
8. The improvement catalog grows with new entries over time.
9. The user opens the improvement list while the selected wagon is still loading.
10. The persisted data contains an unknown improvement code from an older version.

## 13. Risks and Open Questions

1. When removing an improvement, should the user receive a refund, no refund, or a rule-based partial refund?
2. Should the improvement list hide blocked entries or show them disabled with reasons?
3. Should derived wagon stats be stored, cached, or always computed on demand?
4. Should the UI show a stat delta preview before confirmation?
5. Should some improvements be mutually exclusive by category rather than by explicit incompatibility list?

## 14. Implementation Notes

- Keep the improvement catalog as reference data, not as mutable domain state.
- Keep the owned improvement list inside the wagon aggregate or wagon-specific persistence model.
- Do not duplicate rule logic in the frontend; the backend must provide the canonical derived state.
- Add or update architecture boundary tests if new packages are introduced.
- Keep the UI reactive so wagon stats refresh immediately after each successful mutation.

## 15. Related Source Material

- `docs/Reglas_de_Caravana.md` — wagon improvement rules, limits, and modifiers
- `openspec/specs/caravan-instance/spec.md` — active caravan context
- `openspec/specs/caravan-wagons/spec.md` — wagon catalog and wagon ownership management
- `docs/architecture.md` — repository architecture guidelines
