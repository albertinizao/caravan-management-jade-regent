# Caravan Daily Supply Cycle

**Status:** Draft  
**Target:** GestionCaravana  
**Type:** Feature specification

## 1. Summary

The application must allow the user to **pass one day** for the active caravan and have the game resolve the caravan’s daily supply cycle automatically.

This daily cycle must:

- subtract the caravan’s daily consumption,
- generate the supplies that are produced that day,
- apply the relevant rule-driven modifiers and limits,
- persist the resulting caravan state,
- and show the user a clear before/after breakdown of what happened.

The feature must account for the sources that affect supply economy in the current rule set, including at least:

- **Agricultors**,
- **Batidores**,
- **Sirvientes** when they amplify another traveler’s role,
- **Cocineros**,
- **Cocina Portátil**,
- and supply-related **dotes** such as **Autonomía Extrema**, **Ayuno Intermitente**, and **Consumo Eficiente**.

This feature is the missing time-progression layer between the current caravan state and the daily resource economy.

## 2. Problem Statement

Right now the application can show caravan statistics, travelers, wagons, feats, and cargo, but it does not provide a canonical “pass the day” action that resolves supply income and supply spending together.

That creates several problems:

- the user must mentally track the daily economy instead of letting the system do it,
- supply generation from roles and wagons can be forgotten or applied inconsistently,
- consumption can be shown as a stat but not resolved against the caravan’s stored reserves,
- the effect of cook efficiency, farm production, scouting/hunting batidores, and relevant dotes is easy to misapply,
- and repeated clicks or retries could accidentally double-advance the caravan if the operation is not modeled as a single authoritative day resolution.

In short: the app can show the numbers, but it cannot yet **close the day**.

## 3. Goals

1. Allow the user to advance the active caravan by exactly one day through a single action.
2. Resolve daily supply **consumption** and **generation** in the correct rule-driven order.
3. Apply the current caravan state as the source of truth for all daily modifiers.
4. Support daily choices that affect the result, such as batidor mode or optional feast/fasting decisions when the rules require a player choice.
5. Persist the updated caravan supply state and the day-resolution record.
6. Expose a clear breakdown of where supplies came from and where they were spent.
7. Keep the backend as the canonical rules engine for daily supply resolution.
8. Make the operation safe against duplicate submission.

## 4. Non-Goals

This specification does **not** define:

- full combat resolution,
- encounter generation,
- travel route navigation,
- settlement visit logic,
- long-rest healing,
- weather simulation beyond any already-existing cargo decay rules that must be honored when the day advances,
- or a full calendar/timekeeping system for the entire campaign beyond the caravan day counter needed for this feature.

The feature only defines the **daily caravan supply cycle**, not the entire world-time simulation.

## 5. User Stories

### US-1: Pass the day

As a user, I want to press a button to pass one day so that the caravan’s supplies are updated automatically.

### US-2: See the daily supply result

As a user, I want to see how many supplies were consumed and generated so that I can understand the caravan’s net daily balance.

### US-3: Understand rule sources

As a user, I want to see which travelers, wagons, feats, and dotes affected the daily result so that I can trust the calculation.

### US-4: Handle daily choices

As a user, I want the app to ask for any daily choices that matter so that I can control optional trade-offs such as hunting versus scouting or fasting versus normal consumption.

### US-5: Avoid accidental double-advance

As a user, I want the day pass action to be safe against repeated clicks or retries so that I do not lose supplies by advancing twice by mistake.

## 6. Functional Requirements

### 6.1 Active caravan prerequisite

- The day pass action must operate on the currently active caravan.
- If no caravan is active, the action must be disabled and the UI must direct the user to select or create a caravan first.
- All daily supply calculations must use the active caravan’s current travelers, wagons, cargo, feats, and discontent state.

### 6.2 Day pass entry point

- The UI must expose a clear button or action labeled in user language as **Pass Day** or equivalent.
- The action should be available from the main caravan workspace and from any view that already shows the active caravan summary.
- Before final confirmation, the UI must present a preview of the day resolution, including at minimum:
  - expected consumption,
  - expected production,
  - current supply reserve,
  - expected net delta,
  - and any warnings or blocking conditions.
- If the resolution requires a daily choice, the preview must include that choice before confirmation.

### 6.3 Daily resolution scope

- One confirmed action must resolve exactly **one caravan day**.
- The resolution must be atomic from the user’s perspective: either the entire day is applied, or none of it is.
- If the request is repeated with the same command identity or while the caravan is already resolved for that day, the backend must not apply the same day twice.
- The result must be persisted as a dedicated day-resolution record so the user can inspect what happened later.

### 6.4 Supply state model

- The caravan must maintain a canonical **supply reserve** for consumable provisions.
- The reserve must be queryable independently from the cargo inventory view.
- If the application stores supplies as cargo entries, the supply reserve and the cargo inventory must stay synchronized.
- The UI must show:
  - current reserve,
  - daily consumption,
  - daily generation,
  - net delta,
  - and shortage status when the reserve is insufficient.
- The system must distinguish between:
  - **stored provisions** available to be spent,
  - **cargo representation** of supplies,
  - and **future or pending production** that has been earned but not yet committed.

### 6.5 Daily consumption

- The engine must calculate the caravan’s daily consumption from the current caravan state.
- The calculation must include:
  - traveler consumption,
  - wagon consumption,
  - wagon-improvement modifiers that change consumption,
  - feat modifiers that change consumption,
  - and any daily choice that intentionally changes consumption, such as fasting or celebration.
- If the user enables **Ayuno Intermitente** without the feat being active, the backend must reject the resolution.
- Batidores assigned to the batidor role must not count toward caravan consumption while that role is active.
- Consumption modifiers that impose a floor must respect their rule-defined minimums.
- If the final consumption exceeds the available supply reserve, the caravan must enter a shortage state for that day and the UI must show the deficit clearly.

### 6.6 Daily generation

- The engine must calculate daily supply generation from the current caravan state.
- At minimum, it must support:
  - **Agricultors**: 1 unit of supplies every 2 days,
  - **Batidores**: 2 units of provisions when they hunt,
  - and any other supply-generating source already defined by the rulebook or added later.
- If the caravan has **Trabajo En Equipo**, the engine must resolve shared jobs using the productivity contract defined in `openspec/specs/caravan-feat-automation/spec.md`, including the 25% per additional traveler bonus and any persisted fractional carry-over.
- The engine must respect role availability rules, including wagon restrictions such as **Carro Huerto** for agriculturists.
- When cargo supplies are consumed, the backend must remove them from inventory and consume **suministros perecederos** before **suministros**.
- If a traveler’s role or target assignment is invalid on the day of resolution, that source must not generate supplies and the UI must report why.

### 6.7 Cook efficiency and kitchen support

- The engine must support the cook efficiency rule as a bonus on top of the base daily generation and on top of the spendable supply stock that is being consumed that day.
- Each cook may convert one full block of 10 supplies into 5 additional supplies, whether those supplies come from generation or from stored cargo/reserve being spent.
- The number of effective cook bonuses in a day is limited by the number of full 10-supply blocks available in each affected pool.
- If the caravan has a **Cocina Portátil**, the cook bonus is doubled.
- Cook output must be treated as a shared job bucket when multiple cooks are assigned, so **Trabajo En Equipo** can increase the effective cook bonus using the shared-job productivity contract.
- The resolution breakdown must show how many full blocks were converted, how many cooks were effective, and whether the kitchen bonus was applied.

### 6.8 Role synergies and support roles

- The engine must evaluate support roles that modify another traveler’s role, including **Sirviente**.
- If a sirviente is assigned to a valid master, and that master’s role is relevant to supply production or consumption, the synergy must be applied during the day resolution.
- If the master assignment is missing, invalid, or incompatible with the selected role, the sirviente contribution must be ignored and the UI must show the reason.
- The system must not require the frontend to reimplement the role-synergy rules.

### 6.9 Feats and dotes

- The day resolution must evaluate only active feats.
- The engine must apply supply-related dotes, including at least:
  - **Autonomía Extrema**,
  - **Ayuno Intermitente**,
  - **Consumo Eficiente**,
  - and any future feat that changes supply production or consumption.
- If a feat becomes inactive, it must stop contributing to the daily resolution immediately.
- If a feat requires a daily choice, the UI must surface that choice before confirmation.
- The resolution breakdown must indicate when a feat was available but not applied because its conditions were not met.

### 6.10 Time-based cargo effects

- Advancing the day must also advance any existing duration-based cargo effects that are already defined by the cargo system, including perishable supplies and cargo preservation rules.
- The feature must not silently ignore time-based cargo decay when the caravan day advances.
- **Suministros perecederos** must lose effectiveness every 2 days according to the rulebook, and the resulting cargo inventory must reflect the decay.
- If a cargo effect is affected by a wagon improvement, the result must reflect that improvement.

### 6.11 Discontent interaction

- The day pass action must preserve the caravan’s current discontent unless a rule explicitly changes it.
- If a daily choice increases discontent, that change must be included in the same atomic day resolution.
- The UI must highlight when discontent-related rules were involved in the day, especially when the user used a trade-off such as fasting.

### 6.12 Data integrity

- The system must never apply the same day twice.
- The system must never allow a negative supply reserve without also recording the shortage state.
- The system must never treat a disabled or inactive role as if it were active.
- The system must keep the daily resolution record immutable once saved.
- The backend must remain the source of truth for every calculated value shown in the preview and result screens.

## 7. Domain Model

### 7.1 Aggregate root

`CaravanCampaign`

The caravan campaign remains the aggregate root for day advancement. It owns the current day state, supply reserve, discontent, travelers, wagons, cargo, and rule-bearing features.

### 7.2 Supply state

`CaravanSupplyState`

Represents the caravan’s consumable resource state.

Suggested attributes:

- `provisionReserve`
- `cargoEquivalentReserve`
- `dailyConsumption`
- `dailyGeneration`
- `netDelta`
- `shortage`
- `lastResolvedDay`
- `updatedAt`

### 7.3 Day resolution record

`CaravanDayResolution`

Represents one completed day pass.

Suggested attributes:

- `id`
- `caravanId`
- `resolvedDayIndex`
- `resolvedAt`
- `startingReserve`
- `endingReserve`
- `totalConsumption`
- `totalGeneration`
- `netDelta`
- `shortage`
- `choices`
- `contributions`
- `warnings`

### 7.4 Contribution entry

`CaravanDailyContribution`

Represents one rule source that contributed to the day result.

Suggested attributes:

- `effectCode`
- `sourceType`
- `sourceId`
- `sourceName`
- `operation`
- `quantity`
- `reason`
- `applied`
- `ignoredReason` optional

### 7.5 Daily choice model

`CaravanDailyChoice`

Represents a user decision that affects the resolution.

Suggested attributes:

- `choiceType`
- `targetId` optional
- `mode`
- `value` optional
- `reason` optional

Examples:

- batidor chooses between hunting and scouting,
- a fasting feat is enabled or disabled,
- a feast is confirmed or skipped,
- a cook assignment is selected when the rules require an explicit allocation.

### 7.6 Resolution preview

`CaravanDayPreview`

Represents the expected result before the user confirms the day pass.

Suggested attributes:

- `caravanId`
- `currentReserve`
- `expectedConsumption`
- `expectedGeneration`
- `expectedNetDelta`
- `expectedReserveAfterResolution`
- `warnings`
- `requiredChoices`
- `contributions`

## 8. UX / Flow

### 8.1 Passing the day

1. The user opens the active caravan workspace.
2. The UI shows the current supply reserve and the daily balance preview.
3. The user clicks **Pass Day**.
4. If any daily choices are required, the UI asks for them in a modal or step.
5. The UI shows a final confirmation with the expected result.
6. The user confirms.
7. The backend resolves the day and persists the result.
8. The UI refreshes the caravan dashboard, statistics, and supply state.

### 8.2 Reviewing the day result

1. The user opens the resolution result.
2. The UI shows:
   - starting reserve,
   - consumption,
   - generation,
   - net change,
   - ending reserve,
   - shortage if any,
   - and the rule sources involved.
3. The UI highlights major rule sources such as agriculturists, batidores, cooks, and relevant feats.

### 8.3 Handling shortage

1. The user sees that the reserve is insufficient.
2. The UI marks the caravan as in shortage for that day.
3. The UI shows the deficit and the sources that caused it.
4. The UI must not hide the shortage behind a generic error state.

### 8.4 Daily choice flow

1. The user opens the day pass preview.
2. The UI lists any optional or required choices.
3. The user selects the desired mode for each choice.
4. The preview updates to show the new expected result.
5. The user confirms the day.

## 9. API Expectations

The implementation should be structured so the backend can support at least the following use cases:

- fetch the current supply state for a caravan,
- preview the result of advancing one day,
- advance the caravan by one day,
- fetch the resolution record for a previously resolved day,
- and list recent day resolutions if the UI needs a history view.

Suggested HTTP endpoints, if needed:

- `GET /api/caravans/{caravanId}/day-cycle/preview`
- `POST /api/caravans/{caravanId}/day-cycle/advance`
- `GET /api/caravans/{caravanId}/day-cycle/{resolutionId}`
- `GET /api/caravans/{caravanId}/day-cycle`

The exact endpoint naming can change, but the use cases must remain available.

## 10. Acceptance Criteria

### Day advancement

- The user can advance the active caravan by exactly one day.
- The action is atomic and cannot be applied twice by accident.
- The caravan state is persisted after the action completes.

### Consumption and generation

- The system subtracts daily consumption automatically.
- The system adds daily production automatically.
- Batidores, agriculturists, cooks, and relevant feats affect the result according to the rules.
- Carro Huerto restrictions are respected.

### Cook and kitchen support

- Cook efficiency is applied when supplies are spent.
- Cocina Portátil changes the cook result when present.
- The resolution breakdown shows what happened.

### Shortage handling

- If the caravan does not have enough supplies, the shortage is visible.
- The result record preserves the deficit.

### Traceability

- The UI shows the source of each major consumption and generation contribution.
- The backend returns the canonical values and the breakdown.

### Time-based effects

- Advancing the day also advances time-based cargo effects such as perishable supplies.

## 11. Edge Cases

1. No caravan is active when the user presses **Pass Day**.
2. The caravan has zero supplies at the start of the day.
3. A batidor is available but the user chooses scouting instead of hunting.
4. A traveler is marked as agriculturist but is not assigned to a valid wagon.
5. A sirviente is assigned to a master whose role no longer qualifies.
6. Multiple cooks are present, but the bonus is limited by the number of full 10-supply blocks available.
7. The caravan has **Autonomía Extrema** and **Consumo Eficiente** at the same time.
8. The caravan uses **Ayuno Intermitente** on a day with low reserves.
9. The user submits the day advance twice because of a network retry.
10. Perishable supplies and their preservation rules change during the same day advance.
11. A feat or role becomes inactive between preview and confirmation.
12. The caravan’s supply reserve is exactly enough for the day and must not be marked as shortage.

## 12. Risks and Open Questions

1. Should the supply reserve be stored as pure provisions, as cargo units, or as both a reserve plus cargo synchronization?
2. Should batidor hunting and other daily choices be defaulted automatically, or must the user always confirm them explicitly?
3. What is the canonical rounding strategy for fractional production modifiers such as 50% bonuses?
4. Should the day advance preview be read-only, or should it support editing the choices before confirmation?
5. Should the feature also resolve non-supply time-based effects such as morale duration bonuses, temporary dotes, and rest timers in the same transaction?

## 13. Implementation Notes

- Keep the day resolution logic in the application layer so the backend remains canonical.
- Model the operation as a dedicated use case rather than embedding it inside statistics or cargo CRUD.
- Emit a structured resolution record so the UI can explain the result.
- Reuse the existing caravan, traveler, wagon, feat, cargo, and statistics read models.
- Add or update architecture boundary tests if the new day-cycle use case introduces new ports or packages.
- Prefer preview + confirm over a blind one-click mutation because this feature has meaningful trade-offs and optional daily choices.

## 14. Related Source Material

- `docs/Reglas_de_Caravana.md` — daily consumption, agriculturists, batidores, cooks, dotes, wagon and cargo rules
- `openspec/specs/caravan-instance/spec.md` — active caravan context
- `openspec/specs/caravan-travelers/spec.md` — traveler roles and assignments
- `openspec/specs/caravan-wagons/spec.md` — wagon ownership and configuration
- `openspec/specs/caravan-wagon-improvements/spec.md` — cook-supporting and other wagon modifiers
- `openspec/specs/caravan-feats/spec.md` — active and inactive feats
- `openspec/specs/caravan-cargo/spec.md` — supply cargo and perishable goods
- `openspec/specs/caravan-statistics/spec.md` — derived consumption and statistics breakdown
