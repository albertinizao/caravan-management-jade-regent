# Caravan Feat Automation

**Status:** Draft  
**Target:** GestionCaravana  
**Type:** Feature specification

## 1. Summary

The application must support precise automation of the caravan feats whose effects can be resolved by the system without a human referee.

This specification defines:

- the feat automation contract,
- the exact automation behavior for each supported feat,
- the triggers, cooldowns, stacking rules, and persistence rules,
- the boundaries between fully automated feats, user-triggered automated feats, and non-automatable feats.

The source of truth for the feat text remains `docs/Reglas_de_Caravana.md`. This specification translates the supported feats into concrete, testable application behavior.

## 2. Problem Statement

Feat rules in the rulebook are written in natural language and combine:

- persistent passive modifiers,
- repeated daily/weekly/monthly effects,
- conditional effects tied to caravan state,
- user-triggered special actions,
- limited-use resources,
- repeatable selections,
- derived bonuses that depend on caravan state.

Without a dedicated automation specification:

- the same feat may be implemented differently in different screens,
- derived bonuses may be calculated in the UI instead of the backend,
- repeatable feats may stack incorrectly,
- cooldowns may be forgotten,
- triggered effects may be applied more than once or not at all,
- the player cannot trust whether a feat is already being applied automatically.

## 3. Goals

1. Automate every feat in the rules document that can be resolved deterministically by the application.
2. Preserve the original rules text as the canonical reference, while exposing machine-readable automation behavior.
3. Apply passive modifiers automatically whenever the caravan state changes.
4. Apply scheduled effects through explicit day-cycle or time-cycle events.
5. Support manual activation for feats that are automated but still require a player decision to trigger.
6. Keep all automation in the backend/domain/application layer.
7. Surface automation state in the UI in a readable way.
8. Make every automation rule testable with deterministic fixtures.

## 4. Non-Goals

This specification does **not** define automation for:

This specification also does **not** define a full combat engine or a full spell-resolution engine.

However, feats whose effects depend on those external systems must still be represented in the application through a manual applicability flag and explicit user-selected context fields. In other words:

- the application may not auto-compute the underlying combat or spell result,
- but it must store whether the feat is considered active,
- and it must apply the feat effect only when the user marks the feat as applicable.

The manual applicability contract is defined in section 5.6.

This specification also does **not** define:

- the full combat engine,
- spell resolution,
- battlefield positioning,
- external world simulation outside the caravan model,
- calendar integration beyond the caravan time cycle already used by the application,
- multiplayer synchronization,
- manual rule overrides outside the existing data model.

## 5. Automation Model

### 5.1 Automation categories

Every supported feat must belong to one of these categories:

- **Passive** — the effect is always applied when the feat is active and its conditions are met.
- **Triggered** — the effect is applied when a specific caravan event occurs.
- **Action-driven** — the effect is applied only when the user explicitly starts an in-app action.
- **Time-gated** — the effect is limited by a daily, weekly, monthly, or multi-month cooldown.
- **Selection-based** — the feat can be selected multiple times and each selection adds its own deterministic effect.

### 5.2 Source of truth

- The backend must calculate automation outcomes.
- The frontend must not duplicate feat rule logic.
- The frontend may request previews, summaries, and availability reasons, but it must not invent effects.

### 5.3 Required caravan events

The application must expose or reuse these deterministic events:

- start of day,
- end of day,
- caravan rest day,
- caravan travel day,
- caravan settlement arrival,
- caravan settlement departure,
- emergency or shortage state change,
- caravan stat update,
- feat selection / feat removal,
- explicit user-triggered feat activation,
- weekly cycle,
- monthly cycle,
- quarterly or multi-month cycle,
- income/production resolution,
- cargo consumption resolution,
- morale or discontent resolution.

### 5.4 Derived-state rules

- Passive feat effects must be recomputed whenever the caravan state changes.
- Conditional effects must activate and deactivate automatically when their conditions become true or false.
- Repeated effects must be counted exactly once per event instance.
- Cooldowns must be persisted in caravan state or in a rule-specific audit record.
- Repeatable feats must expose an unambiguous selection count.

### 5.5 UI contract

The UI must be able to show, for every supported feat:

- whether the effect is currently active,
- the reason why it is active or inactive,
- whether it is passive or triggered,
- whether it has a cooldown,
- whether it requires a user action,
- how many times it can still be used, if limited.

### 5.6 Manual applicability contract

For feats that cannot be fully auto-evaluated by the backend, the owned feat record must expose:

- `manualApplies: boolean`
- `manualAppliesReason: string | null`

Rules:

1. `manualApplies` is set by the user.
2. When `manualApplies` is `false`, the feat contributes no effect.
3. When `manualApplies` is `true`, the feat contributes its rule-defined effect using the current stored parameters.
4. The backend must not infer `manualApplies` from hidden logic.
5. The UI must surface a clear toggle for this flag in the feat detail view.
6. If the feat requires a user-selected context value, that value must be stored separately from the boolean.

## 6. Supported Feats and Exact Automation Requirements

### 6.1 Artesanos Colaborativos

Status: **Not automated**

- The application may display the feat.
- The application must not attempt to auto-grant the granted feat (`Artesanía colaborativa`) inside this spec.
- Any downstream effect from the granted feat is out of scope here.

### 6.2 Autonomía Extrema

Category: Passive / Time-gated

Automation requirements:

1. Batidores and farmers must produce 50% more supplies.
2. The production modifier must be applied automatically to every supply-production resolution involving those jobs.
3. The application must track a 3-month cooldown for the emergency immunity effect.
4. Once every 3 months, the caravan may ignore shortage effects for 3 consecutive days.
5. The 3-day immunity must activate only on explicit emergency state or on a validated user-triggered activation action, depending on the current shortage model.
6. The cooldown must start when the immunity is activated, not when the feat is selected.
7. The effect must not stack with itself; instead, a new activation is blocked until the cooldown expires.

### 6.3 Ayuno Intermitente

Category: Action-driven / Daily

Automation requirements:

1. The player must be able to start an intermittent fasting action from the caravan day-cycle or feat action menu.
2. Starting the action must prompt or validate the Determination check with the formula:
   - `CD = 15 + current discontent + previous fasting days`
3. If the check succeeds:
   - traveler supply consumption for that day is halved,
   - discontent increases by 1.
4. If there are insufficient supplies and the caravan had already run out the previous day:
   - no supply reduction occurs,
   - discontent increases by 3.
5. The “previous fasting days” counter must persist across consecutive days of fasting and reset when the caravan stops fasting for a day.
6. The effect must be resolved once per day at most.

### 6.4 Caravana Afortunada

Category: Passive

Automation requirements:

1. The feat must be linked to the caravan’s diviner/wisdom reroll pool.
2. The weekly reroll allowance granted by the diviner must be doubled automatically.
3. The doubling must not create extra rerolls if the base allowance is zero.
4. The computed allowance must update immediately when the feat is added, removed, or becomes inactive.

### 6.5 Caravana Armada

Category: Passive

Automation requirements:

1. If the caravan’s own base attack bonus is lower than the cleric-equivalent value for the caravan level, the effective bonus must become that cleric-equivalent value.
2. If the caravan’s own bonus is equal or higher, no downgrade may occur.
3. The effect must be derived, not persisted.
4. The effect must be recomputed whenever the caravan level changes.

### 6.6 Caravana Mejorada

Category: Selection-based / Passive

Automation requirements:

1. Each selection must increase two main stats by 1.
2. Each selected stat must respect the global maximum of `+10`.
3. The same feat may be selected multiple times if the repeatable limit allows it.
4. The application must persist which two stats were chosen for each selection.
5. The application must recalculate derived caravan statistics after each selection.

### 6.7 Caravana De Renombre

Category: Triggered / Time-gated

Automation requirements:

1. When the caravan arrives at a settlement, the player may mark the caravan as having spread its fame for the next day.
2. If activated, the next day must automatically grant:
   - `+2` circumstance bonus to social checks in that settlement,
   - `10%` discount on contracted services.
3. The effect must expire after one in-settlement day.
4. The effect must not persist to another settlement unless explicitly activated again.

### 6.8 Caravana Familiar

Category: Passive

Automation requirements:

1. The application must compute the total morale bonus from romantic and family links between travelers.
2. A link between two travelers may contribute at most `+1` morale once.
3. A pair with a child must count as two family links plus one romantic or family link, according to the rule text.
4. If the travelers involved are assigned to a family wagon, their bonus contribution must be doubled.
5. The effect must be recomputed whenever travel assignments or relationship data changes.

### 6.9 Carroñeros

Category: Action-driven / Weekly

Automation requirements:

1. The player must be able to start the scavenging action once per week.
2. The application must resolve a Security check with `CD 15`.
3. On success, the caravan gains `1` cargo unit of repair material.
4. For every full `5` points by which the roll exceeds the CD, the caravan gains one additional unit.
5. The action must be blocked if it has already been used during the current week.

### 6.10 Carros Adicionales

Category: Passive / Selection-based

Automation requirements:

1. The maximum number of wagons allowed must increase by the caravan level.
2. The bonus must stack with repeated selections.
3. The application must recalculate the wagons limit whenever the caravan level changes or the feat selection count changes.
4. The excess-wagon penalty must be computed against the updated maximum automatically.

### 6.11 Carros Protegidos

Category: Passive

Automation requirements:

1. Every wagon owned by the caravan must gain `+2` hardness.
2. Every wagon owned by the caravan must gain `+10` maximum hit points.
3. The bonus must apply to both existing wagons and wagons added later.
4. The effect must be derived from feat state, not stored on individual wagons.

### 6.12 Celebración

Category: Action-driven / Triggered

Automation requirements:

1. The player must be able to start a celebration action from the caravan day-cycle or settlement context.
2. Starting the action must allow a chosen celebration significance value from `1` to `5`, or a rule-derived equivalent.
3. The action must double traveler supply consumption for that celebration day.
4. The action must reduce discontent by the chosen significance amount.
5. The action must add the same amount as a temporary morale bonus lasting `5` days.
6. If the caravan is resting during the celebration, the temporary bonuses must be doubled.
7. The effect must be logged as a single celebration event to prevent double application.

### 6.13 Cuidado De Animales

Category: Action-driven / Daily rest

Automation requirements:

1. On each rest day, the player may start the animal care action once.
2. The action must consume `1` supply unit.
3. The action must resolve a Security special check.
4. On success, the caravan heals `15 × caravan level` hit points.
5. The healing must be distributed across draft animals by the player or by an automatic allocation rule.
6. The action must be blocked on travel days if the rulebook requires resting.

### 6.14 Consumo Eficiente

Category: Passive / Selection-based

Automation requirements:

1. Caravan consumption must decrease by `2`.
2. Consumption cannot fall below the minimum defined by the wagon consumption floor.
3. The reduction may affect all passenger consumption.
4. The effect must stack with repeated selections.
5. The application must expose the resulting daily consumption in the caravan summary.

### 6.15 Defensa Rápida

Category: Triggered / Combat setup

Automation requirements:

1. When a surprise encounter begins, the application must allow up to two movement actions before combat starts.
2. When a combat is foreseeable with at least 30 seconds of warning, the application must allow pre-positioning.
3. The exact movement handling may be represented as a combat-setup state rather than a full tactical engine.
4. The effect must not be applied outside encounter start conditions.

### 6.16 Dominio Del Terreno

Category: Passive / Selection-based

Automation requirements:

1. The player must choose a terrain type for each selection.
2. While traveling in the selected terrain, the caravan receives `+2` Security.
3. Multiple selections on different terrains must be tracked independently.
4. The current terrain bonus must be recomputed whenever the caravan enters a new region.

### 6.17 Dureza De La Caravana

Category: Passive / Selection-based

Automation requirements:

1. Each selection must target one wagon.
2. The selected wagon gains `+20` hit points.
3. If the wagon is destroyed, the feat’s benefit for that wagon is suspended until the wagon is repaired or replaced by a wagon of the same type.
4. Repeated selections must be associated with different wagons when required by the rule text.

### 6.18 Entrenamiento Cruzado

Category: Passive / Triggered validation

Automation requirements:

1. Up to `4` travelers may exercise a second job if they meet the requirements.
2. The application must validate the secondary job eligibility automatically.
3. The effect must be reflected in role assignment screens and derived productivity.
4. Each eligible traveler must be counted once.

### 6.19 Esclavistas

Category: Passive

Automation requirements:

1. The caravan must not gain discontent from wounds or death of slaves.
2. Determination checks to increase discontent due to the presence of slaves must be ignored.
3. The immunity must apply automatically while the feat is active.
4. The UI must show that the discontent source is suppressed by the feat.

### 6.20 Levantarse De La Nada

Category: Triggered / Defensive

Automation requirements:

1. The first time a wagon would be destroyed, the application must stop destruction.
2. The wagon must be left at `0` hit points instead.
3. The wagon must receive the `levantado de la nada` state marker.
4. The wagon must become repairable up to `50%` of its maximum hit points.
5. The effect may only trigger once per destruction sequence.

### 6.21 Mercado Ambulante

Category: Passive / Selection-based

Automation requirements:

1. The caravan receives the commercial Determination special-check bonus defined by the feat.
2. Each selection must stack according to the repeatable limit.
3. The application must expose the current stack count in the caravan summary.

### 6.22 Maestría Mercantil

Category: Passive / Selection-based

Automation requirements:

1. The caravan’s Determination-based trade bonus must equal one quarter of caravan level.
2. The effect must stack up to the repeatable limit.
3. If `Mercado Ambulante` is required to have more stacks, the application must enforce that dependency.

### 6.23 Oferta Gancho

Category: Action-driven / Daily / Selection-based

Automation requirements:

1. Once per day, the player may activate the sales action in a settlement.
2. If at least one unit of goods was sold by a merchant, the player may sell one treasure item at a premium.
3. The premium must be `+10%` on the first selection and `+20%` when the feat has been selected twice.
4. If the action requires selling two units of goods for the `+20%` variant, the application must enforce that condition.
5. The action must not be reusable more than once per day.

### 6.24 Organización Impecable

Category: Passive / Triggered by state

Automation requirements:

1. Caravan cargo capacity must increase by `10%`.
2. If the caravan has had no discontent for `10` consecutive days, wagon consumption must decrease by `1` per wagon.
3. The discontent-free day counter must persist day to day.
4. The effect may stack up to two selections.

### 6.25 Planificación De Emergencia

Category: Passive / Weekly reroll

Automation requirements:

1. While the caravan is in a prolonged crisis, the application must allow one reroll of a failed Security or Determination check per week.
2. The crisis state must be represented in caravan state.
3. The reroll allowance must reset at the start of the next week.
4. The effect must not be available outside crisis state.

### 6.26 Reparaciones Eficientes

Category: Action-driven / Selection-based

Automation requirements:

1. When the player performs a special Security repair check, the application must apply `+2` to the roll.
2. The effect must stack up to three selections.
3. The bonus must apply only to repair checks, not to unrelated Security checks.

### 6.27 Rituales De Fortuna

Category: Passive / Triggered

Automation requirements:

1. When the caravan avoids a failure by using `Caravana Afortunada`, it gains `1` Moral.
2. The effect must be applied exactly once per avoided failure.
3. The application must not award the bonus if the reroll does not change the outcome.

### 6.28 Rutas Conocidas

Category: Passive / Triggered

Automation requirements:

1. While traveling in previously explored regions, the caravan receives `+2` Security.
2. If the caravan would get an unwanted encounter, the application must offer or resolve a reroll.
3. The effect must be derived from the route/exploration state.

### 6.29 Tripulación Valerosa

Category: Passive / Selection-based

Automation requirements:

1. The caravan gains `+2` Determination against fear and fleeing effects.
2. The effect must stack up to three selections.
3. The bonus must be visible in the Determination summary and in checks against fear sources.

### 6.30 Trabajo En Equipo

Category: Passive / Derived aggregation

Automation requirements:

1. When multiple travelers share the same job, productivity must increase by `25%` for each additional traveler up to `2` extra travelers.
2. The bonus must be computed automatically from role assignments.
3. The effect must not count travelers outside the shared cargo.
4. The UI must show the resulting effective productivity per job.

### 6.31 Venta De Esclavos

Category: Action-driven / Settlement / Daily limit

Automation requirements:

1. For each traveler with the `esclavista` job, up to `5` sale/purchase actions may be executed per day in a settlement.
2. Buying a slave must cost `75%` of its price.
3. Selling a slave not bought in the current settlement must pay `200%` of its price.
4. The application must enforce the settlement-bound and daily limits.
5. The action history must track counts per day and per settlement.

### 6.32 Veloz

Category: Passive / Selection-based

Automation requirements:

1. Caravan travel speed must increase by `4` miles per day per selection.
2. The effect must stack up to the repeatable limit.
3. The updated speed must be reflected in travel previews.

### 6.33 Viajeros Expertos

Category: Passive / Selection-based

Automation requirements:

1. The maximum bonuses that traveler jobs can provide must increase by `1` per selection.
2. The effect must stack up to the repeatable limit.
3. The application must use the adjusted cap in all job-derived calculations.

## 7. Detailed Shared Rules

### 7.1 Repeatable feat handling

- Repeatable feats must store one owned record per selection.
- The selection order must be preserved.
- The UI must show the number of selections when relevant.
- A repeatable feat with a limit of `N` must reject selection `N + 1`.

### 7.2 Cooldowns and counters

- Every daily, weekly, monthly, and quarterly rule must persist its last use date or cycle marker.
- The cycle marker must be calculated from the caravan’s internal day progression, not from wall-clock time.
- Time-gated effects must be blocked when the cycle has not elapsed.

### 7.3 Derived stat recalculation

- Any feat that modifies caravan stats, wagon stats, job outputs, consumption, morale, discontent, or reroll pools must be recalculated when:
  - the feat is added,
  - the feat is removed,
  - the feat becomes inactive,
  - the caravan level changes,
  - the caravan enters or leaves a relevant region,
  - a travel assignment changes,
  - a daily cycle advances,
  - a settlement or emergency state changes.

### 7.4 UI surface requirements

The UI must show automation state in the feat detail view:

- automation category,
- current effect summary,
- trigger conditions,
- cooldown or remaining uses,
- reason for being inactive, if applicable.

## 8. Acceptance Criteria

1. Every supported feat listed in section 6 is represented in machine-readable automation logic.
2. Passive effects update automatically when caravan state changes.
3. Triggered effects only resolve when their trigger condition occurs.
4. Cooldowns prevent duplicate resolution.
5. Repeatable feats stack exactly according to the rule text.
6. The UI can display the active/inactive reason for each automated feat.
7. The backend remains the source of truth for all calculations.
8. The existing feat catalog remains compatible with the automation layer.

## 9. Implementation Notes

- The implementation should extend the existing feat catalog and feat management use cases rather than creating a separate hidden rules engine.
- Automation should be modeled as rule metadata attached to feat types.
- For effects that change caravan stats, the automation layer should feed the existing statistics calculation pipeline.
- For effects that require per-day or per-week usage tracking, the caravan aggregate should store the last resolved cycle.
- For effects with multiple selections, the owned-feat list should remain the audit trail.

## 10. Feat-by-Feat Implementation Contract

The following matrix is the authoritative implementation contract for every feat in `docs/Reglas_de_Caravana.md`.

| Feat | Mode | State / Inputs | Exact automation |
|---|---|---|---|
| Artesanos Colaborativos | manual | `manualApplies:boolean`, `manualAppliesReason:string\|null` | If true, expose the caravan rule marker `Artesanía colaborativa` to all travelers while the feat remains active. The application must store only the boolean assertion and the reason; it must not attempt to infer whether the crafting workflow can actually benefit from the marker. |
| Autonomía Extrema | passive and time-gated | `cooldownEndsAt`, `immunityEndsAt`, `immunityUsesLog[]` | Batidores and farmers produce `50%` more supplies. Recompute the supply-production modifier whenever job productivity is resolved. The emergency immunity may be activated only when the caravan is in shortage or the user explicitly opens the feat action from a validated emergency context. On activation, set `immunityEndsAt = currentDay + 3 days` and `cooldownEndsAt = currentDay + 3 months`. While the immunity window is open, shortage penalties are suppressed but the production bonus remains independent. The effect may not overlap with a second activation; activation must be blocked until the cooldown expires. |
| Ayuno Intermitente | action-driven daily | `fastingStreak:int`, `lastFastingDay`, `currentFastingActionId`, `lastFastingResolvedDay` | The feat is resolved once per caravan day. When the user starts fasting for the day, compute `CD = 15 + currentDiscontent + fastingStreak`. On success, halve traveler supply consumption for that day and increase discontent by `1`. If supplies are insufficient and the previous day already had no supplies, increase discontent by `3` and do not apply the halving. The streak increases only on consecutive fasting days and resets on any non-fasting day. |
| Bendición Del Camino | manual and action-driven | `manualApplies:boolean`, `selectedDeityCode`, `selectedSpellName`, `spellLevel:int`, `lastBlessingDay` | When active, store the chosen level-1 spell and deity. The application does not resolve the spell effect itself; it records the selected spell, validates that the spell is level 1, applies the feat marker, and exposes the effect text `once per day, after prayer, all travelers receive the selected level-1 spell with caster level 25`. The once-per-day limit must be tracked by caravan day, not by real time. |
| Caravana Afortunada | passive | `weeklyDivinerRerolls:int`, `baseWeeklyDivinerRerolls:int` | Double the diviner reroll allowance automatically from the base reroll pool. The derived value is `baseWeeklyDivinerRerolls * 2`. Recalculate on feat add/remove or when the caravan level changes if the base allowance is level-dependent. If the base pool is zero, the result remains zero and no synthetic rerolls are created. |
| Caravana Armada | passive | `baseAttackBonus`, `clericEquivalentAttackBonus`, `effectiveAttackBonus` | Set effective caravan attack bonus to the cleric-equivalent value for caravan level if and only if the caravan’s own bonus is lower. Never reduce a higher native bonus. Recompute whenever caravan level or underlying attack bonus changes. The derived value must feed every downstream combat or stat summary that reads caravan attack bonus. |
| Caravana Mejorada | selection-based passive | `selectedMainStats[]`, one record per selection | Each selection chooses exactly two main stats. Increment each chosen stat by `1`, cap each at `+10`, persist the chosen pair on the owned feat instance, and recalculate caravan stats after every selection. If the same stat is chosen twice in one selection, it must still count as two increments and each increment remains capped individually. |
| Caravana Bendecida | manual | `manualApplies:boolean`, `selectedDeityCode`, `lastBlessingDay` | Record the chosen deity. When active, expose the effect: each day, the clerics of that deity can cast one level 1 or 2 spell without consuming resources. The application records the effect and the deity; spell resolution itself remains outside scope. The manual toggle must be the only gate that decides whether the daily free-cast marker is applied. |
| Caravana Santificada | manual | `manualApplies:boolean`, `selectedDeityCode`, `selectedAltars[]` | Record the chosen deity. When active, apply `+3` effective caster level to divine spells cast by that deity’s clerics within `200` feet of the caravan. The app does not resolve the spell, only the modifier state. The feat may be selected multiple times for different deities, and each selection must preserve its own deity binding and manual toggle. |
| Caravana De Renombre | triggered and timed | `fameAnnouncementPending:boolean`, `fameExpiresAt`, `currentSettlementId` | When the caravan arrives at a settlement, the user may mark fame as announced for the next day. If active, grant `+2` circumstance bonus to social checks and `10%` discount on contracted services for the next in-settlement day only. The effect must expire when the settlement day advances or the caravan leaves the settlement, whichever happens first. |
| Caravana Familiar | manual plus passive calculation | `manualApplies:boolean`, `manualAppliesReason:string\|null` | The user marks whether the relationship conditions currently hold. When `manualApplies=true`, compute morale as `+1` per qualifying family or romance link, with one link between the same pair counting at most once. If the involved travelers are assigned to a family wagon, double the contribution of that link. The backend must never infer the family state from the relationship graph; it only applies the bonus after the user asserts the condition. |
| Carroñeros | weekly action | `weeklyScavengeUsedAt`, `currentWeekKey`, `lastScavengeCheckResult` | Once per week, allow a Security check at `CD 15`. On success, add `1` repair-material cargo unit and add `1` more for each full `5` points above the CD. The action must be blocked until `currentWeekKey` changes. The result record must persist the resolved total so the UI can show success, margin, and reward. |
| Carros Adicionales | selection-based passive | `selectionCount:int` | Increase the wagon limit by the caravan level per selection. The new maximum must be reflected in wagon-capacity validation and in the caravan summary. Excess wagons are not destroyed; they are only flagged by the existing over-capacity rules. |
| Carros En Círculo | manual plus combat-setup | `manualApplies:boolean`, `manualAppliesReason:string\|null` | The user toggles the formation as applicable during combat setup. When active, apply `+4` AC, total cover for covered travelers, difficult terrain between wagons, and a max of one medium creature between two wagons. This spec only stores the user-asserted applicability and the effect marker; tactical positioning remains external. |
| Carros Protegidos | passive | none beyond wagon stats | Add `+2` hardness and `+10` max hit points to every owned wagon. Apply the bonus to future wagons as soon as they enter the caravan. The modifier must be derived from feat ownership and selection count, not duplicated onto wagon records. |
| Celebración | action-driven day event | `celebrationIntensity:1..5`, `celebrationEndsAt`, `celebrationAppliedDay` | The user selects celebration intensity from `1` to `5`. Double traveler supply consumption for the celebration day, reduce discontent by the selected intensity, and add a morale bonus of the same value for `5` days. If resting, double the resulting bonuses. The application must persist the celebration day key so a reload cannot apply the same celebration twice. |
| Cuidado De Animales | rest-day action | `restCareUsedAt`, `chosenHealingTargets[]`, `lastAnimalCareCheckResult` | On a rest day, allow one Security special check that costs `1` supply unit. On success, heal `15 × caravanLevel` hit points and distribute the healing among draft animals according to the chosen targets. The action is invalid on travel days; it must be blocked before rolling if the caravan is not resting. |
| Consumo Eficiente | passive and selection-based | `selectionCount:int`, `consumptionReduction:int` | Reduce caravan consumption by `2` while preserving the floor defined by wagon consumption. Each selection stacks. Recalculate daily consumption after every day-cycle change and after feat add/remove. The derived reduction must be exposed in the caravan summary as a final adjusted consumption value, not as a hidden modifier. |
| Defensa Rápida | combat trigger | `surpriseEncounterGranted:boolean`, `prePositionWindow:boolean`, `combatSetupLockId` | When a surprise encounter begins, allow up to two movement actions before combat. If an encounter is foreseeable with at least `30` seconds of warning, expose a pre-positioning state. No other combat automation is required here. The implementation must record a setup window so the extra movement can only be consumed once per encounter start. |
| Dominio Del Terreno | selection-based passive | `selectedTerrainCodes[]` | Each selection chooses one terrain type. While traveling through that terrain, add `+2` Security. Multiple selections on different terrains stack independently. The terrain bonus must be recomputed from the current route/region and must disappear immediately when the caravan leaves the selected terrain. |
| Dureza De La Caravana | selection-based passive | `selectedWagonIds[]` | Each selection targets one wagon. That wagon gains `+20` hit points. If the wagon is destroyed, suspend the benefit for that wagon until it is repaired or replaced by a wagon of the same type. The hit point bonus must be attached to the wagon selection record, not hard-coded to a wagon type. |
| Entrenamiento Cruzado | passive validation | `eligibleSecondJobTravelerIds[]`, `validatedSecondJobMappings[]` | Allow up to `4` travelers to hold a second job if they satisfy the job requirements. Validate eligibility automatically and include the second job in productivity calculations. A traveler counted for the cap must be one traveler-id, even if that traveler can satisfy multiple candidate second jobs. |
| Esclavistas | passive | `suppressedDiscontentSources[]` | Prevent discontent increases caused by slave wounds or slave death. Ignore Determination checks that would raise discontent because slaves exist. The suppression must apply automatically while the feat is active and the UI must show that the discontent source is suppressed by the feat. |
| Levantarse De La Nada | defensive trigger | `savedFromDestructionWagonId`, `used:boolean`, `savedWagonStateSnapshot` | The first time a wagon would be destroyed, set its hit points to `0` instead, mark it as `raised-from-nothing`, and allow repair up to `50%` of max hit points. Trigger only once per destruction sequence. The saved state marker must be cleared only when the wagon leaves the destroyed state by repair or replacement. |
| Mercado Ambulante | selection-based passive | `selectionCount:int`, `commercialCheckBonus:int` | Apply the commercial Determination bonus defined by the feat. Stack per selection up to the repeatable limit. The application must expose the current stack count and resulting bonus in the caravan summary. |
| Maestría Mercantil | selection-based passive | `selectionCount:int`, `requiredMercadoAmbulanteStacks:int` | Compute the trade bonus as one quarter of the caravan level. Enforce any dependency on `Mercado Ambulante` stacks if the catalog states it. Stack per selection up to the limit. If a prerequisite stack threshold exists, selection must be rejected before save when the threshold is not met. |
| Oferta Gancho | daily settlement action | `dailyOfferUsedAt`, `selectedTreasureItemId`, `premiumTier`, `merchantSoldGoodsToday:int` | Once per day in a settlement, allow selling one treasure item after a merchant sold at least one unit of merchandise. Apply `+10%` for the first tier and `+20%` for the second tier. The second tier additionally requires two units of merchandise sold by the merchant. The daily usage marker must be settlement-scoped, not global, so the action can be used again in a different settlement on the same campaign day only if the rules permit it. |
| Organización Impecable | passive and state-gated | `discontentFreeDays:int`, `selectionCount:int`, `cargoCapacityBonusPercent:int` | Increase cargo capacity by `10%`. When `discontentFreeDays >= 10`, reduce wagon consumption by `1` per wagon. Stack up to two selections. The day counter resets whenever discontent becomes nonzero and must be incremented during the end-of-day resolution after discontent is finalized. |
| Planificación De Emergencia | passive / weekly reroll | `weeklyEmergencyRerollUsedAt`, `crisisState:boolean`, `weeklyRerollWindowKey` | While the caravan is in a prolonged crisis, allow one reroll of a failed Security or Determination check per week. Reset the allowance at the next weekly cycle. The effect must not be available outside crisis state. The crisis flag is a precondition supplied by the campaign state; the feat only adds the reroll entitlement. |
| Reparaciones Eficientes | action-driven passive bonus | `selectionCount:int`, `repairBonusPerCheck:int` | Apply `+2` to each special Security repair check. Stack up to three selections. Only repair checks receive the bonus. The application must not affect non-repair Security checks, even if they use the same skill value. |
| Rituales De Fortuna | triggered | `lastGrantedFromLuckyCaravanEventId`, `moralGrantedByRerollEventIds[]` | When a failure is avoided by `Caravana Afortunada`, grant `+1` Moral exactly once per avoided failure event. The bonus must only be granted when the reroll changes the outcome from failure to success. If the reroll does not change the result, no moral is granted. |
| Rutas Conocidas | passive and trigger | `exploredRegionBonusActive:boolean`, `lastRouteRerollEncounterId`, `knownRegionCodes[]` | While traveling through previously explored regions, grant `+2` Security. If an unwanted encounter would occur, allow or resolve one reroll. The effect must be derived from the route/exploration state and the reroll must be consumed exactly once per encounter opportunity. |
| Tripulación Valerosa | selection-based passive | `selectionCount:int` | Grant `+2` Determination against fear and flee effects per selection, up to the repeatable limit. The bonus must be visible in the Determination summary and in checks against fear sources. |
| Trabajo En Equipo | passive derived aggregation | `sharedJobAssignments[]`, `sharedJobBonusByJobCode` | For each job with multiple travelers, increase productivity by `25%` per additional traveler up to two additional travelers. Recompute from current role assignments only. The bonus must be derived from the current assignment graph and must not count travelers outside the shared cargo. |
| Venta De Esclavos | settlement daily action | `actionsUsedToday:int`, `settlementId`, `slaveMarketActionLog[]`, `actionsUsedByTravelerId[]` | In a settlement, each traveler with the `esclavista` job may contribute up to `5` buy/sell actions per day. Buying costs `75%` of price; selling in the same settlement pays `200%` of price. Track action count per day and settlement. The settlement scope must reset when the caravan changes settlement, and the per-day cap must be enforced before the transaction is committed. |
| Veloz | selection-based passive | `selectionCount:int`, `travelSpeedBonus:int` | Increase travel speed by `4` miles/day per selection, stacked up to the repeatable limit. The updated speed must be reflected in travel previews and route ETA calculations. |
| Viajeros Expertos | selection-based passive | `selectionCount:int`, `maxTravelerJobBonusIncrease:int` | Increase the maximum bonus that traveler jobs can provide by `+1` per selection, stacked up to the repeatable limit. The adjusted cap must be used in all job-derived calculations, including preview and final resolution. |
## 11. Conditional Feat Boolean Contract

The following feats are implemented through the `manualApplies:boolean` contract because their full rules depend on conditions or sub-systems that are not auto-evaluated in this application:

- `Artesanos Colaborativos`
- `Bendición Del Camino`
- `Caravana Bendecida`
- `Caravana Santificada`
- `Caravana Familiar`
- `Carros En Círculo`

Rules:

1. The user may toggle `manualApplies` from the feat detail screen.
2. If the checkbox is unchecked, the feat contributes no effect.
3. If the checkbox is checked, the backend applies the stored effect text or derived numeric modifier.
4. The application must keep the current manual applicability reason visible.
5. The backend must preserve the boolean across save/load cycles.
6. The UI must clearly indicate that the effect is user-asserted, not auto-inferred.
