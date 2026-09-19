<script setup lang="ts">
import { Container, Truck, UserRound } from "lucide-vue-next";

const props = defineProps<{
  driverName: string;
  vehicleRegistrations: string[];
  isUnassigned: boolean;
}>();

</script>

<template>
  <div class="sticky left-0 z-20 flex min-h-[140px] items-start border-r border-slate-300 bg-white p-4">
    <div class="min-w-0">
      <div class="flex items-center gap-3">
        <div class="min-w-0">
          <span class="flex min-w-0 items-center gap-1.5">
            <Truck v-if="isUnassigned" :size="16" class="shrink-0 text-slate-400" />
            <UserRound v-else :size="16" class="shrink-0 text-slate-400" />
            <span class="truncate text-sm font-semibold text-slate-800">{{ driverName }}</span>
          </span>

          <p v-if="isUnassigned" class="mt-3 truncate text-xs text-slate-500">
            {{ vehicleRegistrations.length }} véhicule{{ vehicleRegistrations.length > 1 ? "s" : "" }} sans trajet.{{
              vehicleRegistrations.length > 1 ? "s" : "" }}
          </p>

          <div v-else-if="vehicleRegistrations.length" class="mt-3 flex items-center gap-3 text-xs text-slate-600">
            <span v-if="vehicleRegistrations[0]" class="flex min-w-0 items-center gap-1.5">
              <Truck :size="14" class="shrink-0 text-slate-400" />
              <span class="truncate">{{ vehicleRegistrations[0] }}</span>
            </span>

            <span v-if="vehicleRegistrations[1]" class="flex min-w-0 items-center gap-1.5">
              <Container :size="14" class="shrink-0 text-slate-400" />
              <span class="truncate">{{ vehicleRegistrations[1] }}</span>
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>