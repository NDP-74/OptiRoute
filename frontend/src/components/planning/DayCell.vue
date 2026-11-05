<template>
  <div :class="[
    'relative min-h-[140px] border-r border-slate-200 p-2 transition-colors',
    selected ? 'bg-blue-100 ring-1 ring-inset ring-blue-300' : 'bg-slate-50/60',
    selectionEnabled ? 'cursor-pointer hover:bg-blue-50' : ''
  ]" @click="handleCellClick">
    <label v-if="selectionEnabled" :for="checkboxId" class="absolute right-2 top-2 z-10 flex cursor-pointer items-center gap-2 rounded-md bg-white/90 p-1.5 text-xs text-slate-700 shadow-sm">
      <span class="sr-only">{{ selectionLabel }}</span>
      <input :id="checkboxId" type="checkbox" :checked="selected"
        class="h-4 w-4 cursor-pointer rounded border-slate-300 text-blue-600 focus:ring-blue-500"
        :aria-label="selectionLabel" @click.stop @change="emit('selection-toggle')" />
    </label>

    <div v-if="calendarItems.length > 0" :class="['flex flex-col gap-2', selectionEnabled ? 'pt-7' : '']">
      <TransportCard v-for="item in calendarItems" :key="item.routeId" :transport="item.transport"
      :selection-mode="selectionEnabled"
      @select="handleTransportSelect" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useId } from "vue";

import TransportCard from "@/components/planning/TransportCard.vue";

import type { PlanningTransport } from "@/models/planning/planning";

const props = defineProps<{
  transports: PlanningTransport[];
  selectionEnabled: boolean;
  selected: boolean;
  selectionLabel: string;
}>();

const emit = defineEmits<{
  "transport-select": [selection: { serviceId: number; routeId: number }];
  "selection-toggle": [];
}>();

const checkboxId = useId();

function handleCellClick(): void {
  if (props.selectionEnabled) {
    emit("selection-toggle");
  }
}

function handleTransportSelect(selection: { serviceId: number; routeId: number }): void {
  if (!props.selectionEnabled) {
    emit("transport-select", selection);
  }
}

const calendarItems = computed(() => props.transports
  .map((transport) => ({
    routeId: transport.routeId,
    sortTime: transport.departureStart ? new Date(transport.departureStart).getTime() : Number.MAX_SAFE_INTEGER,
    transport,
  }))
  .sort((first, second) => first.sortTime - second.sortTime));
</script>
