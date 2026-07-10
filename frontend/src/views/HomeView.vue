<script setup lang="ts">
import { computed, onMounted, ref } from "vue";

import {
  createCaravan,
  confirmCaravanDayCycle,
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
import type {
  Caravan,
  CaravanDayCyclePreview,
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
const dayCycleModalOpen = ref(false);
const dayCycleLoading = ref(false);
const dayCycleSubmitting = ref(false);
const dayCyclePreview = ref<CaravanDayCyclePreview | null>(null);
const caravanStatistics = ref<CaravanStatistics | null>(null);
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
const dayCycleTimelineTab = ref<string>("agricultors");

interface DayCycleTimelineCard {
  key: string;
  sectionLabel: string;
  title: string;
  details: string[];
  foodDelta: number;
  tone: "neutral" | "success" | "warning" | "info";
  resultLabel: string;
  isSummary: boolean;
}

interface DayCycleTimelineSection {
  key: string;
  label: string;
  cards: DayCycleTimelineCard[];
}

interface DayCycleCookGapSummary {
  units: number;
  food: number;
}

const dayCycleTimelineSections = computed<DayCycleTimelineSection[]>(() => {
  const entries = dayCyclePreview.value?.simulation ?? [];
  const sections = new Map<string, DayCycleTimelineCard[]>();
  const uncookedSummary = dayCycleUncookedSupplySummary(entries);

  entries.forEach((entry, index) => {
    if (entry.section === "food" && entry.title === "Se consume una unidad de suministros") {
      return;
    }
    if (entry.section === "inventory") {
      return;
    }

    const card = {
      key: `${entry.section}-${index}-${entry.title}`,
      sectionLabel: dayCycleSectionLabel(entry.section),
      title: entry.title,
      details: normalizeDayCycleDetails(entry.title, entry.section, entry.details),
      foodDelta: entry.foodDelta,
      tone: dayCycleTone(entry.section),
      resultLabel: dayCycleResultLabel(entry.section, entry.title, entry.details, entry.foodDelta),
      isSummary: entry.section.endsWith("summary"),
    } satisfies DayCycleTimelineCard;

    const bucketKey = sectionBucketKey(entry.section, entry.title, entry.details);
    const current = sections.get(bucketKey) ?? [];
    current.push(card);
    sections.set(bucketKey, current);
  });

  return Array.from(sections.entries()).map(([key, cards]) => ({
    key,
    label: cards[0]?.sectionLabel ?? "Paso",
    cards: key === "cocineros" && uncookedSummary
      ? [...cards, createCookGapSummaryCard(uncookedSummary)]
      : cards,
  }));
});

const dayCycleTimelineTabs = computed(() =>
  dayCycleTimelineSections.value.map((section) => ({
    key: section.key,
    label: dayCycleTimelineTabLabel(section.key),
  })),
);

const activeDayCycleTimelineTab = computed(() => {
  const tabs = dayCycleTimelineTabs.value;
  if (tabs.length === 0) {
    return "";
  }
  return tabs.some((tab) => tab.key === dayCycleTimelineTab.value) ? dayCycleTimelineTab.value : tabs[0].key;
});

const visibleDayCycleTimelineSections = computed(() =>
  dayCycleTimelineSections.value.filter((section) => section.key === activeDayCycleTimelineTab.value),
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

function formatDecimal(value: number) {
  return Number.isInteger(value) ? `${value}` : value.toFixed(1).replace(/\.0$/, "");
}

function dayCycleSectionLabel(section: string) {
  switch (section) {
    case "pre-food":
      return "Preparación";
    case "pre-food-summary":
      return "Resumen";
    case "batidor":
      return "Batidor";
    case "batidor-summary":
      return "Resumen";
    case "cook":
      return "Conversión de suministros";
    case "cook-summary":
      return "Resumen";
    case "food":
      return "Comida total";
    case "inventory":
      return "Inventario";
    case "leftover":
      return "Sobrante";
    case "cargo":
      return "Reasignación";
    default:
      return "Paso";
  }
}

function dayCycleTone(section: string): "neutral" | "success" | "warning" | "info" {
  switch (section) {
    case "cook":
      return "success";
    case "food":
      return "info";
    case "leftover":
      return "warning";
    default:
      return "neutral";
  }
}

function dayCycleUncookedSupplySummary(entries: CaravanDayCyclePreview["simulation"]) {
  const uncookedFoodEntries = entries.filter(
    (entry) =>
      entry.section === "food" &&
      entry.title === "Se consume una unidad de suministros" &&
      entry.details.some((detail) => detail.includes("No había cocinero disponible.")),
  );

  if (uncookedFoodEntries.length === 0) {
    return null;
  }

  return {
    units: uncookedFoodEntries.length,
    food: uncookedFoodEntries.reduce((total, entry) => total + entry.foodDelta, 0),
  } satisfies DayCycleCookGapSummary;
}

function createCookGapSummaryCard(summary: DayCycleCookGapSummary): DayCycleTimelineCard {
  return {
    key: `cook-gap-${summary.units}-${summary.food}`,
    sectionLabel: "Resumen",
    title: "Suministros restantes sin cocinero",
    details: [`${summary.units} unidades × 10 de comida = ${formatDecimal(summary.food)}`],
    foodDelta: summary.food,
    tone: "warning",
    resultLabel: `Comida +${formatDecimal(summary.food)}`,
    isSummary: true,
  };
}

function dayCycleDeltaTone(label: string) {
  if (label.includes("Suministros")) {
    return "supplies";
  }
  if (label.includes("Comida")) {
    return "food";
  }
  if (label.includes("PO") || label.includes("Oro")) {
    return "alchemy";
  }
  if (label.includes("Perecederos")) {
    return "other";
  }
  return "other";
}

function dayCycleGroupTitle(key: string) {
  switch (key) {
    case "agricultors":
      return "Agricultores";
    case "preparacion":
      return "Preparación";
    case "batidores":
      return "Batidores";
    case "cocineros":
      return "Cocineros";
    case "consumo":
      return "Consumo";
    case "inventario":
      return "Inventario";
    case "sobrante":
      return "Sobrante";
    case "reasignacion":
      return "Reasignación";
    default:
      return "Simulación";
  }
}

function dayCycleTimelineTabLabel(key: string) {
  switch (key) {
    case "agricultors":
      return "Agricultores";
    case "preparacion":
      return "Preparación";
    case "batidores":
      return "Batidores";
    case "cocineros":
      return "Cocineros";
    case "consumo":
      return "Comida total";
    case "sobrante":
      return "Sobrante";
    case "reasignacion":
      return "Reasignación";
    default:
      return "Simulación";
  }
}

function dayCycleGroupHint(key: string) {
  switch (key) {
    case "agricultors":
      return "Primero se resuelve la producción individual y después el resumen general.";
    case "preparacion":
      return "Aquí se agrupan boticarios y artesanos.";
    case "batidores":
      return "Se muestra la comida generada por cada batidor.";
    case "cocineros":
      return "Cada conversión de suministros indica el alimento resultante y el modificador aplicado.";
    case "consumo":
      return "Aquí queda reflejada la comida total antes de calcular el sobrante.";
    case "inventario":
      return "Las unidades se mueven antes de reasignarse.";
    case "sobrante":
      return "La comida sobrante se transforma en perecederos.";
    case "reasignacion":
      return "El inventario temporal vuelve a los carros con capacidad disponible.";
    default:
      return "";
  }
}

function sectionBucketKey(section: string, title: string, details: string[]) {
  const lowerTitle = title.toLowerCase();
  const lowerDetails = details.join(" ").toLowerCase();

  switch (section) {
    case "pre-food":
    case "pre-food-summary":
      if (lowerTitle.includes("agricultor") || lowerDetails.includes("actúa como agricultor")) {
        return "agricultors";
      }
      if (lowerTitle.includes("boticario") || lowerDetails.includes("actúa como boticario")) {
        return "preparacion";
      }
      if (lowerTitle.includes("artesano") || lowerDetails.includes("actúa como artesano")) {
        return "preparacion";
      }
      return "preparacion";
    case "batidor":
    case "batidor-summary":
      return "batidores";
    case "cook":
    case "cook-summary":
      return "cocineros";
    case "food":
      return "consumo";
    case "inventory":
      return "inventario";
    case "leftover":
      return "sobrante";
    case "cargo":
      return "reasignacion";
    default:
      return section;
  }
}

function dayCycleResultLabel(section: string, title: string, details: string[], foodDelta: number) {
  const lowerTitle = title.toLowerCase();
  const lowerDetails = details.join(" ").toLowerCase();

  if (section === "pre-food" || section === "pre-food-summary") {
    if (lowerTitle.includes("boticario") || lowerDetails.includes("actúa como boticario") || lowerDetails.includes("valor generado")) {
      return `PO +${formatDecimal(foodDelta)}`;
    }
    if (lowerTitle.includes("agricultor") || lowerDetails.includes("actúa como agricultor") || lowerDetails.includes("suministros generados")) {
      return `Suministros +${formatDecimal(foodDelta)}`;
    }
    if (lowerTitle.includes("artesano")) {
      return `Resumen`;
    }
    return `Resultado +${formatDecimal(foodDelta)}`;
  }
  if (section === "batidor" || section === "batidor-summary") {
    return `Comida +${formatDecimal(foodDelta)}`;
  }
  if (section === "cook") {
    return `Comida +${formatDecimal(foodDelta)}`;
  }
  if (section === "cook-summary") {
    return `Trabajo en equipo`;
  }
  if (section === "food") {
    return `Comida +${formatDecimal(foodDelta)}`;
  }
  if (section === "leftover") {
    return `Perecederos +${formatDecimal(foodDelta)}`;
  }
  if (section === "cargo" && lowerTitle.includes("suministros reasignados")) {
    return `Unidades +1`;
  }
  if (section === "cargo" && lowerTitle.includes("pereced")) {
    return `Perecederos +${formatDecimal(foodDelta)}`;
  }
  if (section === "cargo") {
    return `Comida +${formatDecimal(foodDelta)}`;
  }
  return `+${formatDecimal(foodDelta)}`;
}

function normalizeDayCycleDetails(title: string, section: string, details: string[]) {
  const normalized = details
    .map((detail) => detail.trim())
    .filter((detail) => detail.length > 0)
    .map((detail) => {
      if (detail === title || detail === `Cocinero: ${title}`) {
        return null;
      }
      if (detail === "Cocina portátil aplicada.") {
        return "Cocina portátil aplicada: +100%";
      }
      if (detail === "Sin cocina portátil.") {
        return "Cocina portátil no usada";
      }
      if (section === "cook" && (detail.startsWith("Comida obtenida por esta unidad") || detail.startsWith("Comida final"))) {
        return null;
      }
      if (section === "food" && detail.startsWith("Comida generada")) {
        return null;
      }
      if (section === "cargo" && detail.startsWith("Comida:")) {
        return null;
      }
      if (detail.startsWith("Cocinero: ") && section === "cook") {
        return null;
      }
      return detail;
    })
    .filter((detail): detail is string => detail !== null);

  return Array.from(new Set(normalized));
}

async function openDayCycleModal() {
  if (!selectedCaravan.value) {
    return;
  }

  dayCycleTimelineTab.value = "agricultors";
  dayCycleModalOpen.value = true;
  dayCycleLoading.value = true;
  dayCyclePreview.value = null;
  error.value = null;

  try {
    dayCyclePreview.value = await previewCaravanDayCycle(selectedCaravan.value.id);
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "No se pudo generar la previsualización del día";
    dayCycleModalOpen.value = false;
  } finally {
    dayCycleLoading.value = false;
  }
}

function closeDayCycleModal() {
  if (dayCycleSubmitting.value) {
    return;
  }

  dayCycleModalOpen.value = false;
  dayCyclePreview.value = null;
}

async function confirmDayCycle() {
  if (!selectedCaravan.value || !dayCyclePreview.value) {
    return;
  }

  dayCycleSubmitting.value = true;
  pendingAction.value = "day-cycle-confirm";
  error.value = null;

  try {
    const confirmed = await confirmCaravanDayCycle(
      selectedCaravan.value.id,
      dayCyclePreview.value.previewFingerprint,
    );
    dayCyclePreview.value = confirmed;
    await refresh();
    closeDayCycleModal();
    showToast(`Día pasado en ${selectedCaravan.value.name}.`);
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "No se pudo confirmar el paso del día";
  } finally {
    dayCycleSubmitting.value = false;
    pendingAction.value = null;
  }
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
          <button
            class="secondary-button"
            type="button"
            :disabled="loading || submitting || !selectedCaravan || dayCycleLoading || dayCycleSubmitting"
            @click="openDayCycleModal"
          >
            Pasar el día
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

      <div v-if="dayCycleModalOpen" class="modal-backdrop" @click.self="closeDayCycleModal">
        <div class="modal modal-cycle">
          <div class="modal-header">
            <div>
              <p class="eyebrow">Ciclo diario</p>
              <h2>Pasar el día</h2>
            </div>
            <button class="ghost-button" type="button" :disabled="dayCycleSubmitting" @click="closeDayCycleModal">
              Cerrar
            </button>
          </div>

          <div v-if="dayCycleLoading" class="muted">Generando simulación…</div>

          <template v-else-if="dayCyclePreview">
            <section class="day-cycle-summary" :class="{ danger: !dayCyclePreview.consumptionCovered }">
              <div class="day-cycle-summary__copy">
                <p class="eyebrow">Resultado de la jornada</p>
                <h3>{{ dayCyclePreview.consumptionCovered ? "Consumo cubierto" : "Consumo no cubierto" }}</h3>
                <p class="day-cycle-summary__text">
                  La caravana necesita {{ formatDecimal(dayCyclePreview.requiredConsumption) }} de comida y ha
                  generado {{ formatDecimal(dayCyclePreview.generatedFood) }}.
                </p>
                <p class="day-cycle-summary__text day-cycle-summary__text--subtle">
                  Total generado = comida de batidores + comida ya contenida en perecederos + comida de cocineros.
                </p>
              </div>

              <div class="day-cycle-summary__chips">
                <span class="day-cycle-chip">{{ dayCyclePreview.consumptionCovered ? "OK" : "ALERTA" }}</span>
                <span class="day-cycle-chip">Sobrante {{ formatDecimal(dayCyclePreview.leftoverFood) }}</span>
                <span class="day-cycle-chip">Suministros usados {{ dayCyclePreview.suppliesConsumed }}</span>
              </div>
            </section>

            <section class="day-cycle-layout">
              <article class="day-cycle-panel">
                <header class="day-cycle-panel__header">
                  <div>
                    <p class="eyebrow">Antes del cálculo</p>
                    <h4>Inventario inicial</h4>
                  </div>
                </header>
                <dl class="day-cycle-metrics">
                  <div>
                    <dt>Suministros</dt>
                    <dd>{{ dayCyclePreview.currentSupplyUnits }}</dd>
                  </div>
                  <div>
                    <dt>Perecederos</dt>
                    <dd>{{ dayCyclePreview.currentPerishableUnits }}</dd>
                  </div>
                  <div>
                    <dt>Comida perecedera</dt>
                    <dd>{{ formatDecimal(dayCyclePreview.currentPerishableFood) }}</dd>
                  </div>
                </dl>
              </article>

              <article class="day-cycle-panel">
                <header class="day-cycle-panel__header">
                  <div>
                    <p class="eyebrow">Después del cálculo</p>
                    <h4>Inventario final</h4>
                  </div>
                </header>
                <dl class="day-cycle-metrics">
                  <div>
                    <dt>Suministros</dt>
                    <dd>{{ dayCyclePreview.finalSupplyUnits }}</dd>
                  </div>
                  <div>
                    <dt>Perecederos</dt>
                    <dd>{{ dayCyclePreview.finalPerishableUnits }}</dd>
                  </div>
                  <div>
                    <dt>Comida perecedera</dt>
                    <dd>{{ formatDecimal(dayCyclePreview.finalPerishableFood) }}</dd>
                  </div>
                </dl>
              </article>

              <article class="day-cycle-panel">
                <header class="day-cycle-panel__header">
                  <div>
                    <p class="eyebrow">Producción previa</p>
                    <h4>Recursos generados</h4>
                  </div>
                </header>
                <dl class="day-cycle-metrics">
                  <div>
                    <dt>Agricultores</dt>
                    <dd>{{ dayCyclePreview.generatedSuppliesFromAgricultors }}</dd>
                  </div>
                  <div>
                    <dt>Boticarios</dt>
                    <dd>{{ formatDecimal(dayCyclePreview.generatedAlchemyValueFromBoticarios) }}</dd>
                  </div>
                  <div>
                    <dt>Comida total</dt>
                    <dd>{{ formatDecimal(dayCyclePreview.generatedFood) }}</dd>
                  </div>
                </dl>
              </article>
            </section>

            <section class="day-cycle-timeline">
              <header class="day-cycle-timeline__header">
                <div>
                  <p class="eyebrow">Simulación</p>
                  <h4>Cómo se resolvió el día</h4>
                </div>
                <p class="muted">Agrupamos cada bloque para que el flujo se lea de un vistazo.</p>
              </header>

              <div class="day-cycle-tabs" role="tablist" aria-label="Fases de la simulación">
                <button
                  v-for="tab in dayCycleTimelineTabs"
                  :key="tab.key"
                  type="button"
                  class="day-cycle-tab"
                  :class="{ active: activeDayCycleTimelineTab === tab.key }"
                  :aria-selected="activeDayCycleTimelineTab === tab.key"
                  :tabindex="activeDayCycleTimelineTab === tab.key ? 0 : -1"
                  role="tab"
                  @click="dayCycleTimelineTab = tab.key"
                >
                  {{ tab.label }}
                </button>
              </div>

              <div class="day-cycle-timeline__groups">
                <section
                  v-for="group in visibleDayCycleTimelineSections"
                  :key="group.key"
                  class="day-cycle-group"
                  :class="`day-cycle-group--${group.key}`"
                >
                  <header class="day-cycle-group__header">
                    <div>
                      <p class="day-cycle-step__section">{{ group.label }}</p>
                      <h5>{{ dayCycleGroupTitle(group.key) }}</h5>
                    </div>
                    <p class="muted">{{ dayCycleGroupHint(group.key) }}</p>
                  </header>

                  <div class="day-cycle-group__cards">
                    <article
                      v-for="entry in group.cards"
                      :key="entry.key"
                      class="day-cycle-step"
                      :class="[
                        `day-cycle-step--${entry.tone}`,
                        { 'day-cycle-step--summary': entry.isSummary },
                      ]"
                    >
                      <div class="day-cycle-step__header">
                        <div>
                          <p class="day-cycle-step__section">{{ entry.sectionLabel }}</p>
                          <h5>{{ entry.title }}</h5>
                        </div>
                        <span class="day-cycle-step__delta" :class="`day-cycle-step__delta--${dayCycleDeltaTone(entry.resultLabel)}`">
                          {{ entry.resultLabel }}
                        </span>
                      </div>

                      <ul v-if="entry.details.length" class="day-cycle-step__details">
                        <li v-for="detail in entry.details" :key="detail">{{ detail }}</li>
                      </ul>
                    </article>
                  </div>
                </section>
              </div>
            </section>

            <section v-if="dayCyclePreview.warnings.length" class="warning-banner danger">
              <strong>Avisos</strong>
              <ul class="simple-list">
                <li v-for="warning in dayCyclePreview.warnings" :key="warning">{{ warning }}</li>
              </ul>
            </section>

            <div class="modal-actions">
              <button class="secondary-button" type="button" :disabled="dayCycleSubmitting" @click="closeDayCycleModal">
                Cancelar
              </button>
              <button class="primary-button" type="button" :disabled="dayCycleSubmitting" @click="confirmDayCycle">
                <span class="button-with-spinner">
                  <span v-if="dayCycleSubmitting" class="button-spinner" aria-hidden="true"></span>
                  <span>{{ dayCycleSubmitting ? "Confirmando…" : "Confirmar paso del día" }}</span>
                </span>
              </button>
            </div>
          </template>
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

.day-cycle-summary {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
  padding: 1rem 1.1rem;
  border-radius: 1rem;
  border: 1px solid rgba(37, 99, 235, 0.18);
  background:
    linear-gradient(180deg, rgba(239, 246, 255, 0.95), rgba(255, 255, 255, 0.92)),
    radial-gradient(circle at top right, rgba(37, 99, 235, 0.14), transparent 35%);
}

.day-cycle-summary.danger {
  border-color: rgba(220, 38, 38, 0.22);
  background:
    linear-gradient(180deg, rgba(254, 242, 242, 0.98), rgba(255, 255, 255, 0.94)),
    radial-gradient(circle at top right, rgba(220, 38, 38, 0.12), transparent 35%);
}

.day-cycle-summary__copy {
  display: grid;
  gap: 0.35rem;
  min-width: 0;
}

.day-cycle-summary h3,
.day-cycle-panel__header h4,
.day-cycle-timeline__header h4,
.day-cycle-step h5 {
  margin: 0;
  color: #0f172a;
}

.day-cycle-summary h3 {
  font-size: 1.35rem;
  line-height: 1.1;
}

.day-cycle-summary__text {
  margin: 0;
  color: #334155;
  max-width: 56rem;
}

.day-cycle-summary__text--subtle {
  color: #64748b;
  font-size: 0.92rem;
}

.day-cycle-summary__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  justify-content: flex-end;
}

.day-cycle-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.45rem 0.7rem;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(148, 163, 184, 0.28);
  font-size: 0.85rem;
  font-weight: 700;
  color: #0f172a;
}

.day-cycle-layout {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.75rem;
}

.day-cycle-panel {
  display: grid;
  gap: 0.75rem;
  padding: 1rem;
  border-radius: 1rem;
  border: 1px solid rgba(148, 163, 184, 0.22);
  background: #fff;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.day-cycle-panel__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.5rem;
}

.day-cycle-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.65rem;
  margin: 0;
}

.day-cycle-metrics div {
  padding: 0.75rem;
  border-radius: 0.85rem;
  background: #f8fafc;
}

.day-cycle-metrics dt {
  font-size: 0.78rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: #64748b;
}

.day-cycle-metrics dd {
  margin: 0.25rem 0 0;
  font-size: 1.15rem;
  font-weight: 800;
  color: #0f172a;
}

.day-cycle-timeline {
  display: grid;
  gap: 0.85rem;
}

.day-cycle-timeline__header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-end;
}

.day-cycle-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  padding: 0.25rem;
  border-radius: 1rem;
  background: rgba(241, 245, 249, 0.9);
  border: 1px solid rgba(148, 163, 184, 0.18);
}

.day-cycle-tab {
  appearance: none;
  border: 1px solid transparent;
  background: transparent;
  color: #475569;
  border-radius: 999px;
  padding: 0.55rem 0.9rem;
  font-weight: 700;
  font-size: 0.92rem;
  cursor: pointer;
  transition: background-color 0.18s ease, color 0.18s ease, border-color 0.18s ease, transform 0.18s ease;
}

.day-cycle-tab:hover {
  background: rgba(255, 255, 255, 0.85);
  color: #0f172a;
}

.day-cycle-tab.active {
  background: #0f172a;
  color: #fff;
  border-color: #0f172a;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.14);
}

.day-cycle-tab:focus-visible {
  outline: 2px solid #2563eb;
  outline-offset: 2px;
}

.day-cycle-timeline__list {
  display: grid;
  gap: 0.75rem;
}

.day-cycle-group {
  display: grid;
  gap: 0.75rem;
  padding: 0.85rem;
  border-radius: 1rem;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: rgba(248, 250, 252, 0.8);
}

.day-cycle-group__header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-end;
}

.day-cycle-group__cards {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
}

.day-cycle-step--summary {
  grid-column: 1 / -1;
  border-style: dashed;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 250, 252, 0.98));
}

.day-cycle-step {
  display: grid;
  gap: 0.7rem;
  padding: 1rem 1.05rem;
  border-radius: 1rem;
  border: 1px solid rgba(148, 163, 184, 0.22);
  background: #fff;
  position: relative;
  overflow: hidden;
}

.day-cycle-step::before {
  content: "";
  position: absolute;
  inset: 0 auto 0 0;
  width: 0.35rem;
  background: #cbd5e1;
}

.day-cycle-step--success::before {
  background: #16a34a;
}

.day-cycle-step--warning::before {
  background: #d97706;
}

.day-cycle-step--info::before {
  background: #2563eb;
}

.day-cycle-step__header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
}

.day-cycle-step__section {
  margin: 0 0 0.2rem;
  font-size: 0.78rem;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: #64748b;
}

.day-cycle-step__delta {
  display: inline-flex;
  align-items: center;
  flex-shrink: 0;
  border-radius: 999px;
  padding: 0.35rem 0.65rem;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 0.84rem;
  font-weight: 800;
}

.day-cycle-step__delta--supplies {
  background: #dcfce7;
  color: #166534;
}

.day-cycle-step__delta--food {
  background: #dbeafe;
  color: #1d4ed8;
}

.day-cycle-step__delta--alchemy {
  background: #fce7f3;
  color: #9d174d;
}

.day-cycle-step__delta--other {
  background: #f3f4f6;
  color: #374151;
}

.day-cycle-step--success .day-cycle-step__delta {
  background: #dcfce7;
  color: #166534;
}

.day-cycle-step--warning .day-cycle-step__delta {
  background: #fef3c7;
  color: #92400e;
}

.day-cycle-step--info .day-cycle-step__delta {
  background: #dbeafe;
  color: #1d4ed8;
}

.day-cycle-step__details {
  margin: 0;
  padding-left: 1.1rem;
  color: #334155;
  display: grid;
  gap: 0.25rem;
}

.day-cycle-step__details li {
  line-height: 1.35;
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

.simple-list {
  margin: 0.5rem 0 0;
  padding-left: 1.25rem;
}

@media (max-width: 900px) {
  .grid,
  .stats,
  .sections {
    grid-template-columns: 1fr;
  }

  .hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .day-cycle-summary,
  .day-cycle-timeline__header,
  .day-cycle-step__header {
    flex-direction: column;
    align-items: flex-start;
  }

  .day-cycle-layout,
  .day-cycle-metrics {
    grid-template-columns: 1fr;
  }

  .day-cycle-group__cards {
    grid-template-columns: 1fr;
  }
}
</style>
