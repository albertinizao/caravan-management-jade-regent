# Caravan Feat Management

**Status:** Draft  
**Target:** GestionCaravana  
**Type:** Feature specification

## 1. Summary

The application must allow the user to **view**, **add**, **edit**, and **delete** the caravan’s feats inside the active caravan context.

The feature must provide:

- a dedicated list of the caravan’s feats,
- the ability to add a new feat to the active caravan,
- the ability to edit an owned feat entry,
- the ability to delete an owned feat entry,
- a clear acquisition source for every feat, indicating whether it came from a level-up or from another cause,
- automatic visibility of feats that are currently inactive because their rule conditions are not met.

This feature operates inside the active caravan context and depends on the caravan instance specification.

## 2. Problem Statement

The user needs a single place to understand which feats belong to the current caravan, how each feat was acquired, and whether each feat is currently active according to the rulebook.

Without a dedicated feat management flow:

- feat ownership is hidden inside generic caravan data,
- the reason a feat was selected is lost or ambiguous,
- level-based feat selection cannot be traced back to the level that granted it,
- non-level causes cannot be recorded consistently,
- feats that stop meeting their conditions can become unclear in the UI,
- stat derivation can become hard to audit when feat effects are involved.

## 3. Goals

1. Show all feats owned by the active caravan in a dedicated view.
2. Allow the user to add a feat to the active caravan.
3. Require acquisition metadata when adding a feat:
   - level-up selection must store the level that granted the feat,
   - any other acquisition must store the cause text.
4. Allow the user to edit the acquisition metadata of an owned feat entry.
5. Allow the user to delete an owned feat entry.
6. Support repeatable feats without losing individual acquisition history.
7. Show whether each feat is currently active, inactive, or repeatable with remaining availability.
8. Keep feat data persistent and scoped to the selected caravan.
9. Keep feat rule evaluation in the domain/application layer, not in the UI.

## 4. Non-Goals

This specification does **not** define:

- combat resolution,
- the full rules engine for every feat effect in the book,
- automatic feat selection on level-up,
- cloud synchronization,
- multiplayer collaboration,
- full campaign history auditing beyond the current caravan record.

## 5. User Stories

### US-1: Browse caravan feats

As a user, I want to see all feats owned by the caravan so that I can understand the caravan’s current rule bonuses and state.

### US-2: Add a feat

As a user, I want to add a feat to the caravan so that I can record a new rule choice.

### US-3: Record feat acquisition source

As a user, I want to mark whether a feat came from a level-up or from another cause so that I can trace why it was selected.

### US-4: Edit an owned feat entry

As a user, I want to edit the acquisition metadata of an owned feat so that I can correct or refine how it was recorded.

### US-5: Delete an owned feat entry

As a user, I want to delete an owned feat so that I can remove an incorrect or obsolete entry from the caravan.

### US-6: Inspect feat activity

As a user, I want to know whether a feat is currently active or inactive so that I can understand whether its effects are applying right now.

## 6. Functional Requirements

### 6.1 Active caravan prerequisite

- The feat feature must operate on the currently selected caravan.
- If no caravan is active, the application must not allow feat management actions and must direct the user to select or create a caravan first.

### 6.2 Feat catalog

- The system must expose the full feat catalog defined by the game rules.
- Each catalog entry must represent a feat type, not an owned instance.
- The catalog must include at least the following data per feat type:
  - code,
  - name,
  - prerequisites,
  - benefit text,
  - special text, when present,
  - notes, when present,
  - repeatable flag or selection limit, when defined,
  - any rule metadata needed to determine availability.
- The catalog must preserve the rule text as the source of truth for feat effects and restrictions.
- The catalog must support future growth without changing the UI contract.

### 6.3 Feat list

- The application must provide a dedicated view that lists the feats owned by the active caravan.
- The list must show one row per owned feat entry.
- Each row must display at least:
  - feat name,
  - current activity state,
  - acquisition source type,
  - acquisition level, if the feat came from a level-up,
  - acquisition cause, if the feat came from another cause,
  - repeatable selection position or count, when relevant.
- The list must show a clear empty state when the caravan has no feats.
- The list must update after add or edit actions without requiring a full application restart.

### 6.4 Add feat flow

- The user must be able to select one feat type and add it to the active caravan.
- Adding a feat must create a persistent owned-feat record associated with the caravan.
- The add flow must require exactly one acquisition source:
  - `LEVEL_UP`, with the level that granted the feat,
  - `OTHER`, with a free-text cause.
- If the source is `LEVEL_UP`, the level value must be required and must be a positive integer.
- If the source is `OTHER`, the cause text must be required and must not be blank.
- The system must reject the addition when:
  - the feat is not known,
  - the caravan does not exist,
  - the feat is not available because a prerequisite is missing,
  - the feat is already owned and the feat is not repeatable,
  - the repeatable selection limit has been reached,
  - the acquisition source is missing or invalid,
  - the selected level is not compatible with the caravan’s current progression.
- The application must show a clear reason when the addition is rejected.

### 6.5 Edit feat flow

- The user must be able to edit an owned feat entry from the feat list or a feat detail modal.
- Editing must allow the user to update the acquisition source metadata:
  - switch between level-up and other-cause,
  - change the recorded level when the source is level-up,
  - change the recorded cause when the source is other-cause.
- Editing must not mutate the feat catalog entry itself.
- If a feat is repeatable, editing one owned instance must not change the other instances of the same feat type.
- The system must reject edits that would leave the acquisition metadata incomplete or invalid.

### 6.6 Availability and repeatable feats

- The UI must show whether a feat type is available, already owned, blocked by prerequisites, or blocked by a repeat limit.
- If a feat is repeatable, the UI must show how many times it has already been selected and whether more selections are allowed.
- If a feat is not repeatable, the add action must be disabled once the feat is owned.
- If the catalog entry is only partially available because of current caravan state, the UI must surface the blocking reason.

### 6.7 Feat activity and rule evaluation

- The application must distinguish between:
  - owned feat records,
  - feat types from the catalog,
  - current active/inactive state derived from caravan rules.
- If a feat’s prerequisites or conditions are no longer met, the feat must be shown as inactive.
- If a feat becomes inactive, it must stop contributing its effects to derived statistics until the conditions are met again.
- If the caravan once again satisfies the conditions, the feat must become active again without losing its ownership record.
- The backend must remain the source of truth for feat activity and stat derivation so that the frontend does not duplicate rule logic.

### 6.8 Domain data integrity

- Feat ownership must be scoped to a single caravan.
- Adding or editing a feat in one caravan must not mutate another caravan.
- The model must distinguish between feat type definitions and owned feat instances.
- The model must preserve repeatable applications separately from one-time applications.

## 7. Domain Model

### 7.1 Feat type

`CaravanFeatType`

Represents a catalog entry defined by the game rules.

Suggested attributes:

- `code`
- `name`
- `prerequisites`
- `benefitText`
- `specialText`
- `notes`
- `repeatable`
- `selectionLimit`
- `availabilityRules`

### 7.2 Owned feat

`CaravanFeat`

Represents a feat that has been added to a specific caravan.

Suggested attributes:

- `id`
- `caravanId`
- `featTypeCode`
- `acquisitionType`
- `acquisitionLevel` optional
- `acquisitionCause` optional
- `selectionIndex` optional
- `isActive` derived, not persisted
- `createdAt`
- `updatedAt`

### 7.3 Acquisition source

`FeatAcquisitionSource`

Represents how the feat was selected.

Suggested values:

- `LEVEL_UP`
- `OTHER`

Rules:

- `LEVEL_UP` requires `acquisitionLevel`.
- `OTHER` requires `acquisitionCause`.
- Exactly one acquisition source must be stored per feat instance.

### 7.4 Caravan relationship

The caravan campaign remains the aggregate root.

The caravan owns the list of `CaravanFeat` instances.

The feat catalog itself is read-only reference data.

## 8. Feat Catalog Scope

The initial catalog must include the feats documented in `docs/Reglas_de_Caravana.md` under **Dotes**.

The implementation must preserve the rule text and metadata as the source of truth for:

- prerequisites,
- benefits,
- special clauses,
- repeatability,
- selection limits,
- any other availability rule.

If the rules document is later expanded with new feats, the catalog must be able to grow without changing the UI contract.

## 9. UX / Flow

### 9.1 Accessing the feature

1. The user opens the active caravan workspace.
2. The user navigates to the feats section.
3. The user sees the feat overview list.

### 9.2 Adding a feat

1. The user opens the add-feat flow.
2. The user browses the feat catalog.
3. The user selects a feat type.
4. The user chooses the acquisition source.
5. If the source is level-up, the user enters the level.
6. If the source is other-cause, the user enters the cause.
7. The user confirms the action.
8. The system creates the owned feat and refreshes the view.

### 9.3 Editing a feat

1. The user opens the detail modal or edit action for an owned feat.
2. The user updates the acquisition source metadata.
3. The system validates the updated data.
4. The user confirms the change.
5. The system saves the updated feat metadata and refreshes the list.

### 9.4 Inspecting feat activity

1. The user reviews the feat list.
2. The UI shows whether each feat is active or inactive.
3. If a feat is inactive, the UI shows the rule reason in a readable form.

## 10. API Expectations

The implementation should be structured so the backend can support at least the following use cases:

- list feat catalog entries,
- fetch feat catalog details,
- list feats for a caravan,
- add a feat to a caravan,
- fetch a specific owned feat,
- update an owned feat’s acquisition metadata,
- validate feat availability before creating or updating the owned feat,
- evaluate whether a feat is currently active.

Suggested HTTP endpoints, if needed:

- `GET /api/caravans/{caravanId}/feats/catalog`
- `GET /api/caravans/{caravanId}/feats`
- `GET /api/caravans/{caravanId}/feats/{featId}`
- `POST /api/caravans/{caravanId}/feats`
- `PUT /api/caravans/{caravanId}/feats/{featId}`
