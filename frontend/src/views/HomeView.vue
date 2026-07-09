<script setup lang="ts">
import { computed, onMounted, ref } from "vue";

import {
  advanceCaravanDayCycle,
  createCaravan,
  deleteCaravan,
  exportCaravanBackup,
  getActiveCaravan,
  getCaravanStatistics,
  listCaravans,
  importCaravanBackup,
  previewCaravanDayCycle,
  selectActiveCaravan,
  updateCaravanDiscontent,
  updateCaravanLevel,
  updateCaravanMainStats,
} from "@/services/caravans";
import { useToast } from "@/composables/useToast";
import { listCaravanTravelers } from "@/services/travelers";
import type {
  Caravan,
  CaravanDayCyclePreview,
  CaravanDayCycleResult,
  CaravanMainStats,
  CaravanStatistics,
} from "@/types/caravan";
import type { CaravanTraveler } from "@/types/traveler";

const caravans = ref<Caravan[]>([]);
const activeCaravan = ref<Caravan | null>(null);
const loading = ref(true);
const submitting = ref(false);
const pendingAction = ref<string | null>(null);
const error = ref<string | null>(null);
const name = ref("");
const description = ref("");
const offense = ref(1);
const defense = ref(1);
const mobility = ref(1);
const morale = ref(1);
const createModalOpen = ref(false);
const caravanStatistics = ref<CaravanStatistics | null>(null);
const dayCycleModalOpen = ref(false);
const dayCycleSubmitting = ref(false);
const dayCycleFasting = ref(false);
const dayCycleIdempotencyKey = ref("");
const dayCycleTravelers = ref<CaravanTraveler[]>([]);
const dayCyclePreview = ref<CaravanDayCyclePreview | null>(null);
const dayCycleError = ref<string | null>(null);
const dayCycleChoices = ref<Record<string, "HUNT" | "EXPLORE">>({});
const editableMainStats = ref<CaravanMainStats | null>(null);
const mainStatsSubmitting = ref(false);
const backupFileInput = ref<HTMLInputElement | null>(null);
const { showToast } = useToast();

const hiddenContributionStats = new Set([
  "offense",
  "defense",
  "security",
  "determination",
  "travelerCapacity",
  "cargoCapacity",
  "cargoLoad",
  "consumption",
  "beastCount",
  "wagonCount",
  "travelerCount",
]);

const selectedCaravan = computed(() => activeCaravan.value ?? caravans.value.find((caravan) => caravan.active) ?? null);
const canEditMainStats = computed(() => (selectedCaravan.value?.mainStats.unassignedPoints ?? 0) > 0);
const displayedMainStats = computed(() => editableMainStats.value ?? selectedCaravan.value?.mainStats ?? null);
const mainStatsRemainingPoints = computed(() => {
  if (!selectedCaravan.value || !editableMainStats.value || !canEditMainStats.value) {
    return 0;
  }

  const initialBudget =
    selectedCaravan.value.mainStats.offense +
    selectedCaravan.value.mainStats.defense +
    selectedCaravan.value.mainStats.mobility +
    selectedCaravan.value.mainStats.morale +
    selectedCaravan.value.mainStats.unassignedPoints;
  const currentAllocation =
    editableMainStats.value.offense +
    editableMainStats.value.defense +
    editableMainStats.value.mobility +
    editableMainStats.value.morale;

  return Math.max(0, initialBudget - currentAllocation);
});
const hasMainStatsChanges = computed(() => {
  if (!selectedCaravan.value || !editableMainStats.value || !canEditMainStats.value) {
    return false;
  }

  const saved = selectedCaravan.value.mainStats;
  const draft = editableMainStats.value;
  return (
    saved.offense !== draft.offense ||
    saved.defense !== draft.defense ||
    saved.mobility !== draft.mobility ||
    saved.morale !== draft.morale
  );
});
const canUseIntermittentFasting = computed(
  () => selectedCaravan.value?.feats.some((feat) => feat === "Ayuno Intermitente") ?? false,
);
const allocatedPoints = computed(() => offense.value + defense.value + mobility.value + morale.value - 4);
const remainingPoints = computed(() => 3 - allocatedPoints.value);
const automaticDerivedStats = computed(() => ({
  attack: offense.value,
  armorClass: 10 + defense.value,
  security: mobility.value,
  determination: morale.value,
}));
const visibleContributions = computed(() =>
  caravanStatistics.value?.contributions.filter((item) => !hiddenContributionStats.has(item.statCode)) ?? [],
);

interface GroupedDayCycleContribution {
  groupLabel: string;
  contributions: CaravanDayCyclePreview["contributions"];
}

const roleGroupLabels = new Map([
  ["Agricultor", "Agricultores"],
  ["Batidor", "Batidores"],
  ["Cocinero", "Cocineros"],
  ["Sirviente", "Sirvientes"],
]);

function contributionGroupLabel(contribution: CaravanDayCyclePreview["contributions"][number]) {
  if (contribution.sourceType === "CARGO") {
    return "Suministros";
  }

  const sourceRoleLabel = contribution.sourceRoleName?.trim();
  if (sourceRoleLabel) {
    return roleGroupLabels.get(sourceRoleLabel) ?? sourceRoleLabel;
  }

  if (contribution.sourceType === "FEAT") {
    return "Dotes";
  }

  return "Otros";
}

function contributionQuantityText(contribution: CaravanDayCyclePreview["contributions"][number]) {
  if (!contribution.quantityUnit) {
    return `${contribution.quantity}`;
  }

  return `${contribution.quantity} ${contribution.quantityUnit}`;
}

function isTeamworkContribution(contribution: CaravanDayCyclePreview["contributions"][number]) {
  return contribution.sourceName === "Trabajo En Equipo";
}

const groupedDayCycleContributions = computed<GroupedDayCycleContribution[]>(() => {
  const contributions = dayCyclePreview.value?.contributions ?? [];
  const roleOrder = new Map([
    ["Agricultores", 0],
    ["Batidores", 1],
    ["Cocineros", 2],
    ["Sirvientes", 3],
    ["Suministros", 4],
    ["Dotes", 5],
    ["Otros", 6],
  ]);
  const groups = new Map<string, CaravanDayCyclePreview["contributions"]>();

  for (const contribution of contributions) {
    const groupLabel = contributionGroupLabel(contribution);
    const group = groups.get(groupLabel) ?? [];
    group.push(contribution);
    groups.set(groupLabel, group);
  }

  return [...groups.entries()]
    .sort(([leftLabel], [rightLabel]) => {
      const leftOrder = roleOrder.get(leftLabel) ?? Number.MAX_SAFE_INTEGER;
      const rightOrder = roleOrder.get(rightLabel) ?? Number.MAX_SAFE_INTEGER;
      if (leftOrder !== rightOrder) {
        return leftOrder - rightOrder;
      }

      return leftLabel.localeCompare(rightLabel, "es", { sensitivity: "base" });
    })
    .map(([groupLabel, groupedContributions]) => ({
      groupLabel,
      contributions: [...groupedContributions].sort((left, right) =>
        Number(isTeamworkContribution(left)) - Number(isTeamworkContribution(right)) ||
        left.sourceName.localeCompare(right.sourceName, "es", { sensitivity: "base" }) ||
        left.sourceType.localeCompare(right.sourceType, "es", { sensitivity: "base" }) ||
        left.quantity - right.quantity,
      ),
    }));
});

const dayCycleFoodGroups = computed(() =>
  groupedDayCycleContributions.value.filter((group) =>
    ["Batidores", "Cocineros"].includes(group.groupLabel),
  ),
);

const dayCycleOtherGroups = computed(() =>
  groupedDayCycleContributions.value.filter(
    (group) => !["Batidores", "Cocineros", "Suministros"].includes(group.groupLabel),
  ),
);

const dayCycleCookFoodTotal = computed(() =>
  dayCyclePreview.value?.contributions
    .filter(
      (contribution) =>
        contribution.effectCode === "generation" &&
        contribution.sourceRoleName === "Cocinero" &&
        contribution.applied,
    )
    .reduce((sum, contribution) => sum + contribution.quantity, 0) ?? 0,
);

const dayCycleInitialOpenFood = computed(() =>
  dayCyclePreview.value?.initialProvisionsInConsumption.reduce((sum, consumption) => sum + consumption.remainingFood, 0) ?? 0,
);

const dayCycleCookSupplyLoads = computed(() =>
  dayCyclePreview.value?.contributions
    .filter(
      (contribution) =>
        contribution.effectCode === "generation" &&
        contribution.sourceRoleName === "Cocinero" &&
        contribution.sourceName !== "Trabajo En Equipo" &&
        contribution.applied,
    )
    .length ?? 0,
);

const dayCycleLateSupplyFoodNeeded = computed(() => {
  if (!dayCyclePreview.value) {
    return 0;
  }

  return Math.max(
    0,
    dayCyclePreview.value.expectedConsumption -
      dayCyclePreview.value.generatedFood -
      dayCycleCookFoodTotal.value -
      dayCycleInitialOpenFood.value,
  );
});

const dayCycleLateSupplyLoads = computed(() => Math.ceil(dayCycleLateSupplyFoodNeeded.value / 10));

const dayCyclePartialSupplyLoads = computed(() => dayCyclePreview.value?.provisionsInConsumption.length ?? 0);

const dayCyclePartialSupply = computed(() =>
  dayCyclePreview.value?.provisionsInConsumption.find(
    (consumption) => consumption.remainingFood > 0 && consumption.remainingFood < 10,
  ) ?? null,
);

const dayCycleCompleteSurplusLoads = computed(() => {
  if (!dayCyclePreview.value) {
    return 0;
  }

  return Math.max(
    0,
    dayCyclePreview.value.generatedProvisions -
      dayCycleCookSupplyLoads.value -
      dayCycleLateSupplyLoads.value -
      dayCyclePartialSupplyLoads.value,
  );
});

const dayCycleLateSupplyUnitsUsed = computed(() =>
  Math.max(
    0,
    (dayCyclePreview.value?.consumedProvisions ?? 0) -
      dayCycleCookSupplyLoads.value -
      dayCyclePartialSupplyLoads.value,
  ),
);

function percentageOf(value: number, max: number) {
  if (max <= 0) {
    return 0;
  }

  return Math.max(0, Math.min(100, (value / max) * 100));
}

function progressBackground(value: number, max: number, fillColor: string, emptyColor = "#e5e7eb") {
  const percentage = percentageOf(value, max);

  return {
    background: `linear-gradient(90deg, ${fillColor} 0%, ${fillColor} ${percentage}%, ${emptyColor} ${percentage}%, ${emptyColor} 100%)`,
  };
}

function progressMarkerStyle(value: number, max: number) {
  const percentage = percentageOf(value, max);

  return {
    left: `calc(${percentage}% - 0.125rem)`,
  };
}

function isPending(action: string) {
  return pendingAction.value === action;
}

function openBackupImportDialog() {
  backupFileInput.value?.click();
}

function buildBackupFilename(caravanName: string) {
  const safeName = caravanName
    .trim()
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "") || "caravana";

  return `${safeName}-backup-${new Date().toISOString().slice(0, 10)}.json`;
}

function downloadBackupFile(fileName: string, payload: unknown) {
  const blob = new Blob([JSON.stringify(payload, null, 2)], { type: "application/json;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = fileName;
  anchor.rel = "noreferrer";
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  window.setTimeout(() => URL.revokeObjectURL(url), 1000);
}

function openCreateModal() {
  name.value = "";
  description.value = "";
  offense.value = 1;
  defense.value = 1;
  mobility.value = 1;
  morale.value = 1;
  createModalOpen.value = true;
}

function closeCreateModal() {
  createModalOpen.value = false;
}

function syncEditableMainStats(caravan: Caravan | null) {
  editableMainStats.value = caravan && caravan.mainStats.unassignedPoints > 0 ? { ...caravan.mainStats } : null;
}

function openDayCycleModal() {
  if (!selectedCaravan.value) {
    return;
  }

  dayCycleModalOpen.value = true;
  dayCycleFasting.value = false;
  dayCycleError.value = null;
  dayCyclePreview.value = null;
  dayCycleIdempotencyKey.value = crypto.randomUUID();
  void refreshDayCyclePreview();
}

function closeDayCycleModal() {
  dayCycleModalOpen.value = false;
  dayCyclePreview.value = null;
  dayCycleTravelers.value = [];
  dayCycleChoices.value = {};
  dayCycleError.value = null;
}

function buildDayCyclePayload() {
  return {
    fastingEnabled: dayCycleFasting.value,
    choices: Object.entries(dayCycleChoices.value).map(([travelerId, mode]) => ({ travelerId, mode })),
  };
}

const dayCycleShortage = computed(() => {
  const preview = dayCyclePreview.value;
  if (!preview) {
    return 0;
  }

  return Math.max(preview.expectedShortage ?? 0, 0);
});

const hasDayCycleShortage = computed(() => dayCycleShortage.value > 0);

async function refreshDayCyclePreview() {
  if (!selectedCaravan.value) {
    return;
  }

  try {
    dayCycleError.value = null;
    const [travelers, preview] = await Promise.all([
      listCaravanTravelers(selectedCaravan.value.id),
      previewCaravanDayCycle(selectedCaravan.value.id, buildDayCyclePayload()),
    ]);
    dayCycleTravelers.value = travelers;
    dayCyclePreview.value = preview;
    for (const traveler of travelers) {
      if (traveler.activeRoleCode === "batidor" && !dayCycleChoices.value[traveler.id]) {
        dayCycleChoices.value[traveler.id] = "HUNT";
      }
    }
  } catch (cause) {
    dayCycleError.value = cause instanceof Error ? cause.message : "No se pudo calcular el día";
  }
}

async function handleAdvanceDayCycle() {
  if (!selectedCaravan.value) {
    return;
  }

  dayCycleSubmitting.value = true;
  dayCycleError.value = null;

  try {
    const result: CaravanDayCycleResult = await advanceCaravanDayCycle(selectedCaravan.value.id, {
      idempotencyKey: dayCycleIdempotencyKey.value,
      ...buildDayCyclePayload(),
    });

    const resolvedShortage = Math.max(result.expectedShortage ?? 0, 0);
    const provisionDelta = result.surplusProvisions >= 0 ? `+ ${result.surplusProvisions}` : `- ${Math.abs(result.surplusProvisions)}`;

    if (resolvedShortage > 0) {
      showToast(
        `No se ha podido cubrir todo el consumo de la caravana. Faltan ${resolvedShortage} de comida. Provisiones consumidas: ${result.consumedProvisions}. Provisiones sobrantes: ${provisionDelta}.`,
        "error",
      );
    } else {
      showToast(
        `Día avanzado: ${result.expectedNetDelta >= 0 ? "+" : ""}${result.expectedNetDelta} de comida neta. Provisiones consumidas: ${result.consumedProvisions}. Provisiones sobrantes: ${provisionDelta}.`,
      );
    }

    closeDayCycleModal();
    await refresh();
  } catch (cause) {
    dayCycleError.value = cause instanceof Error ? cause.message : "No se pudo avanzar el día";
  } finally {
    dayCycleSubmitting.value = false;
  }
}

function adjustEditableMainStat(
  stat: keyof Pick<CaravanMainStats, "offense" | "defense" | "mobility" | "morale">,
  delta: number,
) {
  if (!editableMainStats.value || !canEditMainStats.value) {
    return;
  }

  if (delta > 0 && mainStatsRemainingPoints.value <= 0) {
    return;
  }

  const nextValue = editableMainStats.value[stat] + delta;
  if (nextValue < 1 || nextValue > 10) {
    return;
  }

  editableMainStats.value = {
    ...editableMainStats.value,
    [stat]: nextValue,
  };
}

async function saveMainStats() {
  if (!selectedCaravan.value || !editableMainStats.value || !canEditMainStats.value) {
    return;
  }

  mainStatsSubmitting.value = true;
  pendingAction.value = "save-main-stats";
  error.value = null;

  try {
    await updateCaravanMainStats(selectedCaravan.value.id, {
      offense: editableMainStats.value.offense,
      defense: editableMainStats.value.defense,
      mobility: editableMainStats.value.mobility,
      morale: editableMainStats.value.morale,
    });
    window.location.reload();
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "No se pudieron guardar los atributos principales";
  } finally {
    mainStatsSubmitting.value = false;
    pendingAction.value = null;
  }
}

async function refresh() {
  const previousAction = pendingAction.value;
  const trackRefresh = previousAction === null;

  if (trackRefresh) {
    pendingAction.value = "refresh";
  }

  loading.value = true;
  error.value = null;

  try {
    const [caravanList, activeResponse] = await Promise.all([listCaravans(), getActiveCaravan()]);
    caravans.value = caravanList;
    activeCaravan.value = activeResponse.caravan;
    const selectedId = activeResponse.caravan?.id ?? caravanList.find((caravan) => caravan.active)?.id ?? null;
    caravanStatistics.value = selectedId ? await getCaravanStatistics(selectedId) : null;
    syncEditableMainStats(activeResponse.caravan ?? caravanList.find((caravan) => caravan.active) ?? null);
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "Failed to load caravans";
  } finally {
    loading.value = false;
    pendingAction.value = trackRefresh ? null : previousAction;
  }
}

async function handleCreate() {
  if (!name.value.trim()) {
    error.value = "Caravan name is required";
    return;
  }

  const caravanName = name.value.trim();
  submitting.value = true;
  pendingAction.value = "create";
  error.value = null;

  try {
    const created = await createCaravan({
      name: caravanName,
      description: description.value.trim() || undefined,
      offense: offense.value,
      defense: defense.value,
      mobility: mobility.value,
      morale: morale.value,
    });

    const active = await selectActiveCaravan({ caravanId: created.id });
    await refresh();
    activeCaravan.value = active.caravan;
    closeCreateModal();
    name.value = "";
    description.value = "";
    offense.value = 1;
    defense.value = 1;
    mobility.value = 1;
    morale.value = 1;
    showToast(`Caravana creada: ${caravanName}.`);
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "Failed to create caravan";
  } finally {
    submitting.value = false;
    pendingAction.value = null;
  }
}

async function handleSelect(caravanId: string) {
  submitting.value = true;
  pendingAction.value = `select:${caravanId}`;
  error.value = null;

  try {
    const active = await selectActiveCaravan({ caravanId });
    activeCaravan.value = active.caravan;
    await refresh();
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "Failed to select caravan";
  } finally {
    submitting.value = false;
    pendingAction.value = null;
  }
}

async function handleDelete(caravan: Caravan) {
  const confirmed = window.confirm(
    `¿Seguro que quieres eliminar la caravana "${caravan.name}"? Esta acción no se puede deshacer.`
  );

  if (!confirmed) {
    return;
  }

  submitting.value = true;
  pendingAction.value = `delete:${caravan.id}`;
  error.value = null;

  try {
    await deleteCaravan(caravan.id);
    await refresh();
    showToast(`Caravana eliminada: ${caravan.name}.`);
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "Failed to delete caravan";
  } finally {
    submitting.value = false;
    pendingAction.value = null;
  }
}

async function handleExportBackup() {
  if (!selectedCaravan.value) {
    return;
  }

  const caravan = selectedCaravan.value;
  pendingAction.value = "export-backup";
  error.value = null;

  try {
    const backup = await exportCaravanBackup(caravan.id);
    downloadBackupFile(buildBackupFilename(caravan.name), backup);
    showToast(`Backup exportado: ${caravan.name}.`);
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "No se pudo exportar el backup";
  } finally {
    pendingAction.value = null;
  }
}

async function handleImportBackup(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = "";

  if (!file) {
    return;
  }

  const confirmed = window.confirm(
    "La importación reemplazará la caravana del backup si ya existe. ¿Quieres continuar?",
  );

  if (!confirmed) {
    return;
  }

  pendingAction.value = "import-backup";
  error.value = null;

  try {
    const rawBackup = await file.text();
    const backup = JSON.parse(rawBackup) as Parameters<typeof importCaravanBackup>[0];
    const restored = await importCaravanBackup(backup);
    await refresh();
    showToast(`Backup importado: ${restored.name}.`);
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "No se pudo importar el backup";
  } finally {
    pendingAction.value = null;
  }
}

async function adjustCaravanLevel(delta: number) {
  if (!selectedCaravan.value) {
    return;
  }

  const caravanId = selectedCaravan.value.id;
  const confirmed = window.confirm(
    delta > 0
      ? `¿Seguro que quieres subir de nivel la caravana "${selectedCaravan.value.name}"?`
      : `¿Seguro que quieres bajar de nivel la caravana "${selectedCaravan.value.name}"?`,
  );

  if (!confirmed) {
    return;
  }

  const action = `level:${delta > 0 ? "up" : "down"}`;
  submitting.value = true;
  pendingAction.value = action;
  error.value = null;

  try {
    await updateCaravanLevel(caravanId, { delta });
    await refresh();
    showToast(delta > 0 ? "Nivel de la caravana aumentado." : "Nivel de la caravana reducido.");
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "No se pudo ajustar el nivel";
  } finally {
    submitting.value = false;
    pendingAction.value = null;
  }
}

async function adjustCaravanDiscontent(delta: number) {
  if (!selectedCaravan.value) {
    return;
  }

  const caravanId = selectedCaravan.value.id;
  const action = `discontent:${delta > 0 ? "up" : "down"}`;
  submitting.value = true;
  pendingAction.value = action;
  error.value = null;

  try {
    await updateCaravanDiscontent(caravanId, { delta });
    await refresh();
    showToast(delta > 0 ? "Descontento aumentado." : "Descontento reducido.");
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "No se pudo ajustar el descontento";
  } finally {
    submitting.value = false;
    pendingAction.value = null;
  }
}

onMounted(refresh);
</script>

<template>
  <main class="page">
    <section class="shell">
      <header class="hero">
        <div>
          <p class="eyebrow">GestionCaravana</p>
          <h1>Caravana activa</h1>
          <p class="subtitle">
            Crea una campaña, selecciónala y trabaja siempre sobre la misma instancia.
          </p>
        </div>
        <div class="hero-actions">
          <button class="secondary-button" type="button" :disabled="loading || submitting || !selectedCaravan" @click="openDayCycleModal">
            Pasar el día
          </button>
          <button
            class="secondary-button"
            type="button"
            :disabled="loading || submitting || !selectedCaravan || isPending('export-backup') || isPending('import-backup')"
            @click="handleExportBackup"
          >
            <span class="button-with-spinner">
              <span v-if="isPending('export-backup')" class="button-spinner" aria-hidden="true"></span>
              <span>{{ isPending('export-backup') ? "Exportando…" : "Exportar backup" }}</span>
            </span>
          </button>
          <button
            class="secondary-button"
            type="button"
            :disabled="loading || submitting || isPending('export-backup') || isPending('import-backup')"
            @click="openBackupImportDialog"
          >
            Importar backup
          </button>
          <button class="secondary-button" type="button" :disabled="loading || submitting" @click="openCreateModal">
            Crear caravana
          </button>
          <button class="ghost-button" type="button" :disabled="loading || submitting" @click="refresh">
            <span class="button-with-spinner">
              <span v-if="isPending('refresh')" class="button-spinner" aria-hidden="true"></span>
              <span>{{ isPending('refresh') ? "Refrescando…" : "Refrescar" }}</span>
            </span>
          </button>
        </div>
      </header>

      <input
        ref="backupFileInput"
        accept="application/json,.json"
        type="file"
        style="display: none"
        @change="handleImportBackup"
      />

      <p v-if="error" class="error">{{ error }}</p>

      <section class="grid">
        <article class="card">
          <h2>Caravana seleccionada</h2>

          <div v-if="loading" class="muted">Cargando caravanas…</div>
          <div v-else-if="selectedCaravan" class="detail">
            <div class="title-row">
              <strong>{{ selectedCaravan.name }}</strong>
              <span class="pill" :class="{ active: selectedCaravan.active }">
                {{ selectedCaravan.active ? "Activa" : "Disponible" }}
              </span>
            </div>
            <p v-if="selectedCaravan.description" class="muted">
              {{ selectedCaravan.description }}
            </p>

            <dl class="stats">
              <div class="stat-control-card">
                <dt>Nivel</dt>
                <dd>{{ selectedCaravan.level }}</dd>
                <div class="stat-control-actions" aria-label="Controles de nivel">
                  <button
                    class="stat-control-button"
                    type="button"
                    :disabled="loading || submitting || selectedCaravan.level <= 1"
                    :aria-busy="isPending('level:down')"
                    @click="adjustCaravanLevel(-1)"
                  >
                    <span class="button-with-spinner">
                      <span v-if="isPending('level:down')" class="button-spinner" aria-hidden="true"></span>
                      <span>-</span>
                    </span>
                  </button>
                  <button
                    class="stat-control-button"
                    type="button"
                    :disabled="loading || submitting"
                    :aria-busy="isPending('level:up')"
                    @click="adjustCaravanLevel(1)"
                  >
                    <span class="button-with-spinner">
                      <span v-if="isPending('level:up')" class="button-spinner" aria-hidden="true"></span>
                      <span>+</span>
                    </span>
                  </button>
                </div>
              </div>
              <div>
                <dt>Ofensiva</dt>
                <dd>{{ displayedMainStats?.offense ?? selectedCaravan.mainStats.offense }}</dd>
                <div v-if="canEditMainStats" class="stat-control-actions">
                  <button
                    class="stat-control-button"
                    type="button"
                    :disabled="loading || submitting || mainStatsRemainingPoints <= 0 || (displayedMainStats?.offense ?? 0) >= 10"
                    @click="adjustEditableMainStat('offense', 1)"
                  >
                    +
                  </button>
                  <button
                    class="stat-control-button"
                    type="button"
                    :disabled="loading || submitting || (displayedMainStats?.offense ?? 0) <= 1"
                    @click="adjustEditableMainStat('offense', -1)"
                  >
                    -
                  </button>
                </div>
              </div>
              <div>
                <dt>Defensa</dt>
                <dd>{{ displayedMainStats?.defense ?? selectedCaravan.mainStats.defense }}</dd>
                <div v-if="canEditMainStats" class="stat-control-actions">
                  <button
                    class="stat-control-button"
                    type="button"
                    :disabled="loading || submitting || mainStatsRemainingPoints <= 0 || (displayedMainStats?.defense ?? 0) >= 10"
                    @click="adjustEditableMainStat('defense', 1)"
                  >
                    +
                  </button>
                  <button
                    class="stat-control-button"
                    type="button"
                    :disabled="loading || submitting || (displayedMainStats?.defense ?? 0) <= 1"
                    @click="adjustEditableMainStat('defense', -1)"
                  >
                    -
                  </button>
                </div>
              </div>
              <div>
                <dt>Movilidad</dt>
                <dd>{{ displayedMainStats?.mobility ?? selectedCaravan.mainStats.mobility }}</dd>
                <div v-if="canEditMainStats" class="stat-control-actions">
                  <button
                    class="stat-control-button"
                    type="button"
                    :disabled="loading || submitting || mainStatsRemainingPoints <= 0 || (displayedMainStats?.mobility ?? 0) >= 10"
                    @click="adjustEditableMainStat('mobility', 1)"
                  >
                    +
                  </button>
                  <button
                    class="stat-control-button"
                    type="button"
                    :disabled="loading || submitting || (displayedMainStats?.mobility ?? 0) <= 1"
                    @click="adjustEditableMainStat('mobility', -1)"
                  >
                    -
                  </button>
                </div>
              </div>
              <div>
                <dt>Moral</dt>
                <dd>{{ displayedMainStats?.morale ?? selectedCaravan.mainStats.morale }}</dd>
                <div v-if="canEditMainStats" class="stat-control-actions">
                  <button
                    class="stat-control-button"
                    type="button"
                    :disabled="loading || submitting || mainStatsRemainingPoints <= 0 || (displayedMainStats?.morale ?? 0) >= 10"
                    @click="adjustEditableMainStat('morale', 1)"
                  >
                    +
                  </button>
                  <button
                    class="stat-control-button"
                    type="button"
                    :disabled="loading || submitting || (displayedMainStats?.morale ?? 0) <= 1"
                    @click="adjustEditableMainStat('morale', -1)"
                  >
                    -
                  </button>
                </div>
              </div>
              <div v-if="canEditMainStats">
                <dt>Puntos libres</dt>
                <dd>{{ mainStatsRemainingPoints }}</dd>
              </div>
            </dl>
            <div v-if="canEditMainStats" class="main-stats-actions">
              <button
                class="primary-button"
                type="button"
                :disabled="loading || submitting || mainStatsSubmitting || !hasMainStatsChanges"
                :aria-busy="isPending('save-main-stats')"
                @click="saveMainStats"
              >
                <span class="button-with-spinner">
                  <span v-if="isPending('save-main-stats')" class="button-spinner" aria-hidden="true"></span>
                  <span>{{ isPending('save-main-stats') ? "Guardando…" : "Guardar" }}</span>
                </span>
              </button>
            </div>

            <div v-if="caravanStatistics" class="stats-panel">
              <section>
                <h3>Estadísticas derivadas automáticas</h3>
                <dl class="stats stats-4">
                  <div>
                    <dt>Ataque</dt>
                    <dd>{{ caravanStatistics.derivedStats.attack }}</dd>
                  </div>
                  <div>
                    <dt>CA</dt>
                    <dd>{{ caravanStatistics.derivedStats.armorClass }}</dd>
                  </div>
                  <div>
                    <dt>Seguridad</dt>
                    <dd>{{ caravanStatistics.derivedStats.security }}</dd>
                  </div>
                  <div>
                    <dt>Determinación</dt>
                    <dd>{{ caravanStatistics.derivedStats.determination }}</dd>
                  </div>
                </dl>
              </section>

              <section>
                <h3>Estadísticas de la caravana</h3>
                <dl class="stats stats-3">
                  <div>
                    <dt>Velocidad</dt>
                    <dd>{{ caravanStatistics.otherStats.speed }} mi/día</dd>
                  </div>
                  <div>
                    <dt>Consumo total</dt>
                    <dd>{{ caravanStatistics.otherStats.consumption }}</dd>
                  </div>
                  <div>
                    <dt>Dotes</dt>
                    <dd>{{ selectedCaravan.feats.length }}</dd>
                  </div>
                </dl>

                <div class="sections sections-3 sections-navigation">
                  <RouterLink class="nav-card" to="/wagons">
                    <h3>Carros</h3>
                    <p class="muted">{{ caravanStatistics.otherStats.wagonCount }} / {{ caravanStatistics.otherStats.maxWagons }}</p>
                    <div
                      class="nav-meter nav-meter--wagons"
                      :aria-label="`Carros ocupados ${caravanStatistics.otherStats.wagonCount} de ${caravanStatistics.otherStats.maxWagons}`"
                      :title="`Carros ocupados ${caravanStatistics.otherStats.wagonCount} de ${caravanStatistics.otherStats.maxWagons}`"
                    >
                      <span
                        class="nav-meter-segment nav-meter-segment--wagons"
                        :style="{ width: `${percentageOf(caravanStatistics.otherStats.wagonCount, caravanStatistics.otherStats.maxWagons)}%` }"
                      ></span>
                    </div>
                    <span class="nav-card-link">Abrir vista de carros</span>
                  </RouterLink>
                  <RouterLink class="nav-card" to="/travelers">
                    <h3>Viajeros</h3>
                    <p class="muted">
                      {{ caravanStatistics.otherStats.travelerCount }} + {{ caravanStatistics.otherStats.beastCount }} /
                      {{ caravanStatistics.otherStats.travelerCapacity }}
                    </p>
                    <div
                      class="nav-meter nav-meter--travelers"
                      :aria-label="`Viajeros ${caravanStatistics.otherStats.travelerCount} y bestias ${caravanStatistics.otherStats.beastCount} de ${caravanStatistics.otherStats.travelerCapacity}`"
                      :title="`Viajeros ${caravanStatistics.otherStats.travelerCount} · Bestias ${caravanStatistics.otherStats.beastCount} de ${caravanStatistics.otherStats.travelerCapacity}`"
                    >
                      <span
                        class="nav-meter-segment nav-meter-segment--travelers"
                        :style="{ width: `${percentageOf(caravanStatistics.otherStats.travelerCount, caravanStatistics.otherStats.travelerCapacity)}%` }"
                      ></span>
                      <span
                        class="nav-meter-segment nav-meter-segment--beasts"
                        :style="{ width: `${percentageOf(caravanStatistics.otherStats.beastCount, caravanStatistics.otherStats.travelerCapacity)}%` }"
                      ></span>
                    </div>
                    <span class="nav-card-link">Abrir vista de viajeros</span>
                  </RouterLink>
                  <RouterLink class="nav-card" to="/cargo">
                    <h3>Cargamento</h3>
                    <p class="muted">{{ caravanStatistics.otherStats.cargoLoad }} / {{ caravanStatistics.otherStats.cargoCapacity }}</p>
                    <div
                      class="nav-meter nav-meter--cargo"
                      :aria-label="`Carga ocupada ${caravanStatistics.otherStats.cargoLoad} de ${caravanStatistics.otherStats.cargoCapacity}`"
                      :title="`Carga ocupada ${caravanStatistics.otherStats.cargoLoad} de ${caravanStatistics.otherStats.cargoCapacity}`"
                    >
                      <span
                        class="nav-meter-segment nav-meter-segment--cargo"
                        :style="{ width: `${percentageOf(caravanStatistics.otherStats.cargoLoad, caravanStatistics.otherStats.cargoCapacity)}%` }"
                      ></span>
                    </div>
                    <span class="nav-card-link">Abrir vista de carga</span>
                  </RouterLink>
                </div>
              </section>

              <section>
                <h3>Descontento</h3>
                <p :class="['warning-banner', { danger: caravanStatistics.discontent >= caravanStatistics.moraleThreshold }]">
                  Descontento: {{ caravanStatistics.discontent }} · Umbral de motín: {{ caravanStatistics.moraleThreshold }}
                </p>
                <div class="stat-control-actions stat-control-actions--spaced" aria-label="Controles de descontento">
                  <button
                    class="stat-control-button"
                    type="button"
                    :disabled="loading || submitting || caravanStatistics.discontent <= 0"
                    :aria-busy="isPending('discontent:down')"
                    @click="adjustCaravanDiscontent(-1)"
                  >
                    <span class="button-with-spinner">
                      <span v-if="isPending('discontent:down')" class="button-spinner" aria-hidden="true"></span>
                      <span>-</span>
                    </span>
                  </button>
                  <button
                    class="stat-control-button"
                    type="button"
                    :disabled="loading || submitting"
                    :aria-busy="isPending('discontent:up')"
                    @click="adjustCaravanDiscontent(1)"
                  >
                    <span class="button-with-spinner">
                      <span v-if="isPending('discontent:up')" class="button-spinner" aria-hidden="true"></span>
                      <span>+</span>
                    </span>
                  </button>
                </div>
                <div class="summary-meter-block">
                  <div
                    class="meter-strip"
                    :aria-label="`Descontento ${caravanStatistics.discontent} de ${caravanStatistics.moraleThreshold}`"
                    :title="`Descontento ${caravanStatistics.discontent} de ${caravanStatistics.moraleThreshold}`"
                  >
                    <span
                      class="meter-segment meter-segment--discontent"
                      :style="{ width: `${percentageOf(caravanStatistics.discontent, caravanStatistics.moraleThreshold)}%` }"
                    ></span>
                  </div>
                  <div class="meter-values">
                    <span><strong>{{ caravanStatistics.discontent }}</strong> descontento actual</span>
                    <span><strong>{{ caravanStatistics.moraleThreshold }}</strong> umbral de motín</span>
                  </div>
                </div>
                <p v-if="caravanStatistics.warnings.length > 0" class="muted">
                  {{ caravanStatistics.warnings.join(" ") }}
                </p>
              </section>

            <section>
              <h3>Bonificadores aplicados</h3>
              <ul class="contribution-list">
                  <li v-for="item in visibleContributions" :key="`${item.statCode}-${item.sourceType}-${item.sourceId}-${item.modifier}`">
                    <strong>{{ item.statCode }}</strong>
                    <span>{{ item.sourceName }} · {{ item.modifier }} · {{ item.reason }}</span>
                  </li>
                  <li v-if="visibleContributions.length === 0" class="muted contribution-empty">
                    No hay bonificadores relevantes para mostrar.
                  </li>
                </ul>
              </section>
            </div>

          </div>

          <div v-else class="muted">
            Todavía no hay una caravana activa.
          </div>
        </article>
      </section>

      <article class="card">
        <h2>Instancias de caravana</h2>

        <div v-if="!loading && caravans.length === 0" class="muted">
          No hay caravanas creadas todavía.
        </div>

        <div class="list">
          <div
            v-for="caravan in caravans"
            :key="caravan.id"
            class="list-item"
            :class="{ selected: caravan.active }"
          >
            <div>
              <strong>{{ caravan.name }}</strong>
              <p v-if="caravan.description" class="muted">{{ caravan.description }}</p>
            </div>
            <div class="actions">
              <button class="secondary-button" type="button" :disabled="loading || submitting" @click.stop="handleSelect(caravan.id)">
                <span class="button-with-spinner">
                  <span v-if="isPending(`select:${caravan.id}`)" class="button-spinner" aria-hidden="true"></span>
                  <span>{{ isPending(`select:${caravan.id}`) ? "Seleccionando…" : "Seleccionar" }}</span>
                </span>
              </button>
              <button class="danger-button" type="button" :disabled="loading || submitting" @click.stop="handleDelete(caravan)">
                <span class="button-with-spinner">
                  <span v-if="isPending(`delete:${caravan.id}`)" class="button-spinner" aria-hidden="true"></span>
                  <span>{{ isPending(`delete:${caravan.id}`) ? "Eliminando…" : "Eliminar" }}</span>
                </span>
              </button>
              <span class="pill" :class="{ active: caravan.active }">
                {{ caravan.active ? "Activa" : "Disponible" }}
              </span>
            </div>
          </div>
        </div>
      </article>
    </section>

    <teleport to="body">
      <div v-if="dayCycleModalOpen" class="modal-backdrop" @click.self="closeDayCycleModal">
        <div class="modal modal-cycle">
          <div class="modal-header">
            <div>
              <p class="eyebrow">Ciclo diario</p>
              <h2>Pasar el día</h2>
              <p class="muted" v-if="selectedCaravan">
                {{ selectedCaravan.name }}
              </p>
            </div>
            <button class="ghost-button" type="button" @click="closeDayCycleModal">Cerrar</button>
          </div>

            <p v-if="dayCycleError" class="error">{{ dayCycleError }}</p>

            <div v-if="selectedCaravan" class="day-cycle-layout">
              <section class="card card-compact">
                <h3>Opciones del día</h3>
                <p v-if="hasDayCycleShortage" class="warning-banner danger day-cycle-alert">
                  <strong>Desabastecimiento previsto.</strong>
                  Faltan <strong>{{ dayCycleShortage }}</strong> provisiones para cubrir el consumo diario.
                </p>
                <label class="toggle-row">
                  <input
                    v-model="dayCycleFasting"
                    :disabled="!canUseIntermittentFasting"
                    type="checkbox"
                  @change="refreshDayCyclePreview"
                />
                <span>Activar Ayuno Intermitente</span>
              </label>
              <p v-if="!canUseIntermittentFasting" class="muted">Requiere la dote Ayuno Intermitente.</p>

              <div v-for="traveler in dayCycleTravelers.filter((item) => item.activeRoleCode === 'batidor')" :key="traveler.id" class="day-choice-row">
                <strong>{{ traveler.fullName }}</strong>
                <select v-model="dayCycleChoices[traveler.id]" @change="refreshDayCyclePreview">
                  <option value="HUNT">Cazar</option>
                  <option value="EXPLORE">Explorar</option>
                </select>
              </div>

              <button class="secondary-button" type="button" :disabled="dayCycleSubmitting" @click="refreshDayCyclePreview">
                Recalcular
              </button>
            </section>

                <section class="card card-compact" v-if="dayCyclePreview">
                  <h3>Vista previa</h3>
                  <section class="preview-definition-block">
                    <h4>Estado inicial</h4>
                    <p><strong>Reserva actual:</strong> {{ dayCyclePreview.currentReserve }} Unidades de suministros.</p>
                    <p><strong>Provisiones en consumo:</strong> {{ dayCyclePreview.initialProvisionsInConsumption.length }} Unidades de suministros ya abiertas</p>
                    <p><strong>Consumo:</strong> {{ dayCyclePreview.expectedConsumption }}</p>
                    <p><strong>Provisiones generadas:</strong> {{ dayCyclePreview.generatedProvisions }} Unidades de suministros generadas</p>
                    <p><strong>Comida generada:</strong> {{ dayCyclePreview.generatedFood }}</p>
                    <h4>Estado final de provisiones</h4>
                    <p><strong>Provisiones consumidas:</strong> {{ dayCyclePreview.consumedProvisions }} Unidades de suministros consumidos</p>
                    <p><strong>Provisiones sobrantes:</strong> {{ dayCyclePreview.surplusProvisions }} Unidades de suministros excedentes</p>
                  </section>

                <section>
                  <h4>DESGLOSE</h4>

                  <section>
                    <h5>CONSUMO DE LA CARAVANA</h5>
                    <div v-if="dayCyclePreview.contributions.length > 0" class="contribution-groups">
                      <section class="contribution-group">
                        <ul class="simple-list contribution-group-list">
                          <li
                            v-for="item in dayCyclePreview.contributions.filter((contribution) => contribution.effectCode === 'consumption')"
                            :key="`${item.effectCode}-${item.sourceType}-${item.sourceId}-${item.quantity}`"
                          >
                            <strong>{{ item.sourceName }}</strong>
                            <span> · {{ contributionQuantityText(item) }} · {{ item.reason }}</span>
                          </li>
                        </ul>
                      </section>
                      <section v-for="group in dayCycleFoodGroups" :key="group.groupLabel" class="contribution-group">
                        <h5>
                          {{ group.groupLabel }} ({{
                            group.groupLabel === 'Batidores'
                              ? `+${group.contributions.reduce((sum, item) => sum + item.quantity, 0)} de comida en total`
                              : `-${dayCyclePreview.contributions.filter((contribution) => contribution.sourceRoleName === 'Cocinero' && contribution.sourceName !== 'Trabajo En Equipo' && contribution.applied).length} unidad de suministros, +${group.contributions.reduce((sum, item) => sum + item.quantity, 0)} comida en total`
                          }})
                        </h5>
                        <ul class="simple-list contribution-group-list">
                          <li
                            v-for="item in group.contributions"
                            :key="`${item.effectCode}-${item.sourceType}-${item.sourceId}-${item.quantity}`"
                          >
                            <strong>{{ item.sourceName }}</strong>
                            <span> · {{ contributionQuantityText(item) }} · {{ item.reason }}</span>
                          </li>
                        </ul>
                      </section>
                      <section class="contribution-group">
                        <h5>
                          Suministros (-{{ dayCycleLateSupplyLoads }} unidad de suministros, +{{ dayCycleLateSupplyLoads * 10 }} comida en total)
                        </h5>
                        <ul class="simple-list contribution-group-list">
                          <li class="muted contribution-empty">
                            Faltan {{ dayCycleLateSupplyFoodNeeded }} de comida y por eso se abren {{ dayCycleLateSupplyLoads }} suministros.
                          </li>
                        </ul>
                      </section>
                    </div>
                    <p v-else class="muted contribution-empty">No hay contribuciones para mostrar.</p>
                  </section>

                  <section class="day-cycle-result-block">
                    <h5>RESULTADO DE CONSUMO:</h5>
                    <ul class="simple-list">
                      <li>- {{ dayCyclePreview.consumedProvisions }} Unidades de suministros</li>
                      <li>+ {{ dayCyclePreview.expectedGeneration }} comida generada</li>
                      <li>- {{ dayCyclePreview.expectedConsumption }} de consumo</li>
                    </ul>
                    <p>
                      <strong>TOTAL:</strong>
                      {{
                        dayCyclePreview.expectedNetDelta >= 0
                          ? `${dayCyclePreview.expectedNetDelta} de comida excedente`
                          : `${Math.abs(dayCyclePreview.expectedNetDelta)} de comida faltante`
                      }}
                    </p>
                  </section>

                  <section>
                    <h5>OTRAS GENERACIONES:</h5>
                    <div v-if="dayCycleOtherGroups.length > 0" class="contribution-groups">
                      <section v-for="group in dayCycleOtherGroups" :key="group.groupLabel" class="contribution-group">
                        <h5>{{ group.groupLabel }} (+{{ group.contributions.reduce((sum, item) => sum + item.quantity, 0) }} {{ group.groupLabel === 'Boticario' ? 'po en artículos alquímicos no mágicos en total' : 'en total' }})</h5>
                        <ul class="simple-list contribution-group-list">
                          <li
                            v-for="item in group.contributions"
                            :key="`${item.effectCode}-${item.sourceType}-${item.sourceId}-${item.quantity}`"
                          >
                            <strong>{{ item.sourceName }}</strong>
                            <span> · {{ contributionQuantityText(item) }} · {{ item.reason }}</span>
                          </li>
                        </ul>
                      </section>
                    </div>
                    <p v-else class="muted contribution-empty">No hay otras generaciones para mostrar.</p>
                  </section>

                    <section>
                      <h5>RESUMEN FINAL DE UNIDADES DE SUMINISTROS</h5>
                      <ul class="simple-list">
                        <li>{{ dayCyclePreview.currentReserve }} Unidades de suministros almacenadas originalmente</li>
                        <li>+{{ dayCyclePreview.generatedProvisions }} Unidades de suministros generadas por agricultores</li>
                        <li>
                          -{{ dayCycleCookSupplyLoads }}
                        Unidades de suministros utilizadas por cocineros
                      </li>
                      <li>-{{ dayCycleLateSupplyUnitsUsed }} Unidades de suministros utilizadas</li>
                      <li v-if="dayCyclePartialSupply">-1 Unidad de suministros utilizada parcialmente</li>
                      <li><strong>TOTAL:</strong></li>
                      <li>{{ dayCyclePreview.currentReserve }} Unidades de suministros almacenadas originalmente</li>
                      <li>+{{ dayCycleCompleteSurplusLoads }} Unidades de suministros excedentes y almacenadas</li>
                      <li v-if="dayCyclePartialSupply">
                        +1 Unidad de suministros parcial ({{ dayCyclePartialSupply.remainingFood }} comida restante) excedente y almacenada
                      </li>
                      </ul>
                    </section>

                    <section>
                      <h5>ALMACENAMIENTO DE EXCEDENTE DE SUMINISTROS:</h5>
                      <ul class="simple-list">
                      <li v-if="dayCycleCompleteSurplusLoads > 0">
                        Suministros 2 · +{{ dayCycleCompleteSurplusLoads }} Unidad{{ dayCycleCompleteSurplusLoads === 1 ? "" : "es" }} de suministros
                      </li>
                        <li v-if="dayCyclePartialSupply">
                          Suministros 1 · +1 Unidad de suministros ({{ dayCyclePartialSupply.remainingFood }} comida restante)
                        </li>
                        <li v-if="dayCycleCompleteSurplusLoads === 0 && dayCyclePreview.provisionsInConsumption.length === 0">
                          No hay excedente de suministros para almacenar.
                        </li>
                      </ul>
                    </section>
                  </section>

              <section v-if="dayCyclePreview.warnings.length > 0">
                <h4>Advertencias</h4>
                <ul class="simple-list">
                  <li v-for="warning in dayCyclePreview.warnings" :key="warning">{{ warning }}</li>
                </ul>
              </section>
            </section>
          </div>

          <div class="modal-actions">
            <button class="secondary-button" type="button" @click="closeDayCycleModal">Cancelar</button>
            <button class="primary-button" type="button" :disabled="dayCycleSubmitting || !dayCyclePreview" @click="handleAdvanceDayCycle">
              {{ dayCycleSubmitting ? "Avanzando…" : "Confirmar día" }}
            </button>
          </div>
        </div>
      </div>

      <div v-if="createModalOpen" class="modal-backdrop" @click.self="closeCreateModal">
        <div class="modal modal-create">
          <div class="modal-header">
            <div>
              <p class="eyebrow">Nueva caravana</p>
              <h2>Crear caravana</h2>
            </div>
            <button class="ghost-button" type="button" @click="closeCreateModal">Cerrar</button>
          </div>

          <form class="form" @submit.prevent="handleCreate">
            <label>
              <span>Nombre</span>
              <input v-model="name" type="text" placeholder="Campaña del norte" />
            </label>

            <label>
              <span>Descripción</span>
              <textarea v-model="description" rows="3" placeholder="Opcional"></textarea>
            </label>

            <div class="allocation-grid">
              <label>
                <span>Ofensiva</span>
                <input v-model.number="offense" type="number" min="1" max="10" />
              </label>
              <label>
                <span>Defensa</span>
                <input v-model.number="defense" type="number" min="1" max="10" />
              </label>
              <label>
                <span>Movilidad</span>
                <input v-model.number="mobility" type="number" min="1" max="10" />
              </label>
              <label>
                <span>Moral</span>
                <input v-model.number="morale" type="number" min="1" max="10" />
              </label>
            </div>
            <p class="muted">
              Puntos asignados: {{ allocatedPoints }} / 3 · Puntos libres: {{ remainingPoints }}
            </p>

            <section class="derived-preview">
              <h3>Vista previa automática</h3>
              <dl class="stats stats-2">
                <div>
                  <dt>Ataque</dt>
                  <dd>{{ automaticDerivedStats.attack }}</dd>
                </div>
                <div>
                  <dt>CA</dt>
                  <dd>{{ automaticDerivedStats.armorClass }}</dd>
                </div>
                <div>
                  <dt>Seguridad</dt>
                  <dd>{{ automaticDerivedStats.security }}</dd>
                </div>
                <div>
                  <dt>Determinación</dt>
                  <dd>{{ automaticDerivedStats.determination }}</dd>
                </div>
              </dl>
              <p class="muted">
                Estas estadísticas se calculan automáticamente a partir de tus atributos principales.
              </p>
            </section>

            <div class="modal-actions">
              <button class="secondary-button" type="button" @click="closeCreateModal">Cancelar</button>
              <button class="primary-button" type="submit" :disabled="loading || submitting || remainingPoints < 0" :aria-busy="isPending('create')">
                <span class="button-with-spinner">
                  <span v-if="isPending('create')" class="button-spinner" aria-hidden="true"></span>
                  <span>{{ isPending('create') ? "Creando…" : "Crear y seleccionar" }}</span>
                </span>
              </button>
            </div>
          </form>
        </div>
      </div>
    </teleport>
  </main>
</template>

<style scoped>
.page {
  min-height: 100vh;
  padding: 2rem;
}

.shell {
  width: min(1100px, 100%);
  margin: 0 auto;
  display: grid;
  gap: 1.25rem;
}

.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.eyebrow {
  margin: 0 0 0.25rem;
  font-size: 0.875rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #6b7280;
}

h1,
h2,
h3,
p {
  margin: 0;
}

.subtitle,
.muted {
  color: #6b7280;
}

.grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 1.25rem;
}

.hero-actions {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.card {
  padding: 1.25rem;
  border: 1px solid #e5e7eb;
  border-radius: 1rem;
  background: white;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.08);
}

.form {
  display: grid;
  gap: 1rem;
  margin-top: 1rem;
}

label {
  display: grid;
  gap: 0.35rem;
}

input,
textarea {
  width: 100%;
  padding: 0.8rem 0.9rem;
  border: 1px solid #d1d5db;
  border-radius: 0.75rem;
  font: inherit;
}

.primary-button,
.ghost-button,
.secondary-button,
.danger-button,
.list-item {
  border-radius: 0.85rem;
  font: inherit;
}

.primary-button,
.ghost-button {
  padding: 0.8rem 1rem;
  border: 1px solid #cbd5e1;
  cursor: pointer;
}

.primary-button {
  background: #1d4ed8;
  border-color: #1d4ed8;
  color: white;
}

.ghost-button {
  background: white;
}

.secondary-button {
  background: white;
  border: 1px solid #cbd5e1;
  padding: 0.7rem 0.9rem;
  cursor: pointer;
}

.danger-button {
  background: #fee2e2;
  color: #991b1b;
  border: 1px solid #fecaca;
  padding: 0.7rem 0.9rem;
  cursor: pointer;
}

.error {
  padding: 0.85rem 1rem;
  border-radius: 0.85rem;
  background: #fef2f2;
  color: #b91c1c;
}

.detail {
  display: grid;
  gap: 1rem;
  margin-top: 1rem;
}

.title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.pill {
  padding: 0.25rem 0.6rem;
  border-radius: 999px;
  background: #e5e7eb;
  color: #374151;
  font-size: 0.875rem;
}

.pill.active {
  background: #dbeafe;
  color: #1d4ed8;
}

.stats {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 0.75rem;
}

.stats.stats-2 {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.stats.stats-4 {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.stats.stats-3 {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.stats div {
  padding: 0.85rem;
  border-radius: 0.85rem;
  background: #f9fafb;
  min-width: 0;
}

@media (max-width: 1100px) {
  .stats {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

.stat-control-card {
  display: grid;
  gap: 0.5rem;
}

.stat-with-meter {
  display: grid;
  gap: 0.45rem;
}

.stat-control-actions {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.stat-control-actions--spaced {
  margin: 0.5rem 0 0.25rem;
}

.main-stats-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 0.75rem;
}

.stat-control-button {
  min-width: 2.25rem;
  padding: 0.45rem 0.75rem;
  border-radius: 0.7rem;
  border: 1px solid #cbd5e1;
  background: white;
  cursor: pointer;
  font: inherit;
  font-weight: 700;
}

.stat-control-button:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}

dt {
  font-size: 0.8rem;
  color: #6b7280;
}

dd {
  margin: 0.2rem 0 0;
  font-size: 1.15rem;
  font-weight: 700;
}

.sections {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.75rem;
}

.sections.sections-3 {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.sections section {
  padding: 0.85rem;
  border: 1px solid #e5e7eb;
  border-radius: 0.85rem;
  display: grid;
  gap: 0.35rem;
}

.nav-card {
  padding: 0.85rem;
  border: 1px solid #e5e7eb;
  border-radius: 0.85rem;
  display: grid;
  gap: 0.35rem;
  text-decoration: none;
  color: inherit;
  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease,
    transform 0.15s ease;
}

.nav-card:hover,
.nav-card:focus-visible {
  border-color: #cbd5e1;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.08);
  transform: translateY(-1px);
}

.nav-meter {
  display: flex;
  overflow: hidden;
  min-height: 0.6rem;
  border-radius: 999px;
  background: #e5e7eb;
}

.nav-meter-segment {
  display: block;
  height: 100%;
}

.nav-meter--wagons .nav-meter-segment--wagons {
  background: #7c3aed;
}

.nav-meter--travelers .nav-meter-segment--travelers {
  background: #2563eb;
}

.nav-meter--travelers .nav-meter-segment--beasts {
  background: #f97316;
}

.nav-meter--cargo .nav-meter-segment--cargo {
  background: #f59e0b;
}

.nav-card-link {
  color: #1d4ed8;
  text-decoration: none;
  font-weight: 600;
}

.sections-navigation {
  margin-top: 0.75rem;
}

.stat-progress-card {
  display: grid;
  gap: 0.6rem;
  min-width: 0;
}

.stat-progress-card dd {
  margin-bottom: 0.1rem;
}

.progress-meter {
  position: relative;
  overflow: hidden;
  height: 1.15rem;
  min-height: 1.15rem;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.55);
  box-shadow:
    inset 0 1px 2px rgba(15, 23, 42, 0.12),
    0 1px 0 rgba(255, 255, 255, 0.95);
}

.progress-meter--travelers {
  background-color: #2563eb;
}

.progress-meter--wagons {
  background-color: #7c3aed;
}

.progress-meter--cargo {
  background-color: #f59e0b;
}

.progress-meter-mark {
  position: absolute;
  top: 0.1rem;
  bottom: 0.1rem;
  width: 0.25rem;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 0 0 1px rgba(15, 23, 42, 0.12);
  pointer-events: none;
}

.progress-meta {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  font-size: 0.8rem;
  color: #475569;
}

.progress-meta strong {
  color: #111827;
}

.list {
  display: grid;
  gap: 0.75rem;
  margin-top: 1rem;
}

.list-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  border: 1px solid #e5e7eb;
  background: #fff;
  cursor: pointer;
  text-align: left;
}

.actions {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.list-item.selected {
  border-color: #1d4ed8;
  background: #eff6ff;
}

.allocation-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
}

.allocation-grid label {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.stats-panel {
  display: grid;
  gap: 1rem;
  margin-top: 1rem;
}

.contribution-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 0.5rem;
}

.contribution-list li {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  padding: 0.75rem 1rem;
  border: 1px solid rgba(148, 163, 184, 0.35);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.7);
}

.contribution-empty {
  list-style: none;
  text-align: center;
}

.contribution-groups {
  display: grid;
  gap: 1rem;
}

.contribution-group {
  display: grid;
  gap: 0.5rem;
}

.contribution-group h5 {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 700;
  color: #334155;
}

.contribution-group-list li {
  background: rgba(255, 255, 255, 0.85);
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.55);
  display: grid;
  place-items: center;
  padding: 1.5rem;
  z-index: 30;
}

.modal {
  width: min(1000px, 100%);
  max-height: 90vh;
  overflow: auto;
  background: white;
  border-radius: 1.2rem;
  padding: 1.25rem;
  box-shadow: 0 24px 48px rgba(15, 23, 42, 0.25);
  display: grid;
  gap: 1rem;
}

.modal-create {
  width: min(900px, 100%);
}

.modal-cycle {
  width: min(980px, 100%);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
}

.warning-banner {
  padding: 0.85rem 1rem;
  border: 1px solid rgba(148, 163, 184, 0.35);
  border-radius: 12px;
}

.warning-banner.danger {
  border-color: rgba(220, 38, 38, 0.45);
  background: rgba(254, 226, 226, 0.9);
}

.day-cycle-alert {
  margin-bottom: 0.75rem;
}

.day-cycle-layout {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 1rem;
}

.card-compact {
  box-shadow: none;
  background: #f8fafc;
}

.toggle-row,
.day-choice-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.day-choice-row {
  padding: 0.5rem 0;
  border-bottom: 1px solid #e5e7eb;
}

.simple-list {
  margin: 0.5rem 0 0;
  padding-left: 1.25rem;
}

@media (max-width: 900px) {
  .grid,
  .stats,
  .sections,
  .day-cycle-layout {
    grid-template-columns: 1fr;
  }

  .hero {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
