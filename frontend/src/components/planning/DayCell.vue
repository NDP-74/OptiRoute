<template>
  <div class="min-h-[140px] border-r border-slate-200 bg-slate-50/60 p-2">
    <div v-if="calendarItems.length > 0" class="flex flex-col gap-2">
      <template v-for="item in calendarItems" :key="`${item.type}-${item.id}`">
        <TransportCard v-if="item.type === 'transport'" :transport="item.transport"
          @select="emit('transport-select', $event)" />
        <EventCard v-else :event="item.event" @select="emit('event-select', $event)" />
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";

import TransportCard from "@/components/planning/TransportCard.vue";
import EventCard from "@/components/planning/EventCard.vue";

import type { PlanningTransport } from "@/models/planning/planning";
import type { VehicleEventResponse } from "@/models/vehicle/VehicleEvent";

const props = defineProps<{
  transports: PlanningTransport[];
  events: VehicleEventResponse[];
}>();

const calendarItems = computed(() => [
  ...props.transports.map((transport) => ({
    type: "transport" as const,
    id: transport.id,
    sortTime: new Date(transport.plannedStart).getTime(),
    transport,
  })),
  ...props.events.map((event) => ({
    type: "event" as const,
    id: event.id,
    sortTime: new Date(event.eventDate).getTime(),
    event,
  })),
].sort((first, second) => first.sortTime - second.sortTime));

const emit = defineEmits<{
  "transport-select": [transportId: number];
  "event-select": [eventId: number];
}>();
</script>
