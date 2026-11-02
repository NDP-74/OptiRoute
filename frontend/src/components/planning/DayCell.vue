<template>
  <div class="min-h-[140px] border-r border-slate-200 bg-slate-50/60 p-2">
    <div v-if="calendarItems.length > 0" class="flex flex-col gap-2">
      <TransportCard v-for="item in calendarItems" :key="item.routeId" :transport="item.transport"
      @select="emit('transport-select', $event)" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";

import TransportCard from "@/components/planning/TransportCard.vue";

import type { PlanningTransport } from "@/models/planning/planning";

const props = defineProps<{
  transports: PlanningTransport[];
}>();

const calendarItems = computed(() => props.transports
  .map((transport) => ({
    routeId: transport.routeId,
    sortTime: transport.departureStart ? new Date(transport.departureStart).getTime() : Number.MAX_SAFE_INTEGER,
    transport,
  }))
  .sort((first, second) => first.sortTime - second.sortTime));

const emit = defineEmits<{
  "transport-select": [selection: { serviceId: number; routeId: number }];
}>();
</script>
