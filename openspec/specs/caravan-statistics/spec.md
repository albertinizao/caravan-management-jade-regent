# Caravan Statistics Initialization and Breakdown

**Status:** Draft  
**Target:** GestionCaravana  
**Type:** Feature specification

## 1. Summary

The application must allow the user to define the **initial statistics** of a caravan when it is created and later inspect the caravan as a complete rule-driven statistics sheet.

The feature must expose:

- the main stats defined by the rules,
- the derived stats calculated from those main stats,
- the other gameplay attributes that depend on the caravan configuration,
- the current discontent value and its danger threshold,
- the rule-based bonuses and penalties coming from roles, wagons, beasts, feats, cargo, and any other applicable source.

The current caravan view must therefore move from a simple summary card to a full statistics breakdown with traceable contributions.

## 2. Problem Statement

Right now the user can create a caravan with only a name and description, and the dashboard only shows a shallow summary of a few values.

That is not enough because:

- the caravan begins with **1 point in each main stat plus 3 points to distribute**, and that allocation must be controlled intentionally,
- derived values such as attack, security, determination, speed, capacity, and consumption depend on the current caravan configuration,
- bonuses from roles, wagons, improvements, feats, and other rules can change the effective values,
- discontent has its own gameplay impact and must be visible together with its threshold against moral,
- without a full breakdown, the user cannot understand **why** a statistic has a given final value.

## 3. Goals

1. Allow the user to configure the caravan's initial main-stat allocation during creation.
2. Enforce the rule-based limits for the initial allocation.
3. Persist the created caravan with the exact starting values that were chosen.
4. Expose a full caravan statistics sheet for the active caravan.
5. Show main stats, derived stats, other attributes, and discontent in one coherent view.
6. Apply all relevant rule-based modifiers from caravan composition and related entities.
7. Surface enough breakdown data for the user to understand where each final value comes from.
8. Keep the backend as the canonical source of truth for all calculated values.

## 4. Non-Goals

This specification does **not** define:

- full combat resolution,
- travel turn resolution,
- encounter handling,
- detailed rulebook balancing changes,
- post-creation stat respec or point reallocation,
- multiplayer synchronization,
- cloud synchronization.

The spec focuses on **initial allocation** and **stat visibility**, not on changing the fundamental rule set.

## 5. User Stories

### US-1: Configure the caravan's starting stats

As a user, I want to distribute the caravan's starting points during creation so that I can shape the caravan from the beginning.

### US-2: See the full caravan stat sheet

As a user, I want to inspect all caravan stats in one place so that I can understand the caravan's current rule-driven state.

### US-3: Understand where modifiers come from

As a user, I want to see which roles, wagons, beasts, feats, and other sources affect a statistic so that I can trace the final value.

### US-4: Track discontent against morale

As a user, I want to see discontent and its relation to morale so that I can react before the caravan reaches a mutiny threshold.

### US-5: See updated values after caravan changes

As a user, I want the displayed statistics to refresh when caravan composition changes so that the values remain accurate.

## 6. Functional Requirements

### 6.1 Caravan creation with initial stat allocation

- The caravan creation flow must accept the caravan name and optional description.
- The creation flow must also support allocating the initial main-stat points.
- The default starting state is:
  - Offense: `1`
  - Defense: `1`
  - Mobility: `1`
  - Morale: `1`
  - Unassigned main-stat points: `3`
- The user may distribute the 3 unassigned points among the main stats before creating the caravan.
- The system must reject any allocation that:
  - spends more than the available 3 points,
  - produces a stat below `0`,
  - produces a stat above `10`,
  - omits a required main stat,
  - contains inconsistent or malformed data.
- If the user does not change the defaults, the caravan must be created with the canonical `1 / 1 / 1 / 1 + 3` starting state.
- The saved caravan must persist the exact chosen starting values.

### 6.2 Main stats

- The caravan must expose the four main stats:
  - Offense,
  - Defense,
  - Mobility,
  - Morale.
- The UI must show the current value of each main stat and the remaining unassigned points.
- If a later rule modifies a main stat, the view must show the effective value and the source of the modifier.
- Main stats must remain bounded by the rule-defined limits.

### 6.3 Derived stats

- The caravan must expose the derived stats defined by the rules, at minimum:
  - Attack,
  - Armor Class,
  - Security,
  - Determination.
- Each derived stat must be calculated from the caravan's current state, not manually entered by the user.
- The calculation must include all applicable rule-based modifiers from the active caravan configuration.
- The UI must show the final derived value and, where possible, the contributing sources.

### 6.4 Other gameplay attributes

- The caravan must expose the other rule-driven attributes that are relevant to gameplay, at minimum:
  - Level,
  - Speed,
  - Discontent,
  - Traveler capacity,
  - Cargo capacity,
  - Consumption,
  - Traveler count,
  - Wagon count,
  - Beast count.
- For each attribute, the system must apply the rules that combine base values with modifiers from the caravan's current composition.
- The UI must show the final value and enough context to explain the calculation.
- When a rule depends on thresholds or counts, the UI must surface the threshold or dependency explicitly.

### 6.5 Discontent

- The caravan must always expose the current discontent value.
- The UI must show the discontent value together with the caravan's morale so the user can compare both values directly.
- The UI must warn when discontent reaches or exceeds morale, because that is the mutiny threshold defined by the rules.
- Any rule that increases, decreases, or temporarily affects discontent must be reflected in the displayed value.
- The stat sheet must preserve the current discontent even when the caravan has no wagons, travelers, or beasts yet.

### 6.6 Rule-based modifier application

- The statistics engine must apply modifiers from all rule-bearing sources that affect the caravan.
- The feature must support at least these source categories:
  - traveler roles,
  - wagons,
  - wagon improvements,
  - beasts or beast assignments,
  - feats,
  - cargo or carried items,
  - other future rule sources defined by the game rules.
- The system must apply the rulebook's stacking rules, limits, prerequisites, and exclusions.
- When multiple sources affect the same statistic, the final value must follow the canonical rule order defined by the domain.
- The backend must remain the source of truth; the frontend must not duplicate the calculation logic.

### 6.7 Refresh behavior

- The statistics view must refresh after caravan creation and after any later change that can affect the caravan's values.
- Relevant change examples include:
  - selecting or deselecting the active caravan,
  - adding or removing travelers,
  - changing traveler roles,
  - adding or removing wagons,
  - adding or removing wagon improvements,
  - adding or removing beasts,
  - adding or removing feats or other stat-bearing features,
  - any future rule-driven state change.
- The refreshed values must be visible without requiring a full page reload.

### 6.8 Data integrity

- The initial main-stat allocation must be validated before persistence.
- Derived values must not be editable as raw inputs.
- The system must avoid storing stale calculated values unless they are explicitly used as a cache.
- If cached, derived values must be invalidated whenever a relevant source changes.
- Unknown legacy modifiers should not break the full stat sheet; they should be surfaced as unknown or ignored according to the backend contract.

## 7. Domain Model

### 7.1 Aggregate root

`CaravanCampaign`

The caravan campaign remains the aggregate root and the source of truth for the statistics view.

### 7.2 Main-stat value object

`CaravanMainStats`

Represents the caravan's main-stat allocation and unassigned points.

Suggested attributes:

- `offense`
- `defense`
- `mobility`
- `morale`
- `unassignedPoints`

### 7.3 Statistics read model

`CaravanStatisticsView`

Represents the full stat sheet returned to the UI.

Suggested attributes:

- `caravanId`
- `level`
- `mainStats`
- `derivedStats`
- `otherStats`
- `discontent`
- `sourceBreakdown`
- `warnings`
- `updatedAt`

### 7.4 Statistic breakdown entry

`CaravanStatContribution`

Represents one rule source that contributes to a final statistic.

Suggested attributes:

- `statCode`
- `sourceType`
- `sourceId`
- `sourceName`
- `modifier`
- `operation`
- `reason`

### 7.5 Calculation model

The calculation model should allow the backend to return both:

- the final values, and
- the list of contributions that produced those values.

That traceability is essential for understanding rule-based stacking and for debugging future balancing changes.

## 8. UX / Flow

### 8.1 Creating a caravan

1. The user opens the create caravan form.
2. The form shows the default main stats and the 3 distributable points.
3. The user allocates the initial points or keeps the default values.
4. The system validates the allocation.
5. The caravan is created and stored.
6. The newly created caravan appears with its stat sheet already populated.

### 8.2 Inspecting the caravan stats

1. The user opens the active caravan dashboard or a dedicated statistics tab.
2. The UI displays the main stats, derived stats, other attributes, and discontent.
3. Each statistic shows its effective value.
4. Where available, the UI also shows the source contributions and any relevant warnings.

### 8.3 Understanding a changed statistic

1. The user notices a statistic has changed.
2. The UI shows which source categories contributed to the value.
3. The user can navigate from the summary to the related wagon, traveler, beast, or feat view if deeper inspection is needed.

## 9. API Expectations

The implementation should be structured so the backend can support at least the following use cases:

- create a caravan with an explicit initial main-stat allocation,
- fetch the full caravan stat sheet,
- fetch the caravan's current discontent and threshold context,
- refresh derived values after caravan composition changes.

Suggested HTTP endpoints, if needed:

- `POST /api/caravans`
- `GET /api/caravans/{id}`
- `GET /api/caravans/{id}/statistics`

The exact endpoint naming can change, but the use cases must remain available.

## 10. Acceptance Criteria

### Initial allocation

- A user can create a caravan with a controlled starting stat distribution.
- The default state is `1 / 1 / 1 / 1` with 3 unassigned points.
- Invalid allocations are rejected before persistence.
- The saved caravan keeps the exact chosen initial values.

### Full stat sheet

- The user can see all main stats.
- The user can see derived stats.
- The user can see other gameplay attributes.
- The user can see discontent and compare it against morale.

### Modifier traceability

- The UI shows which rule sources are affecting a statistic.
- The backend provides the canonical calculated values.
- The displayed values change when the caravan composition changes.

### Discontent safety

- The UI warns when discontent reaches morale.
- The user can inspect the current discontent value at any time.

### Data integrity

- Derived values remain consistent after reload.
- Unknown or legacy rule data does not break the statistics view.

## 11. Edge Cases

1. The user leaves the creation form with the default `1 / 1 / 1 / 1 + 3` state.
2. The user tries to spend more than 3 points during creation.
3. The user tries to create a stat above 10 or below 0.
4. The active caravan has no travelers, wagons, or beasts yet.
5. The caravan has sources that affect the same statistic through different categories.
6. A rule source is present but no longer recognized by the current version.
7. Discontent is exactly equal to morale.
8. Discontent is above morale and the UI must surface the danger clearly.
9. A change in travelers or wagons alters derived values while the user is on the dashboard.
10. A repeated source stacks only if the rulebook explicitly allows stacking.

## 12. Risks and Open Questions

1. Should the statistics endpoint return only final values, or also the full contribution breakdown for every stat?
2. Should the UI show all sources by default, or only expand them on demand?
3. Should derived values be computed on demand every time, or cached in the read model?
4. Should the creation form expose initial point allocation inline, or as a separate step after naming the caravan?
5. Which exact source categories beyond roles, wagons, feats, and beasts should be included in the first release?

## 13. Implementation Notes

- Keep main-stat allocation in the caravan creation flow, not as a hidden default only.
- Keep derived-stat calculation in the domain/application layer.
- Prefer a dedicated statistics read model instead of overloading the creation response.
- Add or update architecture boundary tests if new packages or ports are introduced.
- Reuse the rulebook as the canonical source for modifiers, limits, and stacking behavior.

## 14. Related Source Material

- `docs/Reglas_de_Caravana.md` — caravan rules, main stats, derived stats, discontent, wagons, feats, and roles
- `openspec/specs/caravan-instance/spec.md` — caravan creation and active context
- `openspec/specs/caravan-travelers/spec.md` — traveler roles and assignment
- `openspec/specs/caravan-wagons/spec.md` — wagon ownership and configuration
- `openspec/specs/caravan-load-beasts/spec.md` — beasts and assignment rules
- `openspec/specs/caravan-wagon-improvements/spec.md` — wagon improvement modifiers
- `docs/architecture.md` — repository architecture guidelines

