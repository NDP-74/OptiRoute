<template>
  <div class="min-h-[140px] border-r border-slate-200 bg-slate-50/60 p-2">
    <div v-if="calendarItems.length > 0" class="flex flex-col gap-2">
      <TransportCard v-for="item in calendarItems" :key="item.id" :transport="item.transport"
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
    id: transport.id,
    sortTime: new Date(transport.plannedStart).getTime(),
    transport,
  }))
  .sort((first, second) => first.sortTime - second.sortTime));

const emit = defineEmits<{
  "transport-select": [transportId: number];
}>();
</script>
