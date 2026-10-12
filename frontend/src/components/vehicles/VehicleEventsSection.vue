<script setup lang="ts">
import { MapPin } from "lucide-vue-next"

import type { EventResponse } from "@/models/Event"

import { formatDateTime } from "@/utils/formatters"

defineProps<{
    events: EventResponse[]
    loading: boolean
    error: string | null
}>()
</script>

<template>
    <section class="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
        <div class="flex items-center justify-between">
            <h3 class="text-sm font-semibold text-slate-900">
                Événements
            </h3>

            <span v-if="!loading && !error && events.length > 0"
                class="rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-medium text-slate-600">
                {{ events.length }}
            </span>
        </div>

        <p v-if="loading" class="mt-3 text-sm text-slate-500">
            Chargement des événements...
        </p>

        <p v-else-if="error" class="mt-3 text-sm text-red-600">
            {{ error }}
        </p>

        <p v-else-if="events.length === 0" class="mt-3 text-sm text-slate-500">
            Aucun événement assigné à ce véhicule.
        </p>

        <ul v-else class="mt-4 space-y-2">
            <li v-for="event in events" :key="event.id"
                class="flex items-start justify-between gap-3 rounded-xl border border-slate-100 bg-slate-50/60 px-3 py-2.5">
                <div class="min-w-0">
                    <p class="truncate text-sm font-medium text-slate-900">
                        {{ event.costParameterLabel }}
                    </p>

                    <p class="mt-0.5 flex items-center gap-1 truncate text-xs text-slate-500">
                        <MapPin v-if="event.pointOfInterestLabel" class="h-3.5 w-3.5 shrink-0" />
                        {{ event.pointOfInterestLabel ?? "Sans lieu" }}
                    </p>
                </div>

                <div class="shrink-0 text-right">
                    <span
                        class="inline-flex rounded-full border border-slate-200 bg-white px-2 py-0.5 text-xs font-medium text-slate-600">
                        {{ event.tractorId !== null ? "Tracteur" : "Semi-remorque" }}
                    </span>

                    <p class="mt-1 text-xs text-slate-400">
                        {{ formatDateTime(event.eventDate) }}
                    </p>
                </div>
            </li>
        </ul>
    </section>
</template>
