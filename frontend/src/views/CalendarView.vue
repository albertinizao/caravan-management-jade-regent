<script setup lang="ts">
import { computed, onMounted, ref } from "vue";

import { useToast } from "@/composables/useToast";
import {
  confirmCaravanDayCycle,
  confirmCaravanMultiDayCycle,
  getActiveCaravan,
  previewCaravanDayCycle,
  previewCaravanMultiDayCycle,
} from "@/services/caravans";
import { createCalendarEvent, deleteCalendarEvent, getCalendarDay, getCalendarMonth, setCalendarCurrentDate } from "@/services/calendar";
import { getCaravanWeatherProfile, updateCaravanWeatherProfile } from "@/services/weather";
import type { Caravan } from "@/types/caravan";
import type { CaravanDayCyclePreview, CaravanMultiDayCyclePreview } from "@/types/caravan";
import type { CalendarDay, CalendarEvent, CalendarMonth, GolarionDate, WeatherSnapshot } from "@/types/calendar";
import type { CaravanWeatherProfile } from "@/types/weather";
import {
  getWeatherPrecipitationIcon,
  getWeatherTemperatureRiskIcon,
  getWeatherTemperatureTooltip,
  getWeatherWindIcon,
} from "@/utils/weatherIcons";

const activeCaravan = ref<Caravan | null>(null);
const monthView = ref<CalendarMonth | null>(null);
const selectedDay = ref<CalendarDay | null>(null);
const loading = ref(true);
const calendarLoading = ref(false);
const pendingAction = ref<string | null>(null);
const error = ref<string | null>(null);
const manualYear = ref(4712);
const manualMonth = ref(1);
const manualDay = ref(1);
const bulkAdvanceDays = ref(7);
const dayCycleModalOpen = ref(false);
const dayCycleLoading = ref(false);
const dayCycleSubmitting = ref(false);
const dayCyclePreview = ref<CaravanDayCyclePreview | null>(null);
const manualDateModalOpen = ref(false);
const multiDayModalOpen = ref(false);
const multiDayLoading = ref(false);
const multiDaySubmitting = ref(false);
const multiDayRequestedDays = ref(2);
const multiDayPreview = ref<CaravanMultiDayCyclePreview | null>(null);
const weatherProfile = ref<CaravanWeatherProfile | null>(null);
const weatherProfileLoading = ref(false);
const weatherProfileSaving = ref(false);
const weatherConfigModalOpen = ref(false);
const weatherClimateBaseline = ref<CaravanWeatherProfile["climateBaseline"]>("TEMPERATE");
const weatherElevation = ref<CaravanWeatherProfile["elevation"]>("SEA_LEVEL");
const weatherCrownOfWorld = ref(false);
const weatherEffectiveFromYear = ref(4712);
const weatherEffectiveFromMonth = ref(1);
const weatherEffectiveFromDay = ref(1);
const showSecretEvents = ref(false);
const customEventModalOpen = ref(false);
const customEventSaving = ref(false);
const customEventDeletingId = ref<number | null>(null);
const customEventYear = ref(4712);
const customEventMonth = ref(1);
const customEventDay = ref(1);
const customEventName = ref("");
const customEventDescription = ref("");
const customEventSecret = ref(false);
const { showToast } = useToast();

const supportedYears = Array.from({ length: 11 }, (_, index) => 4712 + index);
const supportedMonths = [
  { value: 1, label: "Abadio" },
  { value: 2, label: "Calistril" },
  { value: 3, label: "Farasto" },
  { value: 4, label: "Gozran" },
  { value: 5, label: "Desnio" },
  { value: 6, label: "Sarenith" },
  { value: 7, label: "Erasto" },
  { value: 8, label: "Arodio" },
  { value: 9, label: "Rova" },
  { value: 10, label: "Lamashan" },
  { value: 11, label: "Neth" },
  { value: 12, label: "Kuthona" },
];
const weatherClimateBaselineOptions: Array<{ value: CaravanWeatherProfile["climateBaseline"]; label: string; description: string }> = [
  { value: "COLD", label: "Frío", description: "Más nieve y temperaturas más duras." },
  { value: "TEMPERATE", label: "Templado", description: "Base equilibrada para la mayoría de campañas." },
  { value: "TROPICAL", label: "Tropical", description: "Más calor y lluvia con menos hielo." },
];
const weatherElevationOptions: Array<{ value: CaravanWeatherProfile["elevation"]; label: string; description: string }> = [
  { value: "SEA_LEVEL", label: "Nivel del mar", description: "Aumenta el calor base." },
  { value: "LOWLAND", label: "Tierras bajas", description: "Sin gran corrección por altitud." },
  { value: "HIGHLAND", label: "Tierras altas", description: "Enfría y endurece el clima." },
  { value: "PEAK", label: "Cima", description: "Penalización fuerte de temperatura." },
];
const calendarEventNameTranslations: Record<string, string> = {
  "Abjurant Day": "Día de la abjuración",
  "Archerfeast / Archer's Day": "Fiesta de los arqueros / Día del arquero",
  "Ascendance Day": "Día del ascenso",
  "Ascendance Night": "Noche del ascenso",
  "Ascension Day": "Día de la ascensión",
  "Baptism of Ice": "Bautismo de hielo",
  "Bastion Day": "Día del bastión",
  "Blightmother's Eve": "Víspera de la Madre de la Plaga",
  "Breaching Festival": "Festival de la ruptura",
  "Burning Blades": "Hojas ardientes",
  "Burning Night": "Noche ardiente",
  "Conquest Day": "Día de la conquista",
  "Crabfest": "Fiesta del cangrejo",
  "Darkness Eternal": "Oscuridad eterna",
  "Day of Bones": "Día de los huesos",
  "Day of Destiny": "Día del destino",
  "Day of Silenced Whispers": "Día de los susurros silenciados",
  "Day of Sundering": "Día de la ruptura",
  "Day of the Inheritor": "Día del Heredero",
  "Days of Wrath": "Días de la ira",
  "Even-Tongued Day": "Día de la lengua persuasiva",
  "Evoking Day": "Día de la evocación",
  "Feast of the Survivors": "Banquete de los supervivientes",
  "Feast of Vigor": "Banquete del vigor",
  "Festival of Flowers": "Festival de las flores",
  "Festival of Making and Breaking": "Festival de la creación y la destrucción",
  "First Day of Summer": "Primer día del verano",
  "First Day of Winter": "Primer día del invierno",
  "First Crusader Day / Crusader Memorial Day": "Día del primer cruzado / Día conmemorativo de los cruzados",
  "Firstbloom": "Primera floración",
  "Foundation Day": "Día de la fundación",
  "Founder's Day": "Día del fundador",
  "Founding Day": "Día de la fundación",
  "Founding Festival": "Festival de la fundación",
  "Gala of Sails": "Gala de las velas",
  "Goblin Flea Market": "Mercado de pulgas goblin",
  "Golemwalk Parade": "Desfile de gólems",
  "Grand Day of Independence": "Gran día de la independencia",
  "Great Fire Remembrance": "Conmemoración del gran incendio",
  "Harvest Feast": "Fiesta de la cosecha",
  "Independence Day": "Día de la independencia",
  "Independence Day / Liberty Day": "Día de la independencia / Día de la libertad",
  "Inheritor's Ascendance": "Ascenso del Heredero",
  "Jestercap": "Gorro del bufón",
  "Kraken Carnival": "Carnaval del kraken",
  "Last Day of Summer": "Último día del verano",
  "Leap Day": "Día bisiesto",
  "Longnight": "Noche larga",
  "Lust Festival": "Festival de la lujuria",
  "Merrymead": "Fiesta de la hidromiel",
  "Mirror Poet's Farewell": "Despedida del poeta del espejo",
  "Mooncall": "Llamado lunar",
  "New Year": "Año Nuevo",
  "Night of Tears": "Noche de las lágrimas",
  "Old-Mage Day": "Día del viejo mago",
  "Planting Week": "Semana de la siembra",
  "Pseudodragon Festival": "Festival de los pseudodragones",
  "Ritual of Stardust": "Ritual del polvo estelar",
  "Ritual of the Whip Sting": "Ritual del aguijón del látigo",
  "Sable Company Founding Day": "Día de la fundación de la Compañía Sable",
  "Seven Veils": "Siete velos",
  "Swallowtail Festival / Swallowtail Release": "Festival de la cola de golondrina / Liberación de la cola de golondrina",
  "Tempest Day": "Día de la tempestad",
  "The Final Day": "El día final",
  "Vault Day": "Día de la bóveda",
  "Winter Week": "Semana de invierno",
  "Winterbloom": "Floración invernal",
};

function monthLengthFor(year: number, month: number) {
  const leapYear = year % 8 === 0;
  const monthLengths: Record<number, number> = {
    1: 31,
    2: leapYear ? 29 : 28,
    3: 31,
    4: 30,
    5: 31,
    6: 30,
    7: 31,
    8: 31,
    9: 30,
    10: 31,
    11: 30,
    12: 31,
  };
  return monthLengths[month] ?? 31;
}

function dayOptionsFor(year: number, month: number, selectedDay?: typeof manualDay) {
  const maxDay = monthLengthFor(year, month);
  if (selectedDay && selectedDay.value > maxDay) {
    selectedDay.value = maxDay;
  }
  return Array.from({ length: maxDay }, (_, index) => index + 1);
}

const manualDayOptions = computed(() => dayOptionsFor(manualYear.value, manualMonth.value, manualDay));
const customEventDayOptions = computed(() => dayOptionsFor(customEventYear.value, customEventMonth.value, customEventDay));

const weatherEffectiveFromDayOptions = computed(() =>
  dayOptionsFor(weatherEffectiveFromYear.value, weatherEffectiveFromMonth.value, weatherEffectiveFromDay),
);

const currentDateLabel = computed(() =>
  monthView.value
    ? `${monthView.value.currentDate.day} de ${monthView.value.currentDate.monthName} de ${monthView.value.currentDate.year} AR · ${monthView.value.currentDate.dayOfWeek}`
    : "—",
);

const selectedDateLabel = computed(() =>
  selectedDay.value
    ? `${selectedDay.value.date.day} de ${selectedDay.value.date.monthName} de ${selectedDay.value.date.year} AR`
    : "Selecciona un día",
);

const visibleMonthLabel = computed(() =>
  monthView.value
    ? `${monthView.value.displayMonthName} ${monthView.value.displayYear} AR`
    : "Calendario",
);

function isPending(action: string) {
  return pendingAction.value === action;
}

function formatDecimal(value: number) {
  return Number.isInteger(value) ? `${value}` : value.toFixed(1).replace(/\.0$/, "");
}

function formatWeatherToken(value: string | null | undefined) {
  if (!value) {
    return "—";
  }
  if (value === "NONE") {
    return "Sin precipitación";
  }
  return value.split("_").join(" ");
}

function formatCalendarEventName(name: string) {
  return calendarEventNameTranslations[name] ?? name;
}

function calendarEventKey(event: CalendarEvent) {
  return event.id !== null
    ? `${event.category}-${event.id}`
    : `${event.category}-${event.name}-${event.scope ?? "global"}-${event.secret ? "secret" : "public"}`;
}

function isVisibleCustomEvent(event: { secret: boolean }) {
  return showSecretEvents.value || !event.secret;
}

function visibleCalendarEvents(day: CalendarDay) {
  return [...day.canonicalEvents, ...day.customEvents].filter(isVisibleCustomEvent);
}

function visibleCustomEvents(day: CalendarDay) {
  return day.customEvents.filter(isVisibleCustomEvent);
}

function hiddenCustomEventsCount(day: CalendarDay) {
  return day.customEvents.filter((event) => event.secret && !showSecretEvents.value).length;
}

function syncCustomEventForm(date: GolarionDate) {
  customEventYear.value = date.year;
  customEventMonth.value = date.month;
  customEventDay.value = date.day;
}

function resetCustomEventForm() {
  customEventName.value = "";
  customEventDescription.value = "";
  customEventSecret.value = false;
}

function openCustomEventModal() {
  if (!activeCaravan.value || !monthView.value) {
    return;
  }

  syncCustomEventForm(selectedDay.value?.date ?? monthView.value.currentDate);
  resetCustomEventForm();
  customEventModalOpen.value = true;
}

function closeCustomEventModal() {
  if (customEventSaving.value) {
    return;
  }

  customEventModalOpen.value = false;
}

function toggleSecretEvents() {
  showSecretEvents.value = !showSecretEvents.value;
}

function canDeleteCustomEvent(event: { id: number | null }) {
  return event.id !== null;
}

function weatherPrecipitationIcon(value: string | null | undefined) {
  return getWeatherPrecipitationIcon(value);
}

function weatherWindIcon(value: string | null | undefined) {
  return getWeatherWindIcon(value);
}

function weatherTemperatureIcon(temperatureF: number | null | undefined) {
  return getWeatherTemperatureRiskIcon(temperatureF);
}

function weatherTemperatureTooltip(temperatureC: number | null | undefined, temperatureF: number | null | undefined) {
  return getWeatherTemperatureTooltip(temperatureC, temperatureF);
}

function weatherPeriodRows(weather: WeatherSnapshot | null) {
  if (!weather) {
    return [];
  }

  return [
    { key: "midnightToDawn", label: "Madrugada", period: weather.midnightToDawn },
    { key: "dawnToNoon", label: "Mañana", period: weather.dawnToNoon },
    { key: "noonToDusk", label: "Tarde", period: weather.noonToDusk },
    { key: "duskToMidnight", label: "Noche", period: weather.duskToMidnight },
  ];
}

function syncWeatherProfileForm(profile: CaravanWeatherProfile) {
  weatherClimateBaseline.value = profile.climateBaseline;
  weatherElevation.value = profile.elevation;
  weatherCrownOfWorld.value = profile.crownOfWorld;
}

function syncWeatherEffectiveFromDate(date: GolarionDate) {
  weatherEffectiveFromYear.value = date.year;
  weatherEffectiveFromMonth.value = date.month;
  weatherEffectiveFromDay.value = date.day;
}

function weatherClimateBaselineDescription() {
  return weatherClimateBaselineOptions.find((option) => option.value === weatherClimateBaseline.value)?.description ?? "";
}

function weatherElevationDescription() {
  return weatherElevationOptions.find((option) => option.value === weatherElevation.value)?.description ?? "";
}

async function loadWeatherProfile() {
  if (!activeCaravan.value) {
    return;
  }

  weatherProfileLoading.value = true;
  try {
    const profile = await getCaravanWeatherProfile(activeCaravan.value.id);
    weatherProfile.value = profile;
    syncWeatherProfileForm(profile);
  } finally {
    weatherProfileLoading.value = false;
  }
}

async function saveWeatherProfile() {
  if (!activeCaravan.value) {
    return;
  }

  weatherProfileSaving.value = true;
  pendingAction.value = "weather-profile";
  try {
    const updated = await updateCaravanWeatherProfile(activeCaravan.value.id, {
      climateBaseline: weatherClimateBaseline.value,
      elevation: weatherElevation.value,
      crownOfWorld: weatherCrownOfWorld.value,
      effectiveFromYear: weatherEffectiveFromYear.value,
      effectiveFromMonth: weatherEffectiveFromMonth.value,
      effectiveFromDay: weatherEffectiveFromDay.value,
    });
    weatherProfile.value = updated;
    syncWeatherProfileForm(updated);
    if (monthView.value) {
      await loadCalendarMonth(
        monthView.value.displayYear,
        monthView.value.displayMonth,
        selectedDay.value?.date ?? monthView.value.currentDate,
      );
    }
    showToast("La configuración climática se ha guardado.", "success");
  } catch (caughtError) {
    showToast(caughtError instanceof Error ? caughtError.message : "No se pudo guardar el clima.", "error");
  } finally {
    weatherProfileSaving.value = false;
    pendingAction.value = null;
  }
}

async function saveCustomEvent() {
  if (!activeCaravan.value) {
    return;
  }

  const name = customEventName.value.trim();
  if (!name) {
    showToast("El nombre del evento es obligatorio.", "error");
    return;
  }

  customEventSaving.value = true;
  pendingAction.value = "custom-event";
  try {
    const updated = await createCalendarEvent(activeCaravan.value.id, {
      year: customEventYear.value,
      month: customEventMonth.value,
      day: customEventDay.value,
      name,
      description: customEventDescription.value.trim() || null,
      secret: customEventSecret.value,
    });
    selectedDay.value = updated;
    await loadCalendarMonth(updated.date.year, updated.date.month, updated.date);
    closeCustomEventModal();
    showToast("El evento personalizado se ha creado.", "success");
  } catch (caughtError) {
    showToast(caughtError instanceof Error ? caughtError.message : "No se pudo crear el evento.", "error");
  } finally {
    customEventSaving.value = false;
    pendingAction.value = null;
  }
}

async function deleteCustomEvent(event: CalendarEvent) {
  if (!activeCaravan.value || event.id === null) {
    return;
  }

  const confirmed = window.confirm(
    `¿Eliminar "${formatCalendarEventName(event.name)}"? Esta acción no se puede deshacer.`,
  );
  if (!confirmed) {
    return;
  }

  customEventDeletingId.value = event.id;
  pendingAction.value = "custom-event-delete";
  try {
    const updated = await deleteCalendarEvent(activeCaravan.value.id, event.id);
    selectedDay.value = updated;
    await loadCalendarMonth(updated.date.year, updated.date.month, updated.date);
    showToast(`Evento eliminado: ${formatCalendarEventName(event.name)}.`, "success");
  } catch (caughtError) {
    showToast(caughtError instanceof Error ? caughtError.message : "No se pudo borrar el evento.", "error");
  } finally {
    customEventDeletingId.value = null;
    pendingAction.value = null;
  }
}

function openManualDateModal() {
  if (!activeCaravan.value) {
    return;
  }

  manualDateModalOpen.value = true;
}

function closeManualDateModal() {
  if (pendingAction.value === "manual-date") {
    return;
  }

  manualDateModalOpen.value = false;
}

function openWeatherConfigModal() {
  if (!activeCaravan.value || !monthView.value) {
    return;
  }

  syncWeatherEffectiveFromDate(selectedDay.value?.date ?? monthView.value.currentDate);
  weatherConfigModalOpen.value = true;
}

function closeWeatherConfigModal() {
  if (weatherProfileSaving.value) {
    return;
  }

  weatherConfigModalOpen.value = false;
}

async function loadCalendarMonth(year: number, month: number, selected?: GolarionDate) {
  if (!activeCaravan.value) {
    return;
  }
  calendarLoading.value = true;
  error.value = null;
  try {
    const monthResult = await getCalendarMonth(activeCaravan.value.id, year, month);
    monthView.value = monthResult;

    const targetDate = selected ?? monthResult.currentDate;
    manualYear.value = monthResult.currentDate.year;
    manualMonth.value = monthResult.currentDate.month;
    manualDay.value = monthResult.currentDate.day;
    selectedDay.value = await getCalendarDay(activeCaravan.value.id, targetDate.year, targetDate.month, targetDate.day);
  } catch (caughtError) {
    error.value = caughtError instanceof Error ? caughtError.message : "No se pudo cargar el calendario.";
  } finally {
    calendarLoading.value = false;
  }
}

async function loadActiveCaravan() {
  loading.value = true;
  error.value = null;
  try {
    const response = await getActiveCaravan();
    activeCaravan.value = response.caravan;
    if (activeCaravan.value) {
      await loadWeatherProfile();
      const bootstrapMonth = await getCalendarMonth(activeCaravan.value.id, 4712, 1);
      await loadCalendarMonth(
        bootstrapMonth.currentDate.year,
        bootstrapMonth.currentDate.month,
        bootstrapMonth.currentDate,
      );
    } else {
      monthView.value = null;
      selectedDay.value = null;
    }
  } catch (caughtError) {
    error.value = caughtError instanceof Error ? caughtError.message : "No se pudo cargar el calendario.";
  } finally {
    loading.value = false;
  }
}

async function selectDay(day: CalendarDay) {
  if (!activeCaravan.value) {
    return;
  }
  pendingAction.value = "select-day";
  try {
    selectedDay.value = await getCalendarDay(activeCaravan.value.id, day.date.year, day.date.month, day.date.day);
  } catch (caughtError) {
    showToast(caughtError instanceof Error ? caughtError.message : "No se pudo cargar el detalle del día.", "error");
  } finally {
    pendingAction.value = null;
  }
}

async function jumpToCampaignToday() {
  if (!monthView.value) {
    return;
  }
  await loadCalendarMonth(
    monthView.value.currentDate.year,
    monthView.value.currentDate.month,
    monthView.value.currentDate,
  );
}

async function changeVisibleMonth(delta: number) {
  if (!monthView.value) {
    return;
  }
  let year = monthView.value.displayYear;
  let month = monthView.value.displayMonth + delta;

  if (month < 1) {
    month = 12;
    year -= 1;
  } else if (month > 12) {
    month = 1;
    year += 1;
  }

  if (year < 4712 || year > 4722) {
    return;
  }

  await loadCalendarMonth(year, month, { year, month, day: 1, monthName: "", dayOfWeek: "", dayOfWeekAbbreviation: "" });
}

async function refreshAfterDayCycle() {
  await loadActiveCaravan();
}

async function openDayCycleModal() {
  if (!activeCaravan.value) {
    return;
  }

  dayCycleModalOpen.value = true;
  dayCycleLoading.value = true;
  dayCyclePreview.value = null;
  error.value = null;

  try {
    dayCyclePreview.value = await previewCaravanDayCycle(activeCaravan.value.id);
  } catch (caughtError) {
    error.value = caughtError instanceof Error ? caughtError.message : "No se pudo generar la previsualización del día";
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
  if (!activeCaravan.value || !dayCyclePreview.value) {
    return;
  }

  dayCycleSubmitting.value = true;
  pendingAction.value = "advance-1";
  error.value = null;

  try {
    await confirmCaravanDayCycle(activeCaravan.value.id, dayCyclePreview.value.previewFingerprint);
    await refreshAfterDayCycle();
    closeDayCycleModal();
    showToast(`Día pasado en ${activeCaravan.value.name}.`, "success");
  } catch (caughtError) {
    error.value = caughtError instanceof Error ? caughtError.message : "No se pudo confirmar el paso del día";
  } finally {
    dayCycleSubmitting.value = false;
    pendingAction.value = null;
  }
}

function openMultiDayModal() {
  if (!activeCaravan.value) {
    return;
  }

  multiDayRequestedDays.value = bulkAdvanceDays.value;
  multiDayPreview.value = null;
  multiDayModalOpen.value = true;
  error.value = null;
}

function closeMultiDayModal() {
  if (multiDaySubmitting.value) {
    return;
  }

  multiDayModalOpen.value = false;
  multiDayPreview.value = null;
}

async function previewMultipleDays() {
  if (!activeCaravan.value) {
    return;
  }

  const days = Number(multiDayRequestedDays.value);
  if (!Number.isInteger(days) || days < 1 || days > 30) {
    error.value = "Debes indicar un número entero de días entre 1 y 30.";
    return;
  }

  multiDayLoading.value = true;
  error.value = null;

  try {
    multiDayPreview.value = await previewCaravanMultiDayCycle(activeCaravan.value.id, days);
  } catch (caughtError) {
    error.value = caughtError instanceof Error ? caughtError.message : "No se pudo generar la simulación de varios días";
  } finally {
    multiDayLoading.value = false;
  }
}

async function confirmMultipleDays() {
  if (!activeCaravan.value || !multiDayPreview.value) {
    return;
  }

  multiDaySubmitting.value = true;
  pendingAction.value = "advance-bulk";
  error.value = null;

  try {
    const days = multiDayPreview.value.requestedDays;
    await confirmCaravanMultiDayCycle(
      activeCaravan.value.id,
      days,
      multiDayPreview.value.basePreviewFingerprint,
    );
    await refreshAfterDayCycle();
    closeMultiDayModal();
    showToast(`${days} días pasados en ${activeCaravan.value.name}.`, "success");
  } catch (caughtError) {
    error.value = caughtError instanceof Error ? caughtError.message : "No se pudo confirmar el paso de varios días";
  } finally {
    multiDaySubmitting.value = false;
    pendingAction.value = null;
  }
}

async function applyManualDate() {
  if (!activeCaravan.value) {
    return;
  }
  pendingAction.value = "manual-date";
  try {
    const updated = await setCalendarCurrentDate(activeCaravan.value.id, manualYear.value, manualMonth.value, manualDay.value);
    selectedDay.value = updated;
    await loadCalendarMonth(updated.date.year, updated.date.month, updated.date);
    showToast("La fecha actual de campaña se ha actualizado.", "success");
  } catch (caughtError) {
    showToast(caughtError instanceof Error ? caughtError.message : "No se pudo actualizar la fecha.", "error");
  } finally {
    pendingAction.value = null;
  }
}

onMounted(loadActiveCaravan);
</script>

<template>
  <main class="page">
    <section class="shell">
      <header class="hero">
        <div>
          <p class="eyebrow">Cronología de campaña</p>
          <h1>Calendario</h1>
          <p class="subtitle">
            Navega por Golarion, consulta los eventos del documento canónico y controla el día actual de la caravana.
          </p>
        </div>
      </header>

      <p v-if="error" class="error">{{ error }}</p>

      <section v-if="loading" class="card">
        <p class="muted">Cargando calendario…</p>
      </section>

      <section v-else-if="!activeCaravan" class="card empty-state">
        <h2>No hay caravana activa</h2>
        <p class="muted">
          Selecciona una caravana desde la vista principal. El calendario está ligado a la caravana activa y usa su progreso real.
        </p>
      </section>

      <template v-else>
        <section class="card current-day-card">
          <div>
            <p class="eyebrow">Caravana activa</p>
            <h2>{{ activeCaravan.name }}</h2>
            <p class="subtitle">{{ currentDateLabel }}</p>
          </div>

          <div class="current-day-actions">
            <button class="secondary-button" type="button" @click="jumpToCampaignToday" :disabled="calendarLoading">
              Hoy de campaña
            </button>
            <button class="secondary-button" type="button" @click="openManualDateModal">
              Actualizar fecha
            </button>
            <button class="secondary-button" type="button" @click="openWeatherConfigModal">
              Generar clima
            </button>
            <button class="primary-button" type="button" @click="openDayCycleModal" :disabled="dayCycleLoading || dayCycleSubmitting">
              {{ dayCycleLoading || dayCycleSubmitting ? "Abriendo…" : "+1 día" }}
            </button>
            <button class="secondary-button" type="button" @click="openMultiDayModal" :disabled="multiDayLoading || multiDaySubmitting">
              {{ multiDayLoading || multiDaySubmitting ? "Abriendo…" : "+N días" }}
            </button>
          </div>
        </section>

        <section class="calendar-layout">
          <article class="card calendar-card">
            <div class="calendar-toolbar">
              <button class="ghost-button" type="button" @click="changeVisibleMonth(-1)" :disabled="calendarLoading || monthView?.displayYear === 4712 && monthView.displayMonth === 1">
                ←
              </button>
              <div>
                <p class="eyebrow">Vista mensual</p>
                <h2>{{ visibleMonthLabel }}</h2>
              </div>
              <div class="calendar-toolbar__actions">
                <button
                  class="switch-button"
                  type="button"
                  role="switch"
                  :aria-checked="showSecretEvents"
                  aria-label="Mostrar secretos"
                  @click="toggleSecretEvents"
                >
                  <span class="switch-button__track" :class="{ 'switch-button__track--on': showSecretEvents }">
                    <span class="switch-button__thumb" />
                  </span>
                  <span class="switch-button__label">{{ showSecretEvents ? "Secretos visibles" : "Secretos ocultos" }}</span>
                </button>
                <button class="secondary-button" type="button" :disabled="calendarLoading" @click="openCustomEventModal">
                  Crear evento
                </button>
                <button class="ghost-button" type="button" @click="changeVisibleMonth(1)" :disabled="calendarLoading || monthView?.displayYear === 4722 && monthView.displayMonth === 12">
                  →
                </button>
              </div>
            </div>

            <div v-if="calendarLoading" class="calendar-loading">
              <p class="muted">Cargando mes…</p>
            </div>

            <div v-else-if="monthView" class="calendar-grid">
              <div v-for="weekday in monthView.weekDayHeaders" :key="weekday" class="weekday">{{ weekday }}</div>

              <button
                v-for="day in monthView.days"
                :key="`${day.date.year}-${day.date.month}-${day.date.day}`"
                type="button"
                class="day-cell"
                :class="{
                  'day-cell--muted': !day.isInCurrentMonth,
                  'day-cell--current': day.isCurrentDay,
                  'day-cell--selected': selectedDay?.date.year === day.date.year && selectedDay?.date.month === day.date.month && selectedDay?.date.day === day.date.day,
                }"
                @click="selectDay(day)"
              >
                <div class="day-cell__header">
                  <span>{{ day.date.day }}</span>
                  <small>{{ day.date.dayOfWeekAbbreviation }}</small>
                </div>
                <ul class="day-cell__events">
                  <li v-for="event in visibleCalendarEvents(day).slice(0, 3)" :key="calendarEventKey(event)">
                    {{ formatCalendarEventName(event.name) }}
                  </li>
                  <li v-if="visibleCalendarEvents(day).length > 3" class="day-cell__more">+{{ visibleCalendarEvents(day).length - 3 }} más</li>
                </ul>
              </button>
            </div>
          </article>
          <article class="card detail-card">
              <div class="section-header">
                <div>
                  <p class="eyebrow">Detalle diario</p>
                  <h2>{{ selectedDateLabel }}</h2>
                </div>
              </div>

              <template v-if="selectedDay">
                <section class="detail-section">
                  <h3>Eventos canónicos</h3>
                  <ul v-if="selectedDay.canonicalEvents.length > 0" class="event-list">
                    <li v-for="event in selectedDay.canonicalEvents" :key="`${event.category}-${event.name}-${event.scope}`" class="event-item">
                      <div class="event-item__title">
                        <strong>{{ formatCalendarEventName(event.name) }}</strong>
                        <span v-if="event.category === 'BIRTHDAY'" class="pill">Cumpleaños</span>
                        <span v-else-if="event.category === 'ASTRONOMICAL'" class="pill">Astronómico</span>
                      </div>
                      <p v-if="event.scope" class="muted">{{ event.scope }}</p>
                      <p v-if="event.description">{{ event.description }}</p>
                    </li>
                  </ul>
                  <p v-else class="muted">No hay eventos canónicos para este día.</p>
                </section>

                <section class="detail-section placeholder-block">
                  <h3>Clima</h3>
                  <div v-if="selectedDay.weather" class="weather-period-list">
                    <article v-for="period in weatherPeriodRows(selectedDay.weather)" :key="period.key" class="weather-period-card">
                      <header class="weather-period-card__header">
                        <strong>{{ period.label }}</strong>
                        <span class="pill pill--weather" :title="formatWeatherToken(period.period?.precipitation)">
                          <img
                            v-if="weatherPrecipitationIcon(period.period?.precipitation)"
                            class="weather-pill__icon"
                            :src="weatherPrecipitationIcon(period.period?.precipitation)?.src ?? ''"
                            :alt="weatherPrecipitationIcon(period.period?.precipitation)?.alt ?? ''"
                          />
                          <span v-else class="weather-pill__empty" aria-hidden="true">∅</span>
                        </span>
                      </header>
                      <dl class="weather-period-metrics">
                        <div>
                          <dt>Viento</dt>
                          <dd class="weather-metric__value" :title="formatWeatherToken(period.period?.windStrength)">
                            <img
                              v-if="weatherWindIcon(period.period?.windStrength)"
                              class="weather-metric__icon"
                              :src="weatherWindIcon(period.period?.windStrength)?.src ?? ''"
                              :alt="weatherWindIcon(period.period?.windStrength)?.alt ?? ''"
                            />
                          </dd>
                        </div>
                        <div>
                          <dt>Temp. ºC</dt>
                          <dd class="weather-temperature__value" :title="weatherTemperatureTooltip(period.period?.temperatureC, period.period?.temperatureF)">
                            <img
                              v-if="weatherTemperatureIcon(period.period?.temperatureF)"
                              class="weather-metric__icon"
                              :src="weatherTemperatureIcon(period.period?.temperatureF)?.src ?? ''"
                              :alt="weatherTemperatureIcon(period.period?.temperatureF)?.alt ?? ''"
                            />
                            <span>{{ period.period?.temperatureC ?? "—" }} ºC</span>
                          </dd>
                        </div>
                      </dl>
                    </article>
                  </div>
                  <p v-else class="muted">
                    No hay clima generado todavía para este día.
                  </p>
                </section>

                <section class="detail-section placeholder-block">
                  <h3>Eventos personalizados</h3>
                  <ul v-if="visibleCustomEvents(selectedDay).length > 0" class="event-list">
                    <li v-for="event in visibleCustomEvents(selectedDay)" :key="calendarEventKey(event)" class="event-item">
                      <div class="event-item__title">
                        <strong>{{ formatCalendarEventName(event.name) }}</strong>
                        <div class="event-item__actions">
                          <span v-if="event.secret" class="pill pill--secret">Secreto</span>
                          <span v-else class="pill pill--custom">Personalizado</span>
                          <button
                            v-if="canDeleteCustomEvent(event)"
                            class="ghost-button event-delete-button"
                            type="button"
                            :disabled="customEventDeletingId === event.id"
                            @click.stop="deleteCustomEvent(event)"
                          >
                            {{ customEventDeletingId === event.id ? "Borrando…" : "Borrar" }}
                          </button>
                        </div>
                      </div>
                      <p v-if="event.description">{{ event.description }}</p>
                    </li>
                  </ul>
                  <p v-if="hiddenCustomEventsCount(selectedDay) > 0" class="muted">
                    Hay {{ hiddenCustomEventsCount(selectedDay) }} evento{{ hiddenCustomEventsCount(selectedDay) === 1 ? "" : "s" }} secreto{{ hiddenCustomEventsCount(selectedDay) === 1 ? "" : "s" }} oculto{{ hiddenCustomEventsCount(selectedDay) === 1 ? "" : "s" }}.
                  </p>
                  <p v-else-if="visibleCustomEvents(selectedDay).length === 0" class="muted">Sin eventos personalizados.</p>
                </section>
              </template>

              <p v-else class="muted">Selecciona un día del calendario para ver el detalle.</p>
            </article>
        </section>
      </template>

      <div v-if="customEventModalOpen" class="modal-backdrop" @click.self="closeCustomEventModal">
        <div class="modal modal-cycle">
          <div class="modal-header">
            <div>
              <p class="eyebrow">Calendario</p>
              <h2>Crear evento personalizado</h2>
            </div>
            <button class="ghost-button" type="button" :disabled="customEventSaving" @click="closeCustomEventModal">
              Cerrar
            </button>
          </div>

          <section class="modal-section">
            <div class="weather-effective-date-grid">
              <label>
                <span>Año</span>
                <select v-model.number="customEventYear">
                  <option v-for="year in supportedYears" :key="year" :value="year">{{ year }} AR</option>
                </select>
              </label>
              <label>
                <span>Mes</span>
                <select v-model.number="customEventMonth">
                  <option v-for="monthOption in supportedMonths" :key="monthOption.value" :value="monthOption.value">
                    {{ monthOption.label }}
                  </option>
                </select>
              </label>
              <label>
                <span>Día</span>
                <select v-model.number="customEventDay">
                  <option v-for="day in customEventDayOptions" :key="day" :value="day">
                    {{ day }}
                  </option>
                </select>
              </label>
            </div>

            <div class="modal-form-grid">
              <label>
                <span>Nombre</span>
                <input v-model="customEventName" type="text" maxlength="200" placeholder="Ej. Reunión de la caravana" />
              </label>
              <label>
                <span>Descripción</span>
                <textarea
                  v-model="customEventDescription"
                  rows="4"
                  maxlength="1000"
                  placeholder="Detalles del evento"
                ></textarea>
              </label>
              <label class="checkbox-field">
                <input v-model="customEventSecret" type="checkbox" />
                <span>Secreto</span>
              </label>
            </div>
          </section>

          <div class="modal-actions">
            <button class="secondary-button" type="button" :disabled="customEventSaving" @click="closeCustomEventModal">
              Cancelar
            </button>
            <button class="primary-button" type="button" :disabled="customEventSaving" @click="saveCustomEvent">
              <span class="button-with-spinner">
                <span v-if="customEventSaving" class="button-spinner" aria-hidden="true"></span>
                <span>{{ customEventSaving ? "Creando…" : "Crear evento" }}</span>
              </span>
            </button>
          </div>
        </div>
      </div>

      <div v-if="manualDateModalOpen" class="modal-backdrop" @click.self="closeManualDateModal">
        <div class="modal modal-cycle">
          <div class="modal-header">
            <div>
              <p class="eyebrow">Control temporal</p>
              <h2>Actualizar fecha actual</h2>
            </div>
            <button class="ghost-button" type="button" :disabled="isPending('manual-date')" @click="closeManualDateModal">
              Cerrar
            </button>
          </div>

          <section class="modal-section">
            <p class="muted">Ajusta la fecha de campaña sin salir del calendario.</p>
            <div class="control-grid">
              <label>
                <span>Año</span>
                <select v-model.number="manualYear">
                  <option v-for="year in supportedYears" :key="year" :value="year">{{ year }} AR</option>
                </select>
              </label>
              <label>
                <span>Mes</span>
                <select v-model.number="manualMonth">
                  <option v-for="month in supportedMonths" :key="month.value" :value="month.value">{{ month.label }}</option>
                </select>
              </label>
              <label>
                <span>Día</span>
                <select v-model.number="manualDay">
                  <option v-for="day in manualDayOptions" :key="day" :value="day">{{ day }}</option>
                </select>
              </label>
            </div>
          </section>

          <div class="modal-actions">
            <button class="secondary-button" type="button" :disabled="isPending('manual-date')" @click="closeManualDateModal">
              Cancelar
            </button>
            <button class="primary-button" type="button" @click="applyManualDate" :disabled="isPending('manual-date')">
              {{ isPending("manual-date") ? "Guardando…" : "Fijar fecha actual" }}
            </button>
          </div>
        </div>
      </div>

      <div v-if="weatherConfigModalOpen" class="modal-backdrop" @click.self="closeWeatherConfigModal">
        <div class="modal modal-cycle">
          <div class="modal-header">
            <div>
              <p class="eyebrow">Clima de campaña</p>
              <h2>Perfil climático</h2>
            </div>
            <button class="ghost-button" type="button" :disabled="weatherProfileSaving" @click="closeWeatherConfigModal">
              Cerrar
            </button>
          </div>

          <p v-if="weatherProfileLoading" class="muted">Cargando configuración climática…</p>

          <template v-else>
            <section class="modal-section">
              <p class="muted">Genera el clima de campaña a partir del perfil activo.</p>
              <div class="weather-effective-date-grid">
                <label>
                  <span>Aplicar desde año</span>
                  <select v-model="weatherEffectiveFromYear">
                    <option v-for="year in supportedYears" :key="year" :value="year">
                      {{ year }}
                    </option>
                  </select>
                </label>

                <label>
                  <span>Aplicar desde mes</span>
                  <select v-model="weatherEffectiveFromMonth">
                    <option v-for="month in supportedMonths" :key="month.value" :value="month.value">
                      {{ month.label }}
                    </option>
                  </select>
                </label>

                <label>
                  <span>Aplicar desde día</span>
                  <select v-model="weatherEffectiveFromDay">
                    <option v-for="day in weatherEffectiveFromDayOptions" :key="day" :value="day">
                      {{ day }}
                    </option>
                  </select>
                </label>
              </div>
              <p class="muted">
                Las fechas anteriores conservarán el clima ya generado. Solo se regenerará desde el día indicado.
              </p>
              <div class="weather-config-grid">
                <label>
                  <span>Baseline</span>
                  <select v-model="weatherClimateBaseline">
                    <option v-for="option in weatherClimateBaselineOptions" :key="option.value" :value="option.value">
                      {{ option.label }}
                    </option>
                  </select>
                  <small class="muted">{{ weatherClimateBaselineDescription() }}</small>
                </label>

                <label>
                  <span>Elevación</span>
                  <select v-model="weatherElevation">
                    <option v-for="option in weatherElevationOptions" :key="option.value" :value="option.value">
                      {{ option.label }}
                    </option>
                  </select>
                  <small class="muted">{{ weatherElevationDescription() }}</small>
                </label>

                <label class="checkbox-field">
                  <input v-model="weatherCrownOfWorld" type="checkbox" />
                  <span>Crown of the World</span>
                </label>
              </div>
            </section>

            <div class="modal-actions">
              <button class="secondary-button" type="button" :disabled="weatherProfileSaving" @click="closeWeatherConfigModal">
                Cancelar
              </button>
              <button class="primary-button" type="button" @click="saveWeatherProfile" :disabled="weatherProfileSaving || !activeCaravan">
                {{ weatherProfileSaving ? "Guardando…" : "Guardar clima" }}
              </button>
            </div>
          </template>
        </div>
      </div>

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

      <div v-if="multiDayModalOpen" class="modal-backdrop" @click.self="closeMultiDayModal">
        <div class="modal modal-cycle">
          <div class="modal-header">
            <div>
              <p class="eyebrow">Ciclo diario</p>
              <h2>Pasar varios días</h2>
            </div>
            <button class="ghost-button" type="button" :disabled="multiDaySubmitting" @click="closeMultiDayModal">
              Cerrar
            </button>
          </div>

          <template v-if="!multiDayPreview">
            <section class="multi-day-form">
              <label class="field">
                <span>Días a simular</span>
                <input v-model.number="multiDayRequestedDays" type="number" min="1" max="30" step="1" />
              </label>
              <p class="muted">La simulación encadena cada jornada sobre el resultado de la anterior.</p>
            </section>

            <div class="modal-actions">
              <button class="secondary-button" type="button" :disabled="multiDayLoading || multiDaySubmitting" @click="closeMultiDayModal">
                Cancelar
              </button>
              <button class="primary-button" type="button" :disabled="multiDayLoading || multiDaySubmitting" @click="previewMultipleDays">
                <span class="button-with-spinner">
                  <span v-if="multiDayLoading" class="button-spinner" aria-hidden="true"></span>
                  <span>{{ multiDayLoading ? "Simulando…" : "Simular varios días" }}</span>
                </span>
              </button>
            </div>
          </template>

          <template v-else>
            <section class="day-cycle-summary" :class="{ danger: multiDayPreview.daysWithUncoveredConsumption > 0 }">
              <div class="day-cycle-summary__copy">
                <p class="eyebrow">Resultado acumulado</p>
                <h3>
                  {{ multiDayPreview.daysWithUncoveredConsumption === 0 ? "Simulación completa" : "Hay días con consumo no cubierto" }}
                </h3>
                <p class="day-cycle-summary__text">
                  Del día {{ multiDayPreview.startDayIndex }} al {{ multiDayPreview.endDayIndex }},
                  la caravana necesita {{ formatDecimal(multiDayPreview.totalRequiredConsumption) }} de comida
                  y genera {{ formatDecimal(multiDayPreview.totalGeneratedFood) }}.
                </p>
              </div>

              <div class="day-cycle-summary__chips">
                <span class="day-cycle-chip">Días {{ multiDayPreview.requestedDays }}</span>
                <span class="day-cycle-chip">Suministros usados {{ multiDayPreview.totalSuppliesConsumed }}</span>
                <span class="day-cycle-chip">Alertas {{ multiDayPreview.daysWithUncoveredConsumption }}</span>
              </div>
            </section>

            <section class="day-cycle-layout">
              <article class="day-cycle-panel">
                <header class="day-cycle-panel__header">
                  <div>
                    <p class="eyebrow">Producción total</p>
                    <h4>Recursos acumulados</h4>
                  </div>
                </header>
                <dl class="day-cycle-metrics">
                  <div>
                    <dt>Agricultores</dt>
                    <dd>{{ multiDayPreview.totalGeneratedSuppliesFromAgricultors }}</dd>
                  </div>
                  <div>
                    <dt>Boticarios</dt>
                    <dd>{{ formatDecimal(multiDayPreview.totalGeneratedAlchemyValueFromBoticarios) }}</dd>
                  </div>
                  <div>
                    <dt>Comida total</dt>
                    <dd>{{ formatDecimal(multiDayPreview.totalGeneratedFood) }}</dd>
                  </div>
                </dl>
              </article>

              <article class="day-cycle-panel">
                <header class="day-cycle-panel__header">
                  <div>
                    <p class="eyebrow">Estado final</p>
                    <h4>Inventario</h4>
                  </div>
                </header>
                <dl class="day-cycle-metrics">
                  <div>
                    <dt>Suministros</dt>
                    <dd>{{ multiDayPreview.finalSupplyUnits }}</dd>
                  </div>
                  <div>
                    <dt>Perecederos</dt>
                    <dd>{{ multiDayPreview.finalPerishableUnits }}</dd>
                  </div>
                  <div>
                    <dt>Comida perecedera</dt>
                    <dd>{{ formatDecimal(multiDayPreview.finalPerishableFood) }}</dd>
                  </div>
                </dl>
              </article>
            </section>

            <section v-if="multiDayPreview.warnings.length" class="warning-banner danger">
              <strong>Avisos</strong>
              <ul class="simple-list">
                <li v-for="warning in multiDayPreview.warnings" :key="warning">{{ warning }}</li>
              </ul>
            </section>

            <div class="modal-actions">
              <button class="secondary-button" type="button" :disabled="multiDaySubmitting" @click="closeMultiDayModal">
                Cancelar
              </button>
              <button class="primary-button" type="button" :disabled="multiDaySubmitting" @click="confirmMultipleDays">
                <span class="button-with-spinner">
                  <span v-if="multiDaySubmitting" class="button-spinner" aria-hidden="true"></span>
                  <span>{{ multiDaySubmitting ? "Confirmando…" : "Aceptar pasar esos días" }}</span>
                </span>
              </button>
            </div>
          </template>
        </div>
      </div>
    </section>
  </main>
</template>

<style scoped>
.page {
  min-height: 100vh;
  padding: 2rem;
}

.shell {
  width: min(1380px, 100%);
  margin: 0 auto;
  display: grid;
  gap: 1.25rem;
}

.hero,
.section-header,
.calendar-toolbar,
.current-day-card,
.current-day-actions,
.control-actions {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
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
  color: #64748b;
}

.card {
  padding: 1.25rem;
  border: 1px solid #e5e7eb;
  border-radius: 1rem;
  background: white;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.08);
}

.error {
  padding: 0.85rem 1rem;
  border-radius: 0.85rem;
  background: #fef2f2;
  color: #b91c1c;
}

.empty-state {
  display: grid;
  gap: 0.5rem;
}

.primary-button,
.secondary-button,
.ghost-button,
.day-cell {
  border-radius: 0.85rem;
  font: inherit;
}

.primary-button,
.secondary-button,
.ghost-button {
  padding: 0.75rem 0.95rem;
  border: 1px solid #cbd5e1;
  cursor: pointer;
}

.primary-button {
  background: #1d4ed8;
  border-color: #1d4ed8;
  color: white;
}

.secondary-button,
.ghost-button {
  background: white;
}

.button-with-spinner {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
}

.button-spinner {
  width: 0.85rem;
  height: 0.85rem;
  border-radius: 999px;
  border: 2px solid currentColor;
  border-right-color: transparent;
  animation: spin 0.7s linear infinite;
}

.current-day-card {
  background:
    linear-gradient(180deg, rgba(239, 246, 255, 0.94), rgba(255, 255, 255, 0.98)),
    radial-gradient(circle at top right, rgba(29, 78, 216, 0.18), transparent 35%);
  border-color: rgba(37, 99, 235, 0.2);
}

.calendar-layout {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(320px, 1fr);
  gap: 1.25rem;
  align-items: start;
}

.calendar-toolbar__actions {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.detail-card {
  display: grid;
  gap: 1rem;
}

.control-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.75rem;
}

label {
  display: grid;
  gap: 0.35rem;
}

select,
input,
textarea {
  width: 100%;
  padding: 0.8rem 0.9rem;
  border: 1px solid #d1d5db;
  border-radius: 0.75rem;
  font: inherit;
  resize: vertical;
}

.calendar-card {
  display: grid;
  gap: 1rem;
}

.calendar-loading {
  min-height: 22rem;
  display: grid;
  place-items: center;
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 0.65rem;
}

.weekday {
  text-align: center;
  font-size: 0.85rem;
  font-weight: 700;
  color: #475569;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  padding-bottom: 0.25rem;
}

.day-cell {
  min-height: 8.5rem;
  padding: 0.75rem;
  border: 1px solid #e5e7eb;
  background: #fff;
  text-align: left;
  display: grid;
  gap: 0.6rem;
  align-content: start;
  cursor: pointer;
  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease,
    transform 0.15s ease;
}

.day-cell:hover,
.day-cell:focus-visible {
  border-color: #93c5fd;
  box-shadow: 0 10px 24px rgba(59, 130, 246, 0.12);
  transform: translateY(-1px);
}

.day-cell--muted {
  background: #f8fafc;
  color: #94a3b8;
}

.day-cell--current {
  border-color: #2563eb;
  background:
    linear-gradient(180deg, rgba(239, 246, 255, 0.95), rgba(255, 255, 255, 1)),
    radial-gradient(circle at top right, rgba(37, 99, 235, 0.14), transparent 35%);
}

.day-cell--selected {
  box-shadow: 0 0 0 2px rgba(15, 23, 42, 0.16);
}

.day-cell__header {
  display: flex;
  justify-content: space-between;
  gap: 0.5rem;
  align-items: baseline;
  color: #0f172a;
  font-weight: 700;
}

.day-cell__events {
  margin: 0;
  padding-left: 1rem;
  display: grid;
  gap: 0.25rem;
  color: #334155;
  font-size: 0.86rem;
}

.day-cell__more {
  color: #2563eb;
  font-weight: 700;
}

.detail-section {
  display: grid;
  gap: 0.75rem;
}

.event-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 0.75rem;
}

.event-item {
  padding: 0.9rem;
  border: 1px solid #e5e7eb;
  border-radius: 0.9rem;
  display: grid;
  gap: 0.35rem;
  background: #f8fafc;
}

.event-item__title {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  align-items: center;
}

.event-item__actions {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.event-delete-button {
  padding: 0.35rem 0.55rem;
  border-radius: 999px;
  font-size: 0.8rem;
  color: #475569;
}

.pill {
  padding: 0.25rem 0.6rem;
  border-radius: 999px;
  background: #dbeafe;
  color: #1d4ed8;
  font-size: 0.8rem;
  font-weight: 700;
}

.pill--custom {
  background: #dcfce7;
  color: #166534;
}

.pill--secret {
  background: #fee2e2;
  color: #b91c1c;
}

.switch-button {
  display: inline-flex;
  align-items: center;
  gap: 0.65rem;
  padding: 0.35rem 0.25rem;
  border: 0;
  background: transparent;
  color: #334155;
  cursor: pointer;
  font: inherit;
}

.switch-button:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.switch-button:focus-visible {
  outline: 2px solid rgba(37, 99, 235, 0.35);
  outline-offset: 4px;
  border-radius: 999px;
}

.switch-button__track {
  width: 3.1rem;
  height: 1.8rem;
  padding: 0.2rem;
  border-radius: 999px;
  background: #cbd5e1;
  display: inline-flex;
  align-items: center;
  justify-content: flex-start;
  transition:
    background-color 0.15s ease,
    box-shadow 0.15s ease;
  box-shadow: inset 0 0 0 1px rgba(15, 23, 42, 0.06);
}

.switch-button__track--on {
  background: #1d4ed8;
}

.switch-button__thumb {
  width: 1.4rem;
  height: 1.4rem;
  border-radius: 999px;
  background: white;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.28);
  transform: translateX(0);
  transition: transform 0.15s ease;
}

.switch-button__track--on .switch-button__thumb {
  transform: translateX(1.3rem);
}

.switch-button__label {
  font-size: 0.9rem;
  font-weight: 600;
  color: #475569;
}

.placeholder-block {
  padding-top: 0.25rem;
  border-top: 1px dashed #cbd5e1;
}

.placeholder-list {
  margin: 0;
  padding-left: 1.1rem;
  color: #475569;
  display: grid;
  gap: 0.25rem;
}

.weather-config-card {
  display: grid;
  gap: 1rem;
}

.modal-form-grid {
  display: grid;
  gap: 0.85rem;
}

.weather-config-grid {
  display: grid;
  gap: 0.85rem;
}

.weather-effective-date-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.75rem;
}

.checkbox-field {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.85rem 0.95rem;
  border: 1px solid #e5e7eb;
  border-radius: 0.9rem;
  background: #f8fafc;
}

.checkbox-field input {
  width: auto;
}

.weather-period-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
}

.weather-period-card {
  display: grid;
  gap: 0.75rem;
  padding: 0.9rem;
  border-radius: 0.9rem;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  min-width: 0;
}

.weather-period-card__header {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  align-items: center;
  min-width: 0;
  flex-wrap: wrap;
}

.weather-period-card__header strong {
  min-width: 0;
  flex: 1 1 auto;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pill--weather {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
}

.pill--weather {
  background: #ede9fe;
  color: #6d28d9;
}

.weather-pill__icon,
.weather-metric__icon {
  width: 1.15rem;
  height: 1.15rem;
  object-fit: contain;
  flex: none;
}

.weather-pill__empty {
  display: inline-grid;
  place-items: center;
  width: 1.15rem;
  height: 1.15rem;
  font-size: 0.85rem;
  font-weight: 700;
  line-height: 1;
  color: #6d28d9;
}

.weather-metric__value {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  width: fit-content;
  justify-content: center;
}

.weather-period-metrics {
  display: grid;
  grid-template-columns: minmax(0, 0.92fr) minmax(0, 1.45fr);
  gap: 0.5rem;
  margin: 0;
}

.weather-period-metrics div {
  padding: 0.7rem;
  border-radius: 0.85rem;
  background: white;
  display: grid;
  gap: 0.35rem;
  justify-items: center;
  text-align: center;
  align-content: center;
}

.weather-period-metrics dt {
  font-size: 0.72rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: #64748b;
}

.weather-period-metrics dd {
  margin: 0.25rem 0 0;
  font-size: 0.95rem;
  font-weight: 800;
  color: #0f172a;
}

.weather-temperature__value {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  white-space: nowrap;
  min-width: 4.75rem;
  justify-content: center;
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

.modal-section {
  display: grid;
  gap: 1rem;
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
.day-cycle-panel__header h4 {
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
  position: relative;
  overflow: hidden;
}

.day-cycle-panel::before {
  content: "";
  position: absolute;
  inset: 0 auto 0 0;
  width: 0.35rem;
  background: #cbd5e1;
}

.day-cycle-panel__header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
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

.multi-day-form {
  display: grid;
  gap: 1rem;
}

.field {
  display: grid;
  gap: 0.5rem;
}

.field input {
  width: 100%;
  border: 1px solid #cbd5e1;
  border-radius: 0.9rem;
  padding: 0.8rem 0.9rem;
  font: inherit;
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

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 1200px) {
  .calendar-layout {
    grid-template-columns: 1fr;
  }

  .weather-period-list {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .page {
    padding: 1rem;
  }

  .hero,
  .section-header,
  .calendar-toolbar,
  .current-day-card,
  .current-day-actions,
  .detail-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .calendar-toolbar__actions {
    justify-content: flex-start;
  }

  .control-grid {
    grid-template-columns: 1fr;
  }

  .calendar-grid {
    grid-template-columns: minmax(0, 0.65fr) minmax(0, 1.35fr);
  }

  .weekday {
    display: none;
  }

  .weather-effective-date-grid,
  .day-cycle-layout,
  .day-cycle-metrics {
    grid-template-columns: 1fr;
  }

  .day-cycle-summary,
  .modal-header,
  .day-cycle-summary__chips {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
