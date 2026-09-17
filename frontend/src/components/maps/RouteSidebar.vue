<script setup lang="ts">
import { computed, ref } from 'vue'
import RouteForm from './RouteForm.vue'

import {
    ChevronLeft,
    ChevronRight,
    Fuel,
    Receipt,
    Car
} from 'lucide-vue-next'

import PlanningRouteModal from '@/components/planning/PlanningRouteModal.vue'

import type { RouteRequest, RouteResponse } from '@/models/route/Route'

import { formatDurationSeconds } from "@/utils/formatters"


const props = defineProps<{
    routeResponse?: RouteResponse
    selectedIndex?: number
}>()

const emit = defineEmits([
    'route-calculated',
    'route-selected'
])

const open = ref(true)
const showAssignModal = ref(false)

const routeRequest = ref<RouteRequest>()

function getRouteSpecificities(): string {
    if (routeRequest.value?.mode === "CHEAPEST") {
        return "Route la plus économique"
    }
    return "Route la plus rapide"
}

const onRouteCalculated = (data: { response: RouteResponse, request: RouteRequest }) => {
    routeRequest.value = data.request

    emit('route-calculated', data)
}

function selectRoute(index: number) {
    emit('route-selected', index)
}
</script>

<template>
    <div class="relative h-full">
        <div :class="[
            'h-full w-[400px] shrink-0 bg-white shadow-2xl border-r',
            'transition-all duration-300 overflow-hidden',
            open ? 'translate-x-0' : '-translate-x-full']">
            <div class="h-full flex flex-col">
                <div class="flex-1 overflow-y-auto p-4 space-y-6 custom-scrollbar">
                    <!-- FORM -->
                    <RouteForm @route-calculated="onRouteCalculated" />

                    <!-- RESULTS -->
                    <div v-if="routeResponse?.routes" class="space-y-3">
                        <h2 class="text-xl font-bold text-slate-800">Itinéraire trouvé</h2>

                        <div class="space-y-3">
                            <div v-for="(route, index) in routeResponse.routes" :key="index"
                                @click="selectRoute(Number(index))"
                                class="p-4 border rounded-2xl cursor-pointer transition-all" :class="[
                                    index === selectedIndex
                                        ? 'border-blue-500 bg-blue-50 shadow-sm'
                                        : 'border-slate-200 hover:bg-slate-50',
                                ]">
                                <div class="flex items-start justify-between gap-4">
                                    <!-- LEFT -->
                                    <div class="min-w-0 flex-1">
                                        <div class="mb-2 flex flex-wrap gap-1.5">
                                            <span
                                                class="rounded-md bg-emerald-100 px-2 py-1 text-xs font-semibold text-emerald-700">
                                                {{ getRouteSpecificities() }}
                                            </span>
                                        </div>

                                        <div class="text-lg font-semibold text-slate-800">
                                            {{ route.costs.totalCost.toFixed(0) }} €
                                        </div>

                                        <div class="flex items-center gap-4 text-sm text-slate-500 mt-1">
                                            <div class="flex items-center gap-1">
                                                <Fuel class="h-4 w-4" />
                                                <span>{{ route.costs.fuelCost.toFixed(0) }} €</span>
                                            </div>

                                            <div class="flex items-center gap-1">
                                                <Receipt class="h-4 w-4" />
                                                <span>{{ route.costs.tollCost.toFixed(0) }} €</span>
                                            </div>

                                            <div class="flex items-center gap-1">
                                                <Car class="h-4 w-4" />
                                                <span>{{ formatDurationSeconds(route.duration -
                                                    route.baseDuration) }}</span>
                                            </div>
                                        </div>
                                    </div>

                                    <!-- RIGHT -->
                                    <div class="text-right">
                                        <div class="text-lg font-semibold text-slate-800">
                                            {{ formatDurationSeconds(route.duration) }}
                                        </div>

                                        <div class="text-sm text-slate-500 mt-1">
                                            {{ (route.distanceMeters / 1000).toFixed(0) }} km
                                        </div>
                                    </div>
                                </div>

                                <!-- Assign button -->
                                <div v-if="index === selectedIndex" class="mt-4">
                                    <button
                                        class="w-full bg-blue-600 text-white py-2 rounded-xl hover:bg-blue-700 transition"
                                        @click.stop="showAssignModal = true">
                                        Attribuer l'itinéraire
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- TOGGLE -->
        <button @click="open = !open" :class="[
            'absolute top-1/2 -translate-y-1/2 z-50',
            'h-16 w-8 rounded-r-xl',
            'bg-white border border-slate-200 border-l-0',
            'shadow-lg hover:bg-slate-50',
            'flex items-center justify-center',
            'transition-all duration-300',
            open ? 'left-[400px]' : 'left-0',
        ]">
            <ChevronLeft v-if="open" class="h-5 w-5 text-slate-600" />
            <ChevronRight v-else class="h-5 w-5 text-slate-600" />
        </button>

    </div>

    <PlanningRouteModal :show="showAssignModal" :initial-route-request="routeRequest"
        :initial-route-response="routeResponse" :initial-selected-route-index="selectedIndex"
        @close="showAssignModal = false" @saved="showAssignModal = false" />


</template>
