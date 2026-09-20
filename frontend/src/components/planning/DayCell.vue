<template>
  <div class="min-h-[140px] border-r border-slate-200 bg-slate-50/60 p-2">
    <div v-if="transports.length > 0 || events.length > 0" class="flex flex-col gap-2">
      <TransportCard v-for="transport in transports" :key="transport.id" :transport="transport"
        @select="emit('transport-select', $event)" />
      <EventCard v-for="event in events" :key="event.id" :event="event" @select="emit('event-select', $event)" />
    </div>
  </div>
</template>

<script setup lang="ts">
import TransportCard from "@/components/planning/TransportCard.vue";
import EventCard from "@/components/planning/EventCard.vue";

import type { PlanningTransport } from "@/models/planning/planning";
import type { VehicleEventResponse } from "@/models/vehicle/VehicleEvent";

defineProps<{
  transports: PlanningTransport[];
  events: VehicleEventResponse[];
}>();

const emit = defineEmits<{
  "transport-select": [transportId: number];
  "event-select": [eventId: number];
}>();
</script>
