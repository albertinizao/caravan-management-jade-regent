<script setup lang="ts">
import { computed, useSlots } from "vue";

type SummaryStat = {
  label: string;
  value: string | number;
};

type SummaryMeter = {
  ariaLabel: string;
  title?: string;
  currentValue: string | number;
  currentLabel: string;
  maxValue: string | number;
  maxLabel: string;
  segmentWidth: string;
  segmentClass: string;
};

const props = defineProps<{
  eyebrow: string;
  title: string;
  description?: string | null;
  stats: SummaryStat[];
  meter: SummaryMeter;
  actionLabel?: string | null;
}>();

const emit = defineEmits<{
  action: [];
}>();

const slots = useSlots();
const hasAction = computed(() => Boolean(props.actionLabel || slots.action));
</script>

<template>
  <section class="caravan-summary-hero">
    <div class="caravan-summary-hero__copy">
      <p class="caravan-summary-hero__eyebrow">{{ eyebrow }}</p>
      <h2 class="caravan-summary-hero__title">{{ title }}</h2>
      <p v-if="description" class="caravan-summary-hero__description">{{ description }}</p>
    </div>

    <div class="caravan-summary-hero__right">
      <div class="caravan-summary-hero__top">
        <div class="caravan-summary-hero__stats">
          <div v-for="stat in stats" :key="stat.label" class="caravan-summary-hero__stat">
            <span>{{ stat.label }}</span>
            <strong>{{ stat.value }}</strong>
          </div>
        </div>

        <div v-if="hasAction" class="caravan-summary-hero__action">
          <slot name="action">
            <button class="primary-button" type="button" @click="emit('action')">
              {{ actionLabel }}
            </button>
          </slot>
        </div>
      </div>

      <div class="summary-meter-block summary-meter-block--compact">
        <div
          class="meter-strip"
          :aria-label="meter.ariaLabel"
          :title="meter.title ?? meter.ariaLabel"
        >
          <span
            class="meter-segment"
            :class="meter.segmentClass"
            :style="{ width: meter.segmentWidth }"
          ></span>
        </div>
        <div class="meter-values meter-values--compact">
          <span><strong>{{ meter.currentValue }}</strong> {{ meter.currentLabel }}</span>
          <span><strong>{{ meter.maxValue }}</strong> {{ meter.maxLabel }}</span>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.caravan-summary-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.9fr);
  gap: 1.25rem;
  align-items: start;
  padding: 1.25rem;
  border: 1px solid #e5e7eb;
  border-radius: 1rem;
  background: white;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.08);
}

.caravan-summary-hero__copy {
  display: grid;
  gap: 0.35rem;
  align-self: center;
}

.caravan-summary-hero__eyebrow {
  margin: 0;
  font-size: 0.875rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #6b7280;
}

.caravan-summary-hero__title {
  margin: 0;
  font-size: 1.4rem;
  line-height: 1.15;
  color: #111827;
}

.caravan-summary-hero__description {
  margin: 0;
  color: #6b7280;
}

.caravan-summary-hero__right {
  display: grid;
  gap: 0.85rem;
}

.caravan-summary-hero__top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.caravan-summary-hero__stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.75rem;
  flex: 1;
}

.caravan-summary-hero__stat {
  padding: 0.85rem;
  border-radius: 0.85rem;
  background: #f8fafc;
  min-width: 120px;
}

.caravan-summary-hero__stat span {
  display: block;
  font-size: 0.8rem;
  color: #6b7280;
}

.caravan-summary-hero__stat strong {
  display: block;
  margin-top: 0.2rem;
  font-size: 1.1rem;
  color: #111827;
}

.caravan-summary-hero__action {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.summary-meter-block {
  display: grid;
  gap: 0.5rem;
}

.summary-meter-block--compact {
  margin-bottom: 0;
}

.meter-strip {
  display: flex;
  overflow: hidden;
  min-height: 0.9rem;
  border-radius: 999px;
  background: #e5e7eb;
}

.meter-segment {
  display: block;
  height: 100%;
}

.meter-segment--travelers {
  background: linear-gradient(90deg, #bfdbfe 0%, #2563eb 100%);
}

.meter-segment--cargo {
  background: linear-gradient(90deg, #fde68a 0%, #f59e0b 100%);
}

.meter-segment--draft {
  background: linear-gradient(90deg, #c4b5fd 0%, #7c3aed 100%);
}

.meter-segment--discontent {
  background: linear-gradient(90deg, #f59e0b 0%, #ef4444 100%);
}

.meter-values {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  flex-wrap: wrap;
  font-size: 0.78rem;
  color: #475569;
}

.meter-values strong {
  color: #111827;
}

.meter-values--compact {
  font-size: 0.76rem;
}

@media (max-width: 1100px) {
  .caravan-summary-hero,
  .caravan-summary-hero__top {
    grid-template-columns: 1fr;
  }

  .caravan-summary-hero__top {
    display: grid;
  }

  .caravan-summary-hero__action {
    justify-content: flex-start;
  }
}

@media (max-width: 720px) {
  .caravan-summary-hero {
    padding: 1rem;
  }

  .caravan-summary-hero__stats {
    grid-template-columns: 1fr;
  }
}
</style>
