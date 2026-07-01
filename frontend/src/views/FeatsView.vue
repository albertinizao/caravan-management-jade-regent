<script setup lang="ts">
import { computed, onMounted, ref } from "vue";

import { useToast } from "@/composables/useToast";
import { getActiveCaravan, listCaravans } from "@/services/caravans";
import { addCaravanFeat, listCaravanFeatCatalog, listCaravanFeats, updateCaravanFeat } from "@/services/feats";
import type { Caravan } from "@/types/caravan";
import type { CaravanFeat, CaravanFeatAcquisitionSourceType, CaravanFeatCatalogItem } from "@/types/feat";

const caravans = ref<Caravan[]>([]);
const activeCaravan = ref<Caravan | null>(null);
const catalog = ref<CaravanFeatCatalogItem[]>([]);
const feats = ref<CaravanFeat[]>([]);
const loading = ref(true);
const submitting = ref(false);
const error = ref<string | null>(null);
const pendingAction = ref<string | null>(null);
const search = ref("");
const addModalOpen = ref(false);
const editModalOpen = ref(false);
const modalMode = ref<"add" | "edit">("add");
const selectedFeat = ref<CaravanFeat | null>(null);
const selectedFeatTypeCode = ref("");
const acquisitionSourceType = ref<CaravanFeatAcquisitionSourceType>("LEVEL_UP");
const acquisitionLevel = ref("");
const acquisitionCause = ref("");
const featActive = ref(true);
const modalError = ref<string | null>(null);
const { showToast } = useToast();

const selectedCatalogItem = computed(() =>
  catalog.value.find((item) => item.code === selectedFeatTypeCode.value) ?? null,
);

const catalogByCode = computed(() => Object.fromEntries(catalog.value.map((item) => [item.code, item] as const)));

const visibleFeats = computed(() => {
  const query = search.value.trim().toLowerCase();

  if (!query) {
    return feats.value;
  }

  return feats.value.filter((feat) => {
    const name = feat.name.toLowerCase();
    const cause = feat.acquisitionCause?.toLowerCase() ?? "";
    const code = feat.featTypeCode.toLowerCase();

    return name.includes(query) || cause.includes(query) || code.includes(query);
  });
});

const visibleCatalog = computed(() => catalog.value);

function isPending(action: string) {
  return pendingAction.value === action;
}

function resetForm() {
  selectedFeatTypeCode.value = catalog.value[0]?.code ?? "";
  acquisitionSourceType.value = "LEVEL_UP";
  acquisitionLevel.value = "";
  acquisitionCause.value = "";
  featActive.value = true;
  modalError.value = null;
}

function openAddModal() {
  modalMode.value = "add";
  selectedFeat.value = null;
  resetForm();
  addModalOpen.value = true;
}

function openEditModal(feat: CaravanFeat) {
  modalMode.value = "edit";
  selectedFeat.value = feat;
  selectedFeatTypeCode.value = feat.featTypeCode;
  acquisitionSourceType.value = feat.acquisitionSourceType;
  acquisitionLevel.value = feat.acquisitionLevel === null ? "" : String(feat.acquisitionLevel);
  acquisitionCause.value = feat.acquisitionCause ?? "";
  featActive.value = feat.active;
  modalError.value = null;
  editModalOpen.value = true;
}

function closeModal() {
  addModalOpen.value = false;
  editModalOpen.value = false;
  selectedFeat.value = null;
  modalError.value = null;
}

async function refresh() {
  loading.value = true;
  error.value = null;
  try {
    const [caravanList, activeResponse] = await Promise.all([listCaravans(), getActiveCaravan()]);
    caravans.value = caravanList;
    activeCaravan.value = activeResponse.caravan;

    if (activeResponse.caravan) {
      const [catalogResponse, featsResponse] = await Promise.all([
        listCaravanFeatCatalog(activeResponse.caravan.id),
        listCaravanFeats(activeResponse.caravan.id),
      ]);
      catalog.value = catalogResponse;
      feats.value = featsResponse;
      if (!catalogResponse.some((item) => item.code === selectedFeatTypeCode.value)) {
        selectedFeatTypeCode.value = catalogResponse[0]?.code ?? "";
      }
    } else {
      catalog.value = [];
      feats.value = [];
      selectedFeatTypeCode.value = "";
      closeModal();
    }
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "Failed to load feats";
  } finally {
    loading.value = false;
  }
}

function validateForm() {
  if (!selectedFeatTypeCode.value) {
    return "Debes seleccionar una dote";
  }

  if (acquisitionSourceType.value === "LEVEL_UP") {
    const level = Number(acquisitionLevel.value);
    if (!Number.isInteger(level) || level < 1) {
      return "Debes indicar un nivel válido";
    }
  } else if (!acquisitionCause.value.trim()) {
    return "Debes indicar la causa";
  }

  return null;
}

async function submitFeat() {
  if (!activeCaravan.value) {
    return;
  }

  const validationError = validateForm();
  if (validationError) {
    modalError.value = validationError;
    return;
  }

  submitting.value = true;
  pendingAction.value = modalMode.value === "add" ? "add" : "edit";
  modalError.value = null;

  try {
    const payload = {
      featTypeCode: selectedFeatTypeCode.value,
      acquisitionSourceType: acquisitionSourceType.value,
      acquisitionLevel: acquisitionSourceType.value === "LEVEL_UP" ? Number(acquisitionLevel.value) : null,
      acquisitionCause: acquisitionSourceType.value === "OTHER" ? acquisitionCause.value.trim() : null,
      active: featActive.value,
    };

    if (modalMode.value === "add") {
      await addCaravanFeat(activeCaravan.value.id, payload);
      showToast(`Dote añadida: ${catalogByCode.value[selectedFeatTypeCode.value]?.name ?? selectedFeatTypeCode.value}.`, "success");
    } else if (selectedFeat.value) {
      await updateCaravanFeat(activeCaravan.value.id, selectedFeat.value.id, payload);
      showToast(`Dote actualizada: ${selectedFeat.value.name}.`, "success");
    }

    await refresh();
    closeModal();
  } catch (cause) {
    modalError.value = cause instanceof Error ? cause.message : "Failed to save feat";
  } finally {
    submitting.value = false;
    pendingAction.value = null;
  }
}

onMounted(refresh);
</script>

<template>
  <main class="page-shell">
    <section class="page-header">
      <div>
        <p class="eyebrow">Caravana activa</p>
        <h1>Dotes</h1>
        <p v-if="activeCaravan">{{ activeCaravan.name }} · Nivel {{ activeCaravan.level }}</p>
        <p v-else>No hay una caravana activa.</p>
      </div>
      <div class="header-actions">
        <button class="primary" type="button" @click="openAddModal" :disabled="!activeCaravan || isPending('add')">
          Añadir dote
        </button>
      </div>
    </section>

    <section v-if="loading" class="empty-state">Cargando dotes…</section>
    <section v-else-if="error" class="error-banner">{{ error }}</section>
    <template v-else>
      <section v-if="!activeCaravan" class="panel empty-state">
        <h2>No hay una caravana activa</h2>
        <p class="muted">Debes seleccionar o crear una caravana antes de gestionar dotes.</p>
        <RouterLink class="primary" to="/">Ir a caravanas</RouterLink>
      </section>

      <template v-else>
        <section class="panel">
          <div class="panel-header">
            <h2>Dotes de la caravana</h2>
            <input v-model="search" type="search" placeholder="Buscar dote o causa…" class="search-input" />
          </div>
          <div v-if="visibleFeats.length === 0" class="empty-state">La caravana todavía no tiene dotes que coincidan con el filtro.</div>
          <table v-else class="data-table">
            <thead>
              <tr>
                <th>Dote</th>
                <th>Estado</th>
                <th>Adquisición</th>
                <th>Detalle</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="feat in visibleFeats" :key="feat.id">
                <td>
                  <strong>{{ feat.name }}</strong>
                  <div class="muted">{{ feat.featTypeCode }}</div>
                </td>
                <td>
                  <span class="badge" :class="feat.active ? 'badge--ok' : 'badge--warn'">
                    {{ feat.active ? "Activa" : "Inactiva" }}
                  </span>
                  <div v-if="feat.blockedReason" class="muted">{{ feat.blockedReason }}</div>
                </td>
                <td>
                  <div>{{ feat.acquisitionSourceType === "LEVEL_UP" ? `Nivel ${feat.acquisitionLevel}` : "Otra causa" }}</div>
                  <div class="muted">{{ feat.acquisitionCause ?? "—" }}</div>
                </td>
                <td>
                  <div class="muted">Selección #{{ feat.selectionIndex }}</div>
                  <div class="muted">{{ feat.benefitText }}</div>
                </td>
                <td class="actions">
                  <button type="button" @click="openEditModal(feat)">Editar</button>
                </td>
              </tr>
            </tbody>
          </table>
        </section>

        <section class="panel">
          <div class="panel-header">
            <h2>Catálogo</h2>
            <p class="muted">{{ visibleCatalog.length }} dotes visibles</p>
          </div>

          <div class="catalog-grid">
            <article v-for="item in visibleCatalog" :key="item.code" class="catalog-card" :class="{ 'catalog-card--blocked': !item.available }">
              <div class="catalog-card__top">
                <strong>{{ item.name }}</strong>
                <span class="badge" :class="item.available ? 'badge--ok' : 'badge--warn'">
                  {{ item.available ? "Disponible" : "Bloqueada" }}
                </span>
              </div>
              <p class="muted">{{ item.prerequisites.join(" · ") || "Sin prerrequisitos declarados" }}</p>
              <p>{{ item.benefitText }}</p>
              <p v-if="item.specialText" class="muted">{{ item.specialText }}</p>
              <p v-if="item.notes" class="muted">{{ item.notes }}</p>
              <p class="muted">Seleccionada {{ item.ownedCount }} veces · Límite {{ item.selectionLimit }}</p>
              <p v-if="item.blockedReason" class="warning-text">{{ item.blockedReason }}</p>
            </article>
          </div>
        </section>
      </template>
    </template>

    <teleport to="body">
      <div v-if="addModalOpen || editModalOpen" class="modal-backdrop" @click.self="closeModal">
        <section class="modal">
          <header class="modal__header">
            <div>
              <p class="eyebrow">{{ modalMode === "add" ? "Nueva dote" : "Editar dote" }}</p>
              <h2>{{ modalMode === "add" ? "Añadir dote" : "Editar adquisición" }}</h2>
            </div>
            <button type="button" class="ghost" @click="closeModal">Cerrar</button>
          </header>

          <div class="modal__body">
            <label>
              <span>Dote</span>
              <select v-model="selectedFeatTypeCode" :disabled="modalMode === 'edit'">
                <option v-for="item in catalog" :key="item.code" :value="item.code">
                  {{ item.name }}{{ item.available ? "" : " (bloqueada)" }}
                </option>
              </select>
            </label>

            <label>
              <span>Fuente de adquisición</span>
              <select v-model="acquisitionSourceType">
                <option value="LEVEL_UP">Subida de nivel</option>
                <option value="OTHER">Otra causa</option>
              </select>
            </label>

            <label v-if="acquisitionSourceType === 'LEVEL_UP'">
              <span>Nivel</span>
              <input v-model="acquisitionLevel" type="number" min="1" step="1" />
            </label>

            <label v-else>
              <span>Causa</span>
              <input v-model="acquisitionCause" type="text" placeholder="Ej. recompensa de campaña" />
            </label>

            <label class="toggle-row">
              <input v-model="featActive" type="checkbox" />
              <span>Activa</span>
            </label>
            <p class="muted">
              Cuando está activa, la dote aporta su beneficio a la caravana. Caravana Familiar y Líder de la caravana empiezan activas por defecto.
            </p>

            <p v-if="selectedCatalogItem" class="muted">
              {{ selectedCatalogItem.benefitText }}
            </p>
            <p v-if="selectedCatalogItem?.blockedReason" class="warning-text">{{ selectedCatalogItem.blockedReason }}</p>
            <p v-if="modalError" class="error-banner">{{ modalError }}</p>
          </div>

          <footer class="modal__footer">
            <button type="button" class="ghost" @click="closeModal">Cancelar</button>
            <button type="button" class="primary" @click="submitFeat" :disabled="submitting">
              {{ submitting ? "Guardando…" : "Guardar" }}
            </button>
          </footer>
        </section>
      </div>
    </teleport>
  </main>
</template>

<style scoped>
.page-shell {
  padding: 1.5rem;
  display: grid;
  gap: 1rem;
}

.page-header,
.panel,
.modal {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 1rem;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.06);
}

.page-header {
  padding: 1.25rem 1.5rem;
  display: flex;
  justify-content: space-between;
  gap: 1rem;
}

.panel {
  padding: 1.25rem 1.5rem;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
  margin-bottom: 1rem;
}

.eyebrow {
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-size: 0.72rem;
  color: #6b7280;
  margin: 0 0 0.35rem;
}

.muted {
  color: #6b7280;
  font-size: 0.92rem;
}

.warning-text {
  color: #b45309;
}

.error-banner {
  padding: 0.9rem 1rem;
  background: #fef2f2;
  color: #b91c1c;
  border: 1px solid #fecaca;
  border-radius: 0.9rem;
}

.empty-state {
  padding: 1rem;
  border: 1px dashed #cbd5e1;
  border-radius: 0.9rem;
  color: #64748b;
  background: #fff;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th,
.data-table td {
  padding: 0.8rem 0.7rem;
  border-bottom: 1px solid #e5e7eb;
  vertical-align: top;
  text-align: left;
}

.actions {
  text-align: right;
}

.actions button,
.header-actions button,
.modal__footer button,
.ghost {
  border-radius: 0.75rem;
  padding: 0.65rem 0.95rem;
  border: 1px solid #cbd5e1;
  background: white;
}

.primary {
  background: #2563eb;
  color: white;
  border-color: #2563eb;
}

.badge {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  padding: 0.2rem 0.55rem;
  font-size: 0.78rem;
  font-weight: 700;
}

.badge--ok {
  background: #dcfce7;
  color: #166534;
}

.badge--warn {
  background: #fef3c7;
  color: #92400e;
}

.catalog-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 0.85rem;
}

.catalog-card {
  border: 1px solid #e5e7eb;
  border-radius: 0.9rem;
  padding: 0.95rem;
}

.catalog-card--blocked {
  background: #fffbeb;
}

.catalog-card__top {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
}

.search-input,
select,
input {
  width: 100%;
  border-radius: 0.75rem;
  border: 1px solid #cbd5e1;
  padding: 0.7rem 0.85rem;
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: grid;
  place-items: center;
  padding: 1rem;
  z-index: 1000;
}

.modal {
  width: min(720px, 100%);
  padding: 1.25rem;
}

.modal__header,
.modal__footer {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
}

.modal__body {
  display: grid;
  gap: 0.9rem;
  margin: 1rem 0;
}

.modal__body label {
  display: grid;
  gap: 0.35rem;
}

.modal__body span {
  font-weight: 600;
}

.toggle-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}
</style>
