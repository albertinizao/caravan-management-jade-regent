# Caravan Cargo Management

**Status:** Draft  
**Target:** GestionCaravana  
**Type:** Feature specification

## 1. Summary

The application must allow the user to manage the **cargo** carried by the active caravan.

The feature must provide:

- a cargo catalog sourced from the game rules,
- the ability to add catalog cargo to the caravan,
- the ability to add custom cargo,
- the ability to delete cargo from the caravan,
- the ability to assign each cargo entry to a specific wagon,
- a dedicated overview that shows everything carried by the caravan at a glance,
- a wagon-centric breakdown that shows which cargo is carried by each wagon.

This feature operates inside the active caravan context and depends on the caravan instance and wagon management specifications.

## 2. Problem Statement

The user needs a single place to understand what the caravan is carrying, where each cargo item is stored, and whether the caravan still has enough wagon capacity available.

Without a dedicated cargo management view:

- cargo is invisible or scattered across different screens,
- the user cannot quickly see which wagon carries which load,
- the user cannot compare cargo entries with the available wagon capacity,
- cargo from the rules document is not represented in the application as manageable data,
- custom cargo cannot be tracked consistently with catalog cargo,
- deleting or reassigning cargo becomes error-prone.

## 3. Goals

1. Show the cargo of the active caravan in a dedicated list.
2. Allow adding cargo items from the documented cargo catalog.
3. Allow adding custom cargo entries not present in the catalog.
4. Allow deleting cargo entries from the caravan.
5. Allow assigning each cargo entry to a specific wagon.
6. Show an at-a-glance view of all cargo in the caravan and a breakdown by wagon.
7. Enforce wagon capacity and cargo-specific placement rules.
8. Keep cargo data persistent and scoped to the selected caravan.

## 4. Non-Goals

This specification does **not** define:

- selling cargo,
- trade resolution,
- automatic cargo spoilage over time,
- cargo splitting across multiple wagons within a single entry,
- cargo transfer between caravans,
- pricing logic for commerce,
- inventory weight simulation beyond the cargo units required by the caravan rules,
- multi-user synchronization,
- wagon purchase or wagon deletion workflows beyond their impact on cargo integrity.

## 5. User Stories

### US-1: Browse the cargo catalog

As a user, I want to see the items that can be carried according to the rules so that I can add them to my caravan.

### US-2: Add catalog cargo

As a user, I want to add a documented cargo item to my caravan so that I can manage it as part of the caravan inventory.

### US-3: Add custom cargo

As a user, I want to add cargo that is not present in the catalog so that I can track special or campaign-specific items.

### US-4: Assign cargo to a wagon

As a user, I want to choose which wagon carries each cargo entry so that I can keep the caravan organized.

### US-5: Delete cargo

As a user, I want to remove cargo from the caravan so that I can keep the inventory accurate.

### US-6: Review cargo at a glance

As a user, I want to see the full caravan cargo and a per-wagon breakdown so that I can understand capacity usage immediately.

### US-7: Manage cargo from wagon detail

As a user, I want to inspect, add, and delete the cargo of a specific wagon from that wagon’s detail view so that I can work without switching screens.

## 6. Functional Requirements

### 6.1 Active caravan prerequisite

- The cargo feature must operate on the currently selected caravan.
- If no caravan is active, the application must not allow cargo management actions and must direct the user to select or create a caravan first.

### 6.2 Cargo catalog

- The system must expose the full cargo catalog defined by the game rules.
- The catalog must include every item listed in `docs/Reglas_de_Caravana.md` under **Cargamento Adquirible**.
- The initial catalog must include, at minimum:
  - **Artículos de mejora**
    - Estufa
    - Altar Sagrado
    - Cocina Portátil
    - Equipo Para Climas Fríos
    - Pabellón De Entretenimiento
    - Piedra De Comunicación
    - Tenderete
    - Tienda De Campaña De Supervivencia
    - Trampas De Campamento
  - **Artículos de mercancía**
    - Carbón
    - Hielo
    - Leña
    - Material De Reparaciones
    - Mercancías
    - Mercancías Específicas
    - Materiales Mágicos Diversos
    - Mercancías Locales
    - Suministros
    - Suministros Perecederos
    - Tesoro
  - **Munición**
    - Munición De Balista
    - Munición Perforadora De Balista
    - Munición De Cañón
    - Munición De Draco De Fuego
- Each catalog entry must preserve the source rule data for:
  - name,
  - category,
  - price, when defined,
  - cargo units,
  - fixed quantity or stack quantity, when defined,
  - description,
  - benefit text,
  - notes,
  - any transport restriction or special metadata requirement.
- The catalog must be extensible because the rules document may grow later.

### 6.3 Catalog cargo creation flow

- The user must be able to create a cargo entry from a catalog item.
- The creation flow must support catalog items that require additional metadata, such as:
  - an origin for **Mercancías Locales**,
  - a specific commodity selection for **Mercancías Específicas**,
  - a deity for **Altar Sagrado**,
  - any other per-item data required by the rules.
- The UI must request the required metadata before saving.
- The created cargo entry must be persisted inside the active caravan.
- The created cargo entry must be assigned to exactly one wagon of the active caravan before it is saved.

### 6.4 Custom cargo creation flow

- The user must be able to create cargo that does not come from the catalog.
- The custom cargo flow must allow the user to enter at least:
  - a display name,
  - the cargo amount or quantity,
  - the cargo units consumed,
  - the target wagon,
  - optional notes,
  - optional origin or context metadata when useful.
- Custom cargo must be stored as caravan-owned data and must not mutate the catalog.
- Custom cargo must appear in the same overview as catalog cargo.

### 6.5 Cargo deletion

- The cargo view must allow the user to delete a cargo entry from the caravan.
- Deletion must require explicit confirmation.
- Deleting cargo must not affect other caravans.
- Deleting cargo must immediately free the wagon capacity consumed by that entry.

### 6.6 Wagon assignment

- Each cargo entry must belong to exactly one wagon of the active caravan.
- The user must be able to assign or reassign a cargo entry to a different wagon.
- The selected wagon must belong to the active caravan.
- The target wagon must have enough free cargo capacity for the cargo entry.
- The system must reject assignments that violate wagon-specific restrictions from the rules document.
- If a cargo entry is reassigned, the source wagon’s capacity must be updated accordingly.

### 6.7 Capacity validation

- The system must prevent cargo assignments that exceed the selected wagon’s available cargo capacity.
- The system must reflect wagon capacity as defined by the wagon rules and any applicable wagon improvements already supported by the application.
- The system must show a clear validation message when the user tries to exceed capacity.
- The system must also prevent creating cargo when the active caravan has no available wagon that can legally carry it.

### 6.8 Cargo overview

- The application must provide a dedicated cargo view for the active caravan.
- The view must show one row per cargo entry.
- Each row must display at least:
  - cargo name,
  - source type, meaning catalog or custom,
  - category,
  - cargo amount or quantity,
  - cargo units consumed,
  - assigned wagon,
  - any special cargo restriction that affects placement,
  - optional notes.
- The overview must show a clear empty state when the caravan has no cargo.
- The overview must update after create, delete, or reassignment actions without requiring a full application restart.

### 6.9 Wagon-centric breakdown

- The cargo view must provide a quick way to inspect the cargo by wagon.
- The UI must support filtering by wagon.
- The UI should also show the wagon’s cargo usage summary, including at least:
  - used cargo units,
  - remaining cargo units,
  - total cargo entries assigned to that wagon.
- A grouped or tabular presentation is acceptable as long as the wagon contents can be understood at a glance.

### 6.10 Data integrity

- Cargo must be scoped to a single caravan.
- Managing cargo in one caravan must not mutate the cargo of another caravan.
- Cargo entries must reference wagons by caravan-owned identifiers.
- The UI must consume canonical cargo data from the backend, not duplicated ad hoc state.
- If a wagon is deleted while cargo is assigned to it, the system must block the deletion or require the user to move/delete the cargo first.

### 6.11 Wagon detail cargo panel

- The wagon detail view must show the cargo currently assigned to that wagon.
- The wagon detail view must allow deleting each cargo entry assigned to that wagon.
- The wagon detail view must allow adding new cargo directly to the currently selected wagon without manually choosing another wagon.
- The wagon detail cargo actions must reuse the same validation rules as the dedicated cargo view.
- Only cargo entries valid for the selected wagon may be offered in the wagon detail add flow.

## 7. Domain Model

### 7.1 Aggregate ownership

The caravan campaign remains the aggregate root.

Cargo entries are owned by the caravan campaign and are persisted within that scope.

### 7.2 Cargo catalog item

`CargoCatalogItem`

Represents a catalog entry defined by the game rules.

Suggested attributes:

- `code`
- `name`
- `category`
- `price` optional
- `cargoUnits`
- `quantityLabel` optional
- `description`
- `benefitText` optional
- `notes` optional
- `requiresMetadata` boolean
- `requiredMetadataKeys`
- `allowedWagonCodes` optional
- `restrictedToWagonCategory` optional

### 7.3 Caravan cargo entry

`CaravanCargo`

Represents a cargo item currently being carried by the caravan.

Suggested attributes:

- `id`
- `caravanId`
- `sourceType` (`CATALOG` or `CUSTOM`)
- `catalogCode` optional
- `displayName`
- `category`
- `amount`
- `cargoUnits`
- `wagonId`
- `metadata`
- `notes` optional
- `createdAt`
- `updatedAt`

### 7.4 Cargo metadata

`CargoMetadata`

Represents per-item data required by some catalog entries.

Suggested attributes:

- `origin` optional
- `deity` optional
- `commodityType` optional
- `stackLabel` optional
- any future rule-specific fields

### 7.5 Wagon cargo summary

`CaravanWagonCargoSummary`

Represents the aggregated cargo load for one wagon.

Suggested attributes:

- `wagonId`
- `wagonName`
- `usedCargoUnits`
- `remainingCargoUnits`
- `cargoEntryCount`

## 8. Catalog Scope

The initial cargo catalog must be sourced from `docs/Reglas_de_Caravana.md` and must preserve the rules document as the source of truth for:

- item names,
- item categories,
- prices,
- cargo-unit values,
- fixed stack quantities,
- special benefit text,
- descriptive text,
- item-specific restrictions,
- item-specific metadata requirements.

Examples of item-specific rules that must be preserved:

- **Tesoro** is not purchased normally and has variable cargo usage, but it has no special wagon restriction beyond normal capacity rules.
- **Mercancías Locales** require an origin and use that origin in their trading behavior.
- **Mercancías Específicas** require the specific commodity to be chosen when created.
- **Suministros** and **Suministros Perecederos** have different behavior despite sharing the same general category.
- **Munición** entries represent stackable ammunition with fixed quantities and have no special wagon restriction beyond normal capacity rules.
- The **Carro De Suministros** has an inverse restriction: it may only carry **Suministros** or **Suministros Perecederos**.
- Any cargo item with a wagon restriction must expose that restriction in the UI and enforce it in the backend.

## 9. UX / Flow

### 9.1 Opening the cargo screen

1. The user opens the active caravan workspace.
2. The user navigates to the cargo section.
3. The user sees the cargo overview table and the wagon load summary.

### 9.2 Adding catalog cargo

1. The user opens the add-cargo flow.
2. The user browses the cargo catalog.
3. The user reviews the cargo details.
4. If the item requires extra metadata, the UI requests it.
5. The user selects the wagon that will carry the cargo.
6. The user confirms the creation.
7. The system creates the cargo entry and refreshes the overview.

### 9.3 Adding custom cargo

1. The user opens the custom cargo form.
2. The user enters the cargo details.
3. The user selects the wagon.
4. The user confirms the creation.
5. The system creates the cargo entry and refreshes the overview.

### 9.4 Reassigning cargo

1. The user opens the cargo detail view or inline action.
2. The user selects a different wagon.
3. The system validates the target wagon capacity and restrictions.
4. The system saves the reassignment.
5. The overview and wagon summary update immediately.

### 9.5 Deleting cargo

1. The user clicks delete on a cargo entry.
2. The system shows a confirmation prompt.
3. The user confirms deletion.
4. The system removes the cargo entry and updates the wagon summary.

### 9.6 Managing cargo from wagon detail

1. The user opens a wagon detail view.
2. The user sees the cargo entries assigned to that wagon and the remaining cargo capacity.
3. The user can add catalog cargo or custom cargo directly to that wagon.
4. The UI only offers cargo options valid for the selected wagon.
5. The user can delete any cargo entry assigned to that wagon from the same view.
6. The wagon detail view refreshes its cargo panel immediately after each change.

### 9.6 Inspecting cargo by wagon

1. The user selects a wagon filter or opens a wagon grouping.
2. The system shows only the cargo carried by that wagon.
3. The user can clear the filter to return to the full caravan overview.

## 10. API Expectations

The implementation should be structured so the backend can support at least the following use cases:

- list the cargo catalog,
- fetch one cargo catalog item,
- list cargo entries for the active caravan,
- list cargo entries filtered by wagon,
- fetch a specific cargo entry,
- create cargo from a catalog item,
- create custom cargo,
- reassign a cargo entry to another wagon,
- delete a cargo entry,
- fetch per-wagon cargo summaries.

Suggested HTTP endpoints, if HTTP exposure is needed:

- `GET /api/caravans/{caravanId}/cargo/catalog`
- `GET /api/caravans/{caravanId}/cargo/catalog/{cargoCode}`
- `GET /api/caravans/{caravanId}/cargo?query=&wagonId=&sourceType=&category=`
- `GET /api/caravans/{caravanId}/cargo/{cargoId}`
- `POST /api/caravans/{caravanId}/cargo/catalog`
- `POST /api/caravans/{caravanId}/cargo/custom`
- `PATCH /api/caravans/{caravanId}/cargo/{cargoId}`
- `PUT /api/caravans/{caravanId}/cargo/{cargoId}/wagon`
- `DELETE /api/caravans/{caravanId}/cargo/{cargoId}`
- `GET /api/caravans/{caravanId}/cargo/summary`

The exact endpoint names may change, but the use cases must remain available.

## 11. Acceptance Criteria

### Catalog

- The user can see all cargo items defined by the rules document.
- The catalog reflects the documented cargo categories and item metadata.

### Add catalog cargo

- The user can add a cargo item from the catalog.
- If the item requires extra metadata, the UI asks for it before saving.
- The new cargo entry appears in the list immediately after saving.

### Add custom cargo

- The user can create a cargo entry that does not exist in the catalog.
- The new custom entry appears in the same overview as catalog cargo.

### Wagon assignment

- The user can assign cargo to a wagon.
- The user can reassign cargo to a different wagon.
- The selected wagon is persisted and shown in both the list and the wagon summary.

### Delete cargo

- The user can delete cargo after confirming the action.
- The cargo entry disappears immediately after deletion.

### Overview and breakdown

- The user can see the caravan cargo at a glance.
- The user can filter cargo by wagon.
- The user can see how much cargo each wagon is carrying.

### Validation and integrity

- The system rejects cargo assignments that exceed wagon capacity.
- The system rejects cargo assignments that violate wagon-specific restrictions, including the inverse rule for the **Carro De Suministros**.
- Cargo persists with the active caravan.
- Cargo from different caravans does not interfere with each other.

## 12. Edge Cases

1. The active caravan exists but has no wagons.
2. The active caravan exists but has no cargo.
3. The catalog search matches no item.
4. The user tries to add cargo that requires extra metadata but leaves a required field empty.
5. The user tries to assign cargo to a wagon that has no remaining capacity.
6. The user tries to assign cargo to a wagon that is not allowed by the cargo rules.
7. The user tries to delete a wagon that still has cargo assigned.
8. The cargo entry references a wagon that was removed or became invalid before save.
9. The user tries to create custom cargo with zero or negative cargo units.
10. The user filters by a wagon that carries no cargo.

## 13. Risks and Open Questions

1. Should cargo be represented as one row per stack, or should the UI split some items into multiple rows automatically?
2. Should custom cargo support a free-form category, or should it be constrained to a fixed set?
3. Should the cargo overview support sorting in the first release?
4. Should cargo allow partial reassignment between wagons, or only whole-entry moves?
5. Should the backend compute wagon capacity summaries eagerly or derive them on read?
6. Should the cargo feature expose deep-linkable routes for a specific wagon filter?

## 14. Implementation Notes

- Keep cargo as caravan-owned domain data, not as a standalone global inventory.
- Keep the cargo catalog as reference data, not as mutable domain state.
- Model wagon capacity checks as domain validation, not only as UI hints.
- Reuse the active caravan and wagon use cases instead of duplicating their logic in the cargo screen.
- Add or update architecture boundary tests if new packages are introduced.
- The cargo overview should be able to answer two questions quickly: what the caravan carries and which wagon carries it.

## 15. Related Source Material

- `docs/Reglas_de_Caravana.md` — cargo rules, item names, cargo units, and restrictions
- `openspec/specs/caravan-instance/spec.md` — active caravan context
- `openspec/specs/caravan-wagons/spec.md` — wagon data, capacity, and wagon constraints
- `docs/architecture.md` — repository architecture guidelines
