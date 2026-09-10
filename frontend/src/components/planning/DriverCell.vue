<template>
  <div class="sticky left-0 z-20 flex min-h-[140px] items-start border-r border-slate-300 bg-white p-4">
    <div class="min-w-0">
      <div class="flex items-center gap-3">
        <div
          class="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-slate-100 text-sm font-semibold text-slate-600">
          {{ initials }}
        </div>

        <div class="min-w-0">
          <p class="truncate text-sm font-semibold text-slate-800">
            {{ driverName }}
          </p>

          <p class="mt-0.5 text-xs text-slate-500">
            {{ transportCountLabel }}
          </p>

          <p class="mt-1 truncate text-xs text-slate-500">
            {{ vehiclesLabel }}
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";

const props = defineProps<{
  driverName: string;
  transportCount: number;
  vehicleRegistrations: string[];
}>();

const initials = computed<string>(() => {
  return props.driverName
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part.charAt(0).toUpperCase())
    .join("");
});

const vehiclesLabel = computed<string>(() => {
  if (props.vehicleRegistrations.length === 0) {
    return "Aucun véhicule assigné";
  }

  return props.vehicleRegistrations.join(" — ");
});

const transportCountLabel = computed<string>(() => {
  if (props.transportCount === 0) {
    return "Aucun transport";
  }

  if (props.transportCount === 1) {
    return "1 transport";
  }

  return `${props.transportCount} transports`;
});
</script>