<template>
  <div class="min-h-0 flex-1 overflow-auto bg-white">
    <PlanningHeader :days="days" />

    <div v-if="loading" class="flex min-h-[240px] items-center justify-center">
      <div class="flex items-center gap-3 text-sm text-slate-500">
        <div class="h-5 w-5 animate-spin rounded-full border-2 border-slate-300 border-t-slate-700" />

        Chargement du planning...
      </div>
    </div>

    <div v-else-if="error" class="flex min-h-[240px] flex-col items-center justify-center gap-4 px-6 text-center">
      <div>
        <p class="font-medium text-slate-800">
          Le planning n’a pas pu être chargé
        </p>

        <p class="mt-1 text-sm text-slate-500">
          {{ error }}
        </p>
      </div>

      <button type="button"
        class="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 shadow-sm transition hover:bg-slate-50"
        @click="emit('retry')">
        Réessayer
      </button>
    </div>

    <div v-else-if="drivers.length === 0" class="flex min-h-[240px] items-center justify-center px-6 text-center">
      <div>
        <p class="font-medium text-slate-700">
          Aucun transport planifié
        </p>

        <p class="mt-1 text-sm text-slate-500">
          Aucun transport n’est présent pour cette période.
        </p>
      </div>
    </div>

    <div v-else>
      <PlanningRow v-for="driver in drivers" :key="driver.id" :driver="driver" :days="days"
        @transport-select="emit('transport-select', $event)" />

      <PlanningTotalRow :total-cost="totalCost" :day-count="days.length" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";

import PlanningRow from "@/components/planning/PlanningRow.vue";
import PlanningHeader from "@/components/planning/PlanningHeader.vue";
import PlanningTotalRow from "@/components/planning/PlanningTotalRow.vue";

import type {
  PlanningDay,
  PlanningDriver,
} from "@/models/planning/planning";

const props = defineProps<{
  drivers: PlanningDriver[];
  days: PlanningDay[];
  loading: boolean;
  error: string | null;
}>();

const emit = defineEmits<{
  retry: [];
  "transport-select": [transportId: number];
}>();

const totalCost = computed<number>(() => {
  return props.drivers.reduce((total, driver) => total + driver.totalCost, 0);
});

</script>