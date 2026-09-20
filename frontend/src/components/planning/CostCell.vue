<template>
  <div class="sticky left-[240px] z-20 flex min-h-[140px] items-center border-r border-slate-300 bg-white px-3 py-4">
    <div class="w-full space-y-3 text-center">
      <div v-if="!isUnassigned">
        <p class="text-[11px] text-slate-400">Coûts</p>
        <p class="mt-0.5 text-sm font-semibold text-slate-800">{{ formattedTotalCost }}</p>
      </div>

      <div v-if="!isUnassigned" class="border-t border-slate-100 pt-3">
        <p class="text-[11px] text-slate-400">Chiffre d'affaires</p>
        <p class="mt-0.5 text-sm font-semibold text-slate-800">{{ formattedTotalRevenue }}</p>
      </div>

      <div :class="!isUnassigned && 'border-t border-slate-100 pt-3'">
        <p class="text-[11px] text-slate-400">Résultat</p>
        <p class="mt-0.5 text-sm font-semibold" :class="result >= 0 ? 'text-emerald-700' : 'text-red-600'">
          {{ formattedResult }}
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";

const props = defineProps<{
  totalCost: number;
  totalRevenue: number;
  isUnassigned: boolean;
}>();

const currencyFormatter = new Intl.NumberFormat("fr-FR", {
  style: "currency",
  currency: "EUR",
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

const formattedTotalCost = computed<string>(() => {
  return currencyFormatter.format(props.totalCost);
});

const formattedTotalRevenue = computed<string>(() => {
  return currencyFormatter.format(props.totalRevenue);
});

const result = computed<number>(() => props.totalRevenue - props.totalCost);

const formattedResult = computed<string>(() => {
  return currencyFormatter.format(result.value);
});

</script>