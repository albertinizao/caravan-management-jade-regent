# Caravan Traveler Management

**Status:** Draft  
**Target:** GestionCaravana  
**Type:** Feature specification

## 1. Summary

The application must allow the user to manage the **travelers** belonging to the active caravan.

The feature must provide:

- a traveler list view,
- filtering by active role and wagon,
- search by name,
- a detail modal for each traveler,
- the ability to add a new traveler,
- the ability to assign a traveler to a wagon,
- the ability to change the traveler’s active role,
- role-specific handling for cases such as `servant`, where another traveler must be selected as the person being served.

This feature operates inside the active caravan context and depends on the caravan instance specification.

## 2. Problem Statement

The user needs a single place to understand who is in the caravan, what each traveler is currently doing, and where each traveler is assigned.

Without a dedicated traveler management view:

- it is difficult to see the current composition of the caravan,
- it is hard to know who is performing each role,
- wagon assignment and role assignment are hidden across different parts of the application,
- searching for one traveler by name requires manual inspection of unrelated screens,
- contract and consumption data are not visible together with the rest of the traveler state.

## 3. Goals

1. Show all travelers of the active caravan in a dedicated list.
2. Allow filtering the list by active role and wagon.
3. Allow searching travelers by name.
4. Open a detail modal with the full traveler information.
5. Allow assigning a traveler to a wagon from the detail modal.
6. Allow changing the traveler’s active role from the detail modal.
7. Support role-specific dependencies, including selecting a target traveler for roles such as `servant`.
8. Allow creating a new traveler from a modal without leaving the traveler list.
9. Persist traveler data and keep it scoped to the active caravan.

## 4. Non-Goals

This specification does **not** define:

- combat resolution,
- full caravan scheduling or daily automatic role rotation,
- recruitment economics beyond traveler creation data entry,
- family-tree management,
- relationship simulation beyond what is required by the selected role,
- wagon creation or wagon rule management,
- UI for contract negotiation workflows outside this traveler form,
- multiplayer synchronization.

## 5. User Stories

### US-1: Browse travelers

As a user, I want to see all travelers in the caravan so that I can understand the current caravan roster.

### US-2: Search travelers by name

As a user, I want to search travelers by name so that I can quickly find a specific person.

### US-3: Filter travelers by role and wagon

As a user, I want to filter the traveler list by active role and wagon so that I can inspect only the subset I care about.

### US-4: Inspect full traveler details

As a user, I want to open a traveler detail modal so that I can review all relevant data without leaving the list.

### US-5: Add a traveler

As a user, I want to add a new traveler from the traveler screen so that I can grow the caravan roster.

### US-6: Assign a traveler to a wagon

As a user, I want to assign a traveler to a wagon so that I can manage where they travel.

### US-7: Change a traveler’s active role

As a user, I want to change the role that a traveler is currently exercising so that I can adapt the caravan to the current situation.

### US-8: Configure role-specific dependencies

As a user, I want the UI to ask me for any required target traveler when the chosen role needs one so that the role assignment remains valid.

## 6. Functional Requirements

### 6.1 Active caravan prerequisite

- The traveler feature must operate on the currently selected caravan.
- If no caravan is active, the application must not allow traveler management actions and must direct the user to select or create a caravan first.

### 6.2 Traveler list

- The system must provide a dedicated traveler list view for the active caravan.
- The list must show one row per traveler belonging to the caravan.
- Each row must display at least:
  - a short traveler name for quick scanning,
  - the traveler’s active role,
  - the wagon the traveler is currently assigned to, if any.
- The list must support:
  - filtering by active role,
  - filtering by wagon,
  - searching by name.
- The search must be case-insensitive and should match partial names.
- Filters must be combinable.
- The list must show a clear empty state when there are no travelers or when the active filters match nothing.

### 6.3 Traveler detail modal

- Clicking a traveler must open a modal with the full traveler details.
- The modal must show at least:
  - full name,
  - description,
  - current role,
  - available roles,
  - wagon assignment,
  - salary,
  - consumption,
  - contract conditions, if present.
- The modal must not require a page navigation.
- The modal must be dismissible without changing data.

### 6.4 Add traveler flow

- The traveler screen must provide an `Add` button.
- Clicking `Add` must open a modal for creating a new traveler.
- The creation modal must allow the user to enter:
  - full name,
  - description,
  - available roles,
  - salary,
  - contract conditions,
  - consumption.
- Consumption must default to `1`.
- The full name must be required.
- Description and contract conditions may be optional.
- The traveler must be created inside the active caravan.
- The traveler must persist after creation and appear in the list immediately.

### 6.5 Available roles

- The system must allow the user to select multiple roles that the traveler can potentially exercise.
- The list of available roles must be based on the role catalog defined by the game rules and expanded when the rules grow.
- The UI must not hard-code the role list in a way that prevents future additions.
- The active role must always be one of the available roles.
- If the selected role has prerequisites that are not satisfied, the role must not be selectable.

### 6.6 Assigning a traveler to a wagon

- The detail modal must include an `Assign to wagon` action.
- The action must present the wagons of the active caravan.
- The user must be able to select the wagon where the traveler will travel.
- The selected wagon must be persisted on the traveler.
- If the traveler is already assigned to a wagon, selecting another wagon must reassign the traveler.
- The system must validate the assignment against wagon constraints and any role-specific restrictions that depend on the wagon.

### 6.7 Changing the active role

- The detail modal must allow changing the traveler’s active role.
- The role selector must show only the roles available to that traveler.
- When the user selects a new role, the system must persist that role as the traveler’s active role.
- If the selected role requires additional data, the UI must request that data before saving.

### 6.8 Role-specific requirements

- Some roles may require extra contextual data.
- For roles such as `servant`, the user must select the other traveler being served.
- The target traveler must belong to the same active caravan.
- The target traveler must not be the same traveler being edited.
- The role-specific requirement must be validated before saving.
- The selected target must be visible again in the detail modal after saving.

### 6.9 Contract data

- The traveler detail modal must show the traveler’s salary and contract conditions if they exist.
- The add traveler modal must allow capturing both values.
- Salary and contract conditions are optional from a persistence perspective unless the business rules for a specific traveler require them.
- When no contract exists, the detail modal must present a clear empty state instead of misleading placeholder values.

### 6.10 Data integrity

- Travelers must be scoped to a single caravan.
- Managing travelers in one caravan must not mutate the travelers of another caravan.
- Wagon assignments, role assignments, and contract data must be stored with the traveler record or with a traveler-owned sub-structure.
- The UI must consume canonical traveler data from the backend, not duplicated ad hoc state.

## 7. Domain Model

### 7.1 Aggregate ownership

The caravan campaign remains the aggregate root.

Travelers are owned by the caravan campaign and are persisted within that scope.

### 7.2 Traveler

`Traveler`

Represents a person or creature that belongs to the caravan and can exercise one or more roles.

Suggested attributes:

- `id`
- `caravanId`
- `fullName`
- `description`
- `availableRoleCodes`
- `activeRoleCode`
- `roleSpecificData`
- `wagonId` optional
- `salary` optional
- `contractConditions` optional
- `consumption`
- `createdAt`
- `updatedAt`

### 7.3 Role-specific data

`TravelerRoleData`

Represents the payload needed by the currently active role.

Suggested attributes:

- `roleCode`
- `servedTravelerId` optional
- other role-specific fields as future rules require

### 7.4 Contract

`TravelerContract`

Represents the negotiated or assigned contract data for the traveler.

Suggested attributes:

- `salary`
- `conditions`
- `startedAt` optional
- `endedAt` optional

The contract may be absent entirely. In that case, the traveler still exists and can still be managed.

### 7.5 Role catalog

`TravelerRoleCatalogItem`

Represents a role definition that can be shown to the user.

Suggested attributes:

- `code`
- `name`
- `description`
- `requirements`
- `needsTargetTraveler`

The role catalog must be extensible because the game rules may introduce additional roles later.

## 8. Traveler Catalog Scope

The initial role catalog must include the traveler roles documented in `docs/Reglas_de_Caravana.md`.

The implementation must keep the rule data as the source of truth for:

- role names,
- role descriptions,
- prerequisites,
- special restrictions,
- target-traveler requirements.

The UI may present simplified labels, but it must not invent role behavior that conflicts with the rules.

## 9. UX / Flow

### 9.1 Opening the traveler screen

1. The user opens the active caravan workspace.
2. The user navigates to the travelers section.
3. The user sees a list of travelers.

### 9.2 Searching and filtering

1. The user types part of a name in the search box.
2. The list updates to show only matching travelers.
3. The user can combine the search with role and wagon filters.

### 9.3 Inspecting a traveler

1. The user clicks a traveler row.
2. The system opens the detail modal.
3. The modal shows the full traveler data.
4. The user can assign a wagon or change the active role from the same modal.

### 9.4 Assigning a wagon

1. The user clicks `Assign to wagon`.
2. The system opens a wagon picker with the active caravan wagons.
3. The user selects a wagon.
4. The system saves the assignment and refreshes the modal and list.

### 9.5 Changing the active role

1. The user opens the role selector.
2. The system shows the traveler’s available roles.
3. If the chosen role needs another traveler, the UI prompts for that target.
4. The user confirms the change.
5. The system saves the new active role.

### 9.6 Adding a traveler

1. The user clicks `Add`.
2. The system opens the creation modal.
3. The user enters the required data.
4. The user confirms creation.
5. The system creates the traveler and adds it to the list.

## 10. API Expectations

The implementation should be structured so the backend can support at least the following use cases:

- list travelers for the active caravan,
- search and filter travelers by role and wagon,
- fetch a specific traveler,
- create a traveler,
- update a traveler’s wagon assignment,
- update a traveler’s active role,
- fetch the role catalog or role metadata needed by the UI,
- validate role-specific requirements before persisting.

Suggested HTTP endpoints, if HTTP exposure is needed:

- `GET /api/caravans/{caravanId}/travelers?query=&roleCode=&wagonId=`
- `GET /api/caravans/{caravanId}/travelers/{travelerId}`
- `POST /api/caravans/{caravanId}/travelers`
- `PATCH /api/caravans/{caravanId}/travelers/{travelerId}`
- `GET /api/caravans/{caravanId}/travelers/roles/catalog`

The exact endpoint names may change, but the use cases must remain available.

## 11. Acceptance Criteria

### List, search, and filter

- The user can see the travelers of the active caravan in a dedicated list.
- The user can filter by active role.
- The user can filter by wagon.
- The user can search by name.
- The filters work together.

### Detail modal

- Clicking a traveler opens a modal.
- The modal shows the full traveler details.
- The modal includes contract information when present.
- The modal can be closed without changing data.

### Add traveler

- The user can create a new traveler from the traveler screen.
- The new traveler appears in the list immediately after saving.
- The consumption field defaults to `1`.

### Wagon assignment

- The user can assign a traveler to a wagon from the detail modal.
- The selected wagon is persisted and shown in the list and detail views.

### Role management

- The user can change the traveler’s active role.
- The role selector only shows available roles.
- When the role requires another traveler, the UI asks for that target before saving.

### Persistence and integrity

- Travelers persist with the active caravan.
- Travelers from different caravans do not interfere with each other.
- Invalid role assignments or wagon assignments are rejected with clear feedback.

## 12. Edge Cases

1. The active caravan exists but has no travelers.
2. The active caravan exists but no wagons are available for assignment.
3. The search text matches no traveler.
4. The user tries to assign a role that requires prerequisites the traveler does not satisfy.
5. The user tries to set `servant` without selecting a target traveler.
6. The selected target traveler is removed or becomes invalid before saving.
7. The user tries to assign a traveler to a wagon that is already full or otherwise invalid.
8. The active role is no longer valid after a rule change.
9. The traveler has no contract, so the modal must display empty contract state instead of misleading defaults.

## 13. Risks and Open Questions

1. Should the traveler list support sorting in the first release?
2. Should wagons be shown as a flat select list or grouped by wagon type?
3. Should the contract be an optional free-text field only, or should it later become a structured object?
4. Should the system allow a traveler to be created without any available roles, or should at least one role be required?
5. Should `consumption` be editable after creation, or only at creation time?

## 14. Implementation Notes

- Keep travelers as caravan-owned domain data, not as a standalone global entity.
- Keep available roles and active roles separate in the domain model.
- Treat role-specific requirements as validation rules, not as UI-only hints.
- Reuse the active caravan and wagon use cases instead of duplicating their logic in the traveler screen.
- Add or update architecture boundary tests if new packages are introduced.
- Keep the role picker, wagon picker, and traveler detail modal in the frontend, but source all data from backend use cases.

## 15. Related Source Material

- `docs/Reglas_de_Caravana.md` — traveler roles, role restrictions, and caravan rules
- `openspec/specs/caravan-instance/spec.md` — active caravan context
- `openspec/specs/caravan-wagons/spec.md` — wagon data and wagon assignment context
- `docs/architecture.md` — repository architecture guidelines
