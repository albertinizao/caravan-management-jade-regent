<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { RouterLink } from "vue-router";

import { getActiveCaravan } from "@/services/caravans";
import { listCaravanTravelers } from "@/services/travelers";
import { listCaravanWagons } from "@/services/wagons";
import {
  addCaravanBeast,
  addCaravanBeastFromCatalog,
  clearCaravanBeastAssignment,
  getCaravanBeast,
  listBeastCatalog,
  listCaravanBeasts,
  updateCaravanBeastAssignment,
} from "@/services/beasts";
import type { Caravan } from "@/types/caravan";
import type { CaravanTraveler } from "@/types/traveler";
import type { CaravanWagon } from "@/types/wagon";
import type {
  AddCaravanBeastPayload,
  BeastAssignmentType,
  BeastCatalogItem,
  CaravanBeast,
} from "@/types/beast";

interface DraftRequirement {
  maxLargeBeasts: number;
  maxMediumBeasts: number;
  minimumStrength: number;
}

const activeCaravan = ref<Caravan | null>(null);
const beasts = ref<CaravanBeast[]>([]);
const catalog = ref<BeastCatalogItem[]>([]);
const wagons = ref<CaravanWagon[]>([]);
const travelers = ref<CaravanTraveler[]>([]);
const loading = ref(true);
const submitting = ref(false);
const error = ref<string | null>(null);
const search = ref("");
const sourceFilter = ref<"all" | "CATALOG" | "CUSTOM">("all");
const assignmentFilter = ref<"all" | BeastAssignmentType>("all");
const wagonFilter = ref("all");
const selectedBeast = ref<CaravanBeast | null>(null);
const selectedDraftWagonId = ref("");
const selectedTravelerWagonId = ref("");
const selectedBeastError = ref<string | null>(null);
const catalogModalOpen = ref(false);
const customModalOpen = ref(false);
const catalogModalError = ref<string | null>(null);
const customModalError = ref<string | null>(null);
const customName = ref("");
const customSize = ref("M");
const customStrength = ref("2");
const customSpeed = ref("40");
const customThermalAdaptation = ref("");
const customBasePrice = ref("");
const customTrainedPrice = ref("");
const customFourLegged = ref(true);
const customSpecialNote = ref("Ninguno");
const customDescription = ref("");
const customNotes = ref("");

const catalogByCode = computed(() =>
  Object.fromEntries(catalog.value.map((item) => [item.code, item] as const)),
);

const wagonById = computed(() =>
  Object.fromEntries(wagons.value.map((wagon) => [wagon.id, wagon] as const)),
);

const travelersByWagonId = computed(() => {
  const grouped = new Map<string, number>();
  for (const traveler of travelers.value) {
    if (!traveler.wagonId) {
      continue;
    }
    grouped.set(traveler.wagonId, (grouped.get(traveler.wagonId) ?? 0) + 1);
  }
  return grouped;
});

const draftBeastsByWagonId = computed(() => {
  const grouped = new Map<string, CaravanBeast[]>();
  for (const beast of beasts.value.filter((item) => item.assignmentType === "DRAFT" && item.assignedWagonId)) {
    const wagonId = beast.assignedWagonId as string;
    grouped.set(wagonId, [...(grouped.get(wagonId) ?? []), beast]);
  }
  return grouped;
});

const visibleBeasts = computed(() => {
  const query = search.value.trim().toLowerCase();
  return beasts.value
    .filter((beast) => (query ? beast.name.toLowerCase().includes(query) : true))
    .filter((beast) => sourceFilter.value === "all" || beast.sourceType === sourceFilter.value)
    .filter((beast) => assignmentFilter.value === "all" || beast.assignmentType === assignmentFilter.value)
    .filter((beast) => wagonFilter.value === "all" || beast.assignedWagonId === wagonFilter.value);
});

const selectedBeastCatalogItem = computed(() =>
  selectedBeast.value?.catalogBeastCode ? catalogByCode.value[selectedBeast.value.catalogBeastCode] ?? null : null,
);

const selectedDraftRequirement = computed(() =>
  selectedDraftWagonId.value ? parseDraftRequirement(wagonById.value[selectedDraftWagonId.value]?.propulsion ?? "") : null,
);

const selectedTravelerWagon = computed(() =>
  selectedTravelerWagonId.value ? wagonById.value[selectedTravelerWagonId.value] ?? null : null,
);

async function refresh() {
  loading.value = true;
  error.value = null;

  try {
    const activeResponse = await getActiveCaravan();
    activeCaravan.value = activeResponse.caravan;

    if (activeResponse.caravan) {
      const [beastList, catalogList, wagonList, travelerList] = await Promise.all([
        listCaravanBeasts(activeResponse.caravan.id),
        listBeastCatalog(activeResponse.caravan.id),
        listCaravanWagons(activeResponse.caravan.id),
        listCaravanTravelers(activeResponse.caravan.id),
      ]);

      beasts.value = beastList;
      catalog.value = catalogList;
      wagons.value = wagonList;
      travelers.value = travelerList;

      if (selectedBeast.value) {
        const refreshed = beastList.find((beast) => beast.id === selectedBeast.value?.id);
        if (refreshed) {
          selectedBeast.value = refreshed;
          selectedDraftWagonId.value = refreshed.assignedWagonId ?? wagonList[0]?.id ?? "";
          selectedTravelerWagonId.value = refreshed.assignedWagonId ?? wagonList[0]?.id ?? "";
        }
      }
    } else {
      beasts.value = [];
      catalog.value = [];
      wagons.value = [];
      travelers.value = [];
      selectedBeast.value = null;
      selectedDraftWagonId.value = "";
      selectedTravelerWagonId.value = "";
    }
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "Failed to load beasts";
  } finally {
    loading.value = false;
  }
}

function openCatalogModal() {
  catalogModalError.value = null;
  catalogModalOpen.value = true;
}

function closeCatalogModal() {
  catalogModalOpen.value = false;
  catalogModalError.value = null;
}

function openCustomModal() {
  customModalError.value = null;
  resetCustomForm();
  customModalOpen.value = true;
}

function closeCustomModal() {
  customModalOpen.value = false;
  customModalError.value = null;
}

function resetCustomForm() {
  customName.value = "";
  customSize.value = "M";
  customStrength.value = "2";
  customSpeed.value = "40";
  customThermalAdaptation.value = "";
  customBasePrice.value = "";
  customTrainedPrice.value = "";
  customFourLegged.value = true;
  customSpecialNote.value = "Ninguno";
  customDescription.value = "";
  customNotes.value = "";
}

async function handleAddCatalogBeast(beastCode: string) {
  if (!activeCaravan.value) {
    return;
  }

  submitting.value = true;
  catalogModalError.value = null;

  try {
    const created = await addCaravanBeastFromCatalog(activeCaravan.value.id, beastCode);
    selectedBeast.value = created;
    selectedDraftWagonId.value = created.assignedWagonId ?? wagons.value[0]?.id ?? "";
    selectedTravelerWagonId.value = created.assignedWagonId ?? wagons.value[0]?.id ?? "";
    closeCatalogModal();
    await refresh();
  } catch (cause) {
    catalogModalError.value = cause instanceof Error ? cause.message : "Failed to add beast";
  } finally {
    submitting.value = false;
  }
}

async function handleCreateCustomBeast() {
  if (!activeCaravan.value) {
    return;
  }

  if (!normalizeInputValue(customName.value)) {
    customModalError.value = "El nombre es obligatorio";
    return;
  }

  if (!normalizeInputValue(customSize.value)) {
    customModalError.value = "El tamaño es obligatorio";
    return;
  }

  const parsedStrength = Number(customStrength.value);
  const parsedSpeed = Number(customSpeed.value);
  const thermalInput = normalizeInputValue(customThermalAdaptation.value);
  const basePriceInput = normalizeInputValue(customBasePrice.value);
  const trainedPriceInput = normalizeInputValue(customTrainedPrice.value);
  const parsedThermal = thermalInput === "" ? null : Number(thermalInput);
  const parsedBasePrice = basePriceInput === "" ? null : Number(basePriceInput);
  const parsedTrainedPrice = trainedPriceInput === "" ? null : Number(trainedPriceInput);

  if (Number.isNaN(parsedStrength) || parsedStrength < 0) {
    customModalError.value = "La fuerza debe ser un número válido";
    return;
  }
  if (Number.isNaN(parsedSpeed) || parsedSpeed < 0) {
    customModalError.value = "La velocidad debe ser un número válido";
    return;
  }
  if (parsedThermal !== null && Number.isNaN(parsedThermal)) {
    customModalError.value = "La adaptación térmica debe ser un número válido";
    return;
  }
  if (parsedBasePrice !== null && Number.isNaN(parsedBasePrice)) {
    customModalError.value = "El precio base debe ser un número válido";
    return;
  }
  if (parsedTrainedPrice !== null && Number.isNaN(parsedTrainedPrice)) {
    customModalError.value = "El precio adiestrado debe ser un número válido";
    return;
  }

  submitting.value = true;
  customModalError.value = null;

  const payload: AddCaravanBeastPayload = {
    sourceType: "CUSTOM",
    name: normalizeInputValue(customName.value),
    size: normalizeInputValue(customSize.value),
    strength: parsedStrength,
    speed: parsedSpeed,
    thermalAdaptation: parsedThermal,
    basePrice: parsedBasePrice,
    trainedPrice: parsedTrainedPrice,
    fourLegged: customFourLegged.value,
    specialNote: normalizeInputValue(customSpecialNote.value) || "Ninguno",
    description: normalizeInputValue(customDescription.value),
    customNotes: normalizeInputValue(customNotes.value) || null,
  };

  try {
    const created = await addCaravanBeast(activeCaravan.value.id, payload);
    selectedBeast.value = created;
    selectedDraftWagonId.value = created.assignedWagonId ?? wagons.value[0]?.id ?? "";
    selectedTravelerWagonId.value = created.assignedWagonId ?? wagons.value[0]?.id ?? "";
    closeCustomModal();
    await refresh();
  } catch (cause) {
    customModalError.value = cause instanceof Error ? cause.message : "Failed to create beast";
  } finally {
    submitting.value = false;
  }
}

async function openBeastDetails(beast: CaravanBeast) {
  if (!activeCaravan.value) {
    return;
  }

  try {
    const detailed = await getCaravanBeast(activeCaravan.value.id, beast.id);
    selectedBeast.value = detailed;
    selectedDraftWagonId.value = detailed.assignedWagonId ?? wagons.value[0]?.id ?? "";
    selectedTravelerWagonId.value = detailed.assignedWagonId ?? wagons.value[0]?.id ?? "";
    selectedBeastError.value = null;
  } catch (cause) {
    selectedBeastError.value = cause instanceof Error ? cause.message : "Failed to load beast details";
  }
}

function closeBeastDetails() {
  selectedBeast.value = null;
  selectedDraftWagonId.value = "";
  selectedTravelerWagonId.value = "";
  selectedBeastError.value = null;
}

async function assignAsDraft() {
  if (!activeCaravan.value || !selectedBeast.value || !selectedDraftWagonId.value) {
    return;
  }

  submitting.value = true;
  selectedBeastError.value = null;

  try {
    const updated = await updateCaravanBeastAssignment(activeCaravan.value.id, selectedBeast.value.id, {
      assignmentType: "DRAFT",
      wagonId: selectedDraftWagonId.value,
    });
    selectedBeast.value = updated;
    await refresh();
    closeBeastDetails();
  } catch (cause) {
    selectedBeastError.value = cause instanceof Error ? cause.message : "Failed to assign beast as draft";
  } finally {
    submitting.value = false;
  }
}

async function assignAsTraveler() {
  if (!activeCaravan.value || !selectedBeast.value || !selectedTravelerWagonId.value) {
    return;
  }

  submitting.value = true;
  selectedBeastError.value = null;

  try {
    const updated = await updateCaravanBeastAssignment(activeCaravan.value.id, selectedBeast.value.id, {
      assignmentType: "TRAVELER",
      wagonId: selectedTravelerWagonId.value,
    });
    selectedBeast.value = updated;
    await refresh();
    closeBeastDetails();
  } catch (cause) {
    selectedBeastError.value = cause instanceof Error ? cause.message : "Failed to assign beast as traveler";
  } finally {
    submitting.value = false;
  }
}

async function clearAssignment() {
  if (!activeCaravan.value || !selectedBeast.value) {
    return;
  }

  submitting.value = true;
  selectedBeastError.value = null;

  try {
    const updated = await clearCaravanBeastAssignment(activeCaravan.value.id, selectedBeast.value.id);
    selectedBeast.value = updated;
    await refresh();
  } catch (cause) {
    selectedBeastError.value = cause instanceof Error ? cause.message : "Failed to clear assignment";
  } finally {
    submitting.value = false;
  }
}

function draftLabel(wagon: CaravanWagon): string {
  const requirement = parseDraftRequirement(wagon.propulsion);
  if (!requirement) {
    return wagon.propulsion;
  }

  const used = draftOccupancyForWagon(wagon.id);
  const remaining = Math.max(0, requirement.maxMediumBeasts - used.mediumSlots);
  return `${used.mediumSlots}/${requirement.maxMediumBeasts} slots, ${remaining} libres`;
}

function draftOccupancyForWagon(wagonId: string) {
  const assigned = draftBeastsByWagonId.value.get(wagonId) ?? [];
  const largeCount = assigned.filter((beast) => beast.size.toUpperCase() === "G").length;
  const mediumCount = assigned.filter((beast) => beast.size.toUpperCase() === "M").length;
  return {
    largeCount,
    mediumCount,
    mediumSlots: largeCount * 4 + mediumCount,
  };
}

function travelerOccupancyForWagon(wagonId: string) {
  const travelerCount = travelersByWagonId.value.get(wagonId) ?? 0;
  const beastCount = beasts.value.filter(
    (beast) => beast.assignmentType === "TRAVELER" && beast.assignedWagonId === wagonId,
  ).length;
  return travelerCount + beastCount;
}

function parseDraftRequirement(propulsion: string): DraftRequirement | null {
  const match = propulsion.match(/(\d+)\s+criatura[s]?\s+grande[s]?\s*\/\s*(\d+)\s+mediana[s]?\s*\(\+(\d+)\s+fuerza\)/i);
  if (!match) {
    return null;
  }

  return {
    maxLargeBeasts: Number(match[1]),
    maxMediumBeasts: Number(match[2]),
    minimumStrength: Number(match[3]),
  };
}

function currentAssignmentLabel(beast: CaravanBeast): string {
  if (beast.assignmentType === "NONE") {
    return "Sin asignar";
  }

  if (beast.assignmentType === "DRAFT") {
    return `Tiro · ${beast.assignedWagonName ?? beast.assignedWagonId ?? "—"}`;
  }

  return `Viajero · ${beast.assignedWagonName ?? beast.assignedWagonId ?? "—"}`;
}

function beastTypeLabel(beast: CaravanBeast): string {
  return beast.sourceType === "CATALOG" ? "Catálogo" : "Personalizada";
}

function beastPriceLabel(value: number | null) {
  return value === null ? "—" : `${value} po`;
}

function beastThermalLabel(value: number | null) {
  return value === null ? "—" : String(value);
}

function beastSizeLabel(size: string) {
  return size === "G" ? "Grande" : size === "M" ? "Mediana" : size === "E" ? "Enorme" : size;
}

function normalizeInputValue(value: string | number | null | undefined): string {
  if (value === null || value === undefined) {
    return "";
  }
  return String(value).trim();
}

onMounted(refresh);
</script>

<template>
  <main class="page">
    <section class="shell">
      <header class="hero">
        <div>
          <p class="eyebrow">Caravana activa</p>
          <h1>Bestias de carga</h1>
          <p class="subtitle">Gestiona bestias de catálogo, bestias personalizadas y su asignación a los carros.</p>
        </div>
        <div class="hero-actions">
          <button class="ghost-button" type="button" @click="refresh">Refrescar</button>
          <button class="primary-button" type="button" :disabled="!activeCaravan || submitting" @click="openCatalogModal">
            Añadir del catálogo
          </button>
          <button class="secondary-button" type="button" :disabled="!activeCaravan || submitting" @click="openCustomModal">
            Crear personalizada
          </button>
        </div>
      </header>

      <p v-if="error" class="error">{{ error }}</p>

      <section v-if="!activeCaravan" class="card empty-state">
        <h2>No hay caravana activa</h2>
        <p class="muted">Selecciona o crea una caravana para gestionar bestias.</p>
        <RouterLink class="primary-link" to="/">Ir a caravanas</RouterLink>
      </section>

      <template v-else>
        <section class="card">
          <div class="section-header">
            <div>
              <h2>{{ activeCaravan.name }}</h2>
              <p v-if="activeCaravan.description" class="muted">{{ activeCaravan.description }}</p>
            </div>
            <div class="stats-inline">
              <div><span>Bestias</span><strong>{{ beasts.length }}</strong></div>
              <div><span>Carros</span><strong>{{ wagons.length }}</strong></div>
              <div><span>Viajeros</span><strong>{{ travelers.length }}</strong></div>
            </div>
          </div>

          <div class="filters">
            <label>
              <span>Buscar</span>
              <input v-model="search" type="search" placeholder="Buscar por nombre" />
            </label>

            <label>
              <span>Origen</span>
              <select v-model="sourceFilter">
                <option value="all">Todos</option>
                <option value="CATALOG">Catálogo</option>
                <option value="CUSTOM">Personalizadas</option>
              </select>
            </label>

            <label>
              <span>Asignación</span>
              <select v-model="assignmentFilter">
                <option value="all">Todas</option>
                <option value="NONE">Sin asignar</option>
                <option value="DRAFT">Tiro</option>
                <option value="TRAVELER">Viajero</option>
              </select>
            </label>

            <label>
              <span>Carro</span>
              <select v-model="wagonFilter">
                <option value="all">Todos</option>
                <option v-for="wagon in wagons" :key="wagon.id" :value="wagon.id">
                  {{ wagon.name }}
                </option>
              </select>
            </label>
          </div>

          <div v-if="loading" class="muted">Cargando bestias…</div>
          <div v-else-if="visibleBeasts.length === 0" class="empty-state-inline">
            <p>No hay bestias que coincidan con los filtros.</p>
          </div>
          <div v-else class="table-wrap">
            <table class="beast-table">
              <thead>
                <tr>
                  <th>Nombre</th>
                  <th>Origen</th>
                  <th>Tamaño</th>
                  <th>Fuerza</th>
                  <th>Velocidad</th>
                  <th>Asignación</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="beast in visibleBeasts"
                  :key="beast.id"
                  tabindex="0"
                  @click="openBeastDetails(beast)"
                  @keyup.enter="openBeastDetails(beast)"
                >
                  <td>
                    <strong>{{ beast.name }}</strong>
                    <p class="muted">{{ beast.specialNote }}</p>
                  </td>
                  <td>{{ beastTypeLabel(beast) }}</td>
                  <td>{{ beastSizeLabel(beast.size) }}</td>
                  <td>{{ beast.strength }}</td>
                  <td>{{ beast.speed }}</td>
                  <td>{{ currentAssignmentLabel(beast) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
      </template>
    </section>

    <teleport to="body">
      <div v-if="catalogModalOpen" class="modal-backdrop" @click.self="closeCatalogModal">
        <div class="modal">
          <div class="modal-header">
            <div>
              <p class="eyebrow">Añadir bestia</p>
              <h2>Catálogo de bestias</h2>
            </div>
            <button class="ghost-button" type="button" @click="closeCatalogModal">Cerrar</button>
          </div>

          <p v-if="catalogModalError" class="error">{{ catalogModalError }}</p>

          <div class="catalog-grid">
            <article v-for="item in catalog" :key="item.code" class="catalog-card">
              <div class="catalog-header">
                <div>
                  <h3>{{ item.name }}</h3>
                  <p class="muted">{{ item.code }}</p>
                </div>
                <button class="primary-button" type="button" :disabled="submitting" @click="handleAddCatalogBeast(item.code)">
                  Añadir
                </button>
              </div>

              <dl class="stats">
                <div><dt>Precio</dt><dd>{{ beastPriceLabel(item.basePrice) }}</dd></div>
                <div><dt>Adiestrado</dt><dd>{{ beastPriceLabel(item.trainedPrice) }}</dd></div>
                <div><dt>Tamaño</dt><dd>{{ beastSizeLabel(item.size) }}</dd></div>
                <div><dt>Fuerza</dt><dd>{{ item.strength }}</dd></div>
                <div><dt>Velocidad</dt><dd>{{ item.speed }}</dd></div>
                <div><dt>Adaptación</dt><dd>{{ beastThermalLabel(item.thermalAdaptation) }}</dd></div>
              </dl>

              <p class="muted"><strong>Nota:</strong> {{ item.specialNote }}</p>
              <p>{{ item.description }}</p>
            </article>
          </div>
        </div>
      </div>
    </teleport>

    <teleport to="body">
      <div v-if="customModalOpen" class="modal-backdrop" @click.self="closeCustomModal">
        <div class="modal modal-create">
          <div class="modal-header">
            <div>
              <p class="eyebrow">Crear bestia</p>
              <h2>Bestia personalizada</h2>
            </div>
            <button class="ghost-button" type="button" @click="closeCustomModal">Cerrar</button>
          </div>

          <p v-if="customModalError" class="error">{{ customModalError }}</p>

          <div class="create-form">
            <div class="two-columns">
              <label>
                <span>Nombre</span>
                <input v-model="customName" type="text" placeholder="Nombre de la bestia" />
              </label>
              <label>
                <span>Tamaño</span>
                <select v-model="customSize">
                  <option value="M">Mediana</option>
                  <option value="G">Grande</option>
                  <option value="E">Enorme</option>
                </select>
              </label>
            </div>

            <div class="two-columns">
              <label>
                <span>Fuerza</span>
                <input v-model="customStrength" type="number" min="0" />
              </label>
              <label>
                <span>Velocidad</span>
                <input v-model="customSpeed" type="number" min="0" />
              </label>
            </div>

            <div class="two-columns">
              <label>
                <span>Adaptación térmica</span>
                <input v-model="customThermalAdaptation" type="number" step="1" />
              </label>
              <label>
                <span>Precio base</span>
                <input v-model="customBasePrice" type="number" min="0" step="1" placeholder="Opcional" />
              </label>
            </div>

            <div class="two-columns">
              <label>
                <span>Precio adiestrado</span>
                <input v-model="customTrainedPrice" type="number" min="0" step="1" placeholder="Opcional" />
              </label>
              <label class="checkbox-row">
                <input v-model="customFourLegged" type="checkbox" />
                <span>Cuenta como criatura de cuatro patas para el tiro</span>
              </label>
            </div>

            <label>
              <span>Nota especial</span>
              <input v-model="customSpecialNote" type="text" placeholder="Ninguno" />
            </label>

            <label>
              <span>Descripción</span>
              <textarea v-model="customDescription" rows="3"></textarea>
            </label>

            <label>
              <span>Notas personalizadas</span>
              <textarea v-model="customNotes" rows="3" placeholder="Opcional"></textarea>
            </label>

            <div class="modal-actions">
              <button class="secondary-button" type="button" @click="closeCustomModal">Cancelar</button>
              <button class="primary-button" type="button" :disabled="submitting" @click="handleCreateCustomBeast">
                {{ submitting ? "Guardando…" : "Confirmar" }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </teleport>

    <teleport to="body">
      <div v-if="selectedBeast" class="modal-backdrop" @click.self="closeBeastDetails">
        <div class="modal modal-detail">
          <div class="modal-header">
            <div>
              <p class="eyebrow">Detalle de bestia</p>
              <h2>{{ selectedBeast.name }}</h2>
            </div>
            <div class="modal-header-actions">
              <button class="ghost-button" type="button" @click="closeBeastDetails">Cerrar</button>
            </div>
          </div>

          <p v-if="selectedBeastError" class="error">{{ selectedBeastError }}</p>

          <div class="detail-grid">
            <section class="info-block">
              <h3>Datos</h3>
              <dl class="stats stats-3">
                <div><dt>Origen</dt><dd>{{ beastTypeLabel(selectedBeast) }}</dd></div>
                <div><dt>Código</dt><dd>{{ selectedBeast.catalogBeastCode ?? "—" }}</dd></div>
                <div><dt>Tamaño</dt><dd>{{ beastSizeLabel(selectedBeast.size) }}</dd></div>
                <div><dt>Fuerza</dt><dd>{{ selectedBeast.strength }}</dd></div>
                <div><dt>Velocidad</dt><dd>{{ selectedBeast.speed }}</dd></div>
                <div><dt>Adaptación</dt><dd>{{ beastThermalLabel(selectedBeast.thermalAdaptation) }}</dd></div>
                <div><dt>Precio base</dt><dd>{{ beastPriceLabel(selectedBeast.basePrice) }}</dd></div>
                <div><dt>Adiestrado</dt><dd>{{ beastPriceLabel(selectedBeast.trainedPrice) }}</dd></div>
                <div><dt>Cuatro patas</dt><dd>{{ selectedBeast.fourLegged ? "Sí" : "No" }}</dd></div>
              </dl>
            </section>

            <section class="info-block">
              <h3>Estado actual</h3>
              <p><strong>Asignación:</strong> {{ currentAssignmentLabel(selectedBeast) }}</p>
              <p v-if="selectedBeast.assignedWagonName">
                <strong>Carro:</strong> {{ selectedBeast.assignedWagonName }}
              </p>
              <p v-if="selectedBeast.customNotes">
                <strong>Notas:</strong> {{ selectedBeast.customNotes }}
              </p>
              <p><strong>Nota especial:</strong> {{ selectedBeast.specialNote }}</p>
              <p>{{ selectedBeast.description }}</p>
              <p v-if="selectedBeast.sourceType === 'CATALOG' && selectedBeast.catalogBeastCode" class="muted">
                Catálogo: {{ selectedBeastCatalogItem?.name ?? selectedBeast.catalogBeastCode }}
              </p>
            </section>
          </div>

          <section class="info-block">
            <div class="section-header">
              <div>
                <h3>Asignar como tiro</h3>
                <p class="muted">La validación respeta el límite de tiro del carro y la fuerza requerida.</p>
              </div>
              <div class="inline-actions">
                <button class="secondary-button" type="button" :disabled="submitting || selectedBeast.assignmentType === 'NONE'" @click="clearAssignment">
                  Quitar asignación
                </button>
                <button class="primary-button" type="button" :disabled="submitting || !selectedDraftWagonId" @click="assignAsDraft">
                  Asignar como tiro
                </button>
              </div>
            </div>

            <div class="two-columns">
              <label>
                <span>Carro</span>
                <select v-model="selectedDraftWagonId" :disabled="submitting">
                  <option value="">Selecciona un carro</option>
                  <option v-for="wagon in wagons" :key="wagon.id" :value="wagon.id">
                    {{ wagon.name }} · {{ draftLabel(wagon) }}
                  </option>
                </select>
              </label>

              <div class="draft-summary">
                <span>Requisito de tiro</span>
                <strong v-if="selectedDraftRequirement">
                  {{ selectedDraftRequirement.maxLargeBeasts }} grandes / {{ selectedDraftRequirement.maxMediumBeasts }} medianas
                  · fuerza {{ selectedDraftRequirement.minimumStrength }}
                </strong>
                <strong v-else>—</strong>
              </div>
            </div>
          </section>

          <section class="info-block">
            <div class="section-header">
              <div>
                <h3>Asignar como viajero</h3>
                <p class="muted">La bestia ocupará una plaza de viajero en el carro seleccionado.</p>
              </div>
              <button class="primary-button" type="button" :disabled="submitting || !selectedTravelerWagonId" @click="assignAsTraveler">
                Asignar como viajero
              </button>
            </div>

            <div class="two-columns">
              <label>
                <span>Carro</span>
                <select v-model="selectedTravelerWagonId" :disabled="submitting">
                  <option value="">Selecciona un carro</option>
                  <option v-for="wagon in wagons" :key="wagon.id" :value="wagon.id">
                    {{ wagon.name }} · {{ travelerOccupancyForWagon(wagon.id) }}/{{ wagon.travelerCapacity }} viajeros
                  </option>
                </select>
              </label>

              <div class="draft-summary">
                <span>Viajeros y bestias viajando</span>
                <strong v-if="selectedTravelerWagon">
                  {{ travelerOccupancyForWagon(selectedTravelerWagon.id) }}/{{ selectedTravelerWagon.travelerCapacity }}
                </strong>
                <strong v-else>—</strong>
              </div>
            </div>
          </section>
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
  width: min(1300px, 100%);
  margin: 0 auto;
  display: grid;
  gap: 1.25rem;
}

.hero,
.section-header,
.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.hero-actions,
.modal-header-actions,
.inline-actions {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
  justify-content: flex-end;
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

.card,
.modal,
.catalog-card {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 1rem;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.08);
}

.card {
  padding: 1.25rem;
}

.empty-state,
.empty-state-inline {
  display: grid;
  place-items: center;
  gap: 0.5rem;
  text-align: center;
}

.primary-button,
.secondary-button,
.ghost-button,
.primary-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 0.85rem;
  font: inherit;
  padding: 0.8rem 1rem;
  border: 1px solid #cbd5e1;
  cursor: pointer;
}

.primary-button {
  background: #1d4ed8;
  color: white;
  border-color: #1d4ed8;
}

.secondary-button,
.ghost-button {
  background: white;
}

.primary-link {
  background: #1d4ed8;
  color: white;
  text-decoration: none;
}

.error {
  padding: 0.85rem 1rem;
  border-radius: 0.85rem;
  background: #fef2f2;
  color: #b91c1c;
}

.filters {
  display: grid;
  grid-template-columns: 1.3fr 0.8fr 0.8fr 0.8fr;
  gap: 0.75rem;
  margin-top: 1rem;
}

.filters label,
.create-form label,
.two-columns label,
.catalog-card {
  display: grid;
  gap: 0.35rem;
}

.filters span,
.create-form span,
.two-columns span {
  font-size: 0.85rem;
  color: #6b7280;
}

.filters input,
.filters select,
.create-form input,
.create-form select,
.create-form textarea,
.two-columns input,
.two-columns select {
  width: 100%;
  padding: 0.75rem 0.9rem;
  border-radius: 0.8rem;
  border: 1px solid #d1d5db;
  font: inherit;
  background: white;
}

.stats-inline {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.stats-inline div {
  padding: 0.7rem 0.85rem;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 0.85rem;
  min-width: 7rem;
}

.stats-inline span {
  display: block;
  font-size: 0.8rem;
  color: #6b7280;
}

.stats-inline strong {
  display: block;
  font-size: 1.05rem;
}

.table-wrap {
  overflow: auto;
  margin-top: 1rem;
}

.beast-table {
  width: 100%;
  border-collapse: collapse;
}

.beast-table th,
.beast-table td {
  text-align: left;
  padding: 0.8rem;
  border-bottom: 1px solid #e5e7eb;
  vertical-align: top;
}

.beast-table tbody tr {
  cursor: pointer;
}

.beast-table tbody tr:hover {
  background: #f8fafc;
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.55);
  display: grid;
  place-items: center;
  padding: 1.5rem;
}

.modal {
  width: min(1200px, 100%);
  max-height: 90vh;
  overflow: auto;
  padding: 1.25rem;
  display: grid;
  gap: 1rem;
}

.modal-create {
  width: min(900px, 100%);
}

.modal-detail {
  width: min(1100px, 100%);
}

.catalog-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.85rem;
}

.catalog-card {
  padding: 1rem;
}

.catalog-header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: start;
}

.create-form,
.detail-grid {
  display: grid;
  gap: 1rem;
}

.detail-grid {
  grid-template-columns: 1fr 1fr;
}

.stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.75rem;
}

.stats-3 {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.stats div {
  padding: 0.85rem;
  border-radius: 0.85rem;
  background: #fff;
  border: 1px solid #e5e7eb;
}

dt {
  font-size: 0.8rem;
  color: #6b7280;
}

dd {
  margin: 0.2rem 0 0;
  font-size: 0.98rem;
  font-weight: 700;
}

.info-block {
  padding: 0.95rem;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 0.9rem;
  display: grid;
  gap: 0.7rem;
}

.two-columns {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.85rem;
}

.checkbox-row {
  display: flex !important;
  align-items: center;
  gap: 0.6rem;
  padding-top: 1.35rem;
}

.draft-summary {
  display: grid;
  gap: 0.35rem;
  align-content: start;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

@media (max-width: 1080px) {
  .filters,
  .detail-grid,
  .catalog-grid,
  .stats,
  .two-columns {
    grid-template-columns: 1fr;
  }

  .hero,
  .section-header,
  .modal-header {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 720px) {
  .page {
    padding: 1rem;
  }

  .hero-actions,
  .modal-header-actions,
  .inline-actions,
  .modal-actions {
    width: 100%;
    flex-direction: column;
  }
}
</style>
