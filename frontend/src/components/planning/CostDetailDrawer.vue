<script setup lang="ts">
import { computed } from "vue"
import { CircleDollarSign, Truck, WalletCards } from "lucide-vue-next"

import AppDetailDrawer from "@/components/ui/AppDetailDrawer.vue"
import DetailRow from "@/components/ui/DetailRow.vue"
import DetailSection from "@/components/ui/DetailSection.vue"
import { formatCurrency } from "@/utils/formatters"

import type { PlanningDriver } from "@/models/planning/planning"

const props = defineProps<{
    open: boolean
    driver: PlanningDriver | null
}>()

const emit = defineEmits<{
    close: []
}>()

const isUnassigned = computed(() => props.driver?.id === -1)
const result = computed(() => props.driver ? props.driver.totalRevenue - props.driver.totalCost : 0)
</script>

<template>
    <AppDetailDrawer :open="open" title="Synthèse des coûts" @close="emit('close')">
        <template #header>
            <div class="min-w-0">
                <p class="text-xs font-medium uppercase tracking-wide text-blue-600">Synthèse financière</p>
                <h2 class="mt-1 truncate text-lg font-semibold text-slate-900">{{ driver?.name ?? "Chargement..." }}
                </h2>
            </div>
        </template>

        <div v-if="driver" class="space-y-6 p-6">
            <DetailSection v-if="!isUnassigned" title="Répartition des coûts" :icon="WalletCards">
                <DetailRow label="Total coût chauffeur" :value="formatCurrency(driver.driverCost)" />
                <DetailRow label="Total coût structure" :value="formatCurrency(driver.structureCost)" />
                <DetailRow label="Total coût véhicule" :value="formatCurrency(driver.vehicleCost)" />
                <DetailRow label="Total coûts événements" :value="formatCurrency(driver.eventCost)" />
            </DetailSection>

            <DetailSection v-else title="Véhicules non utilisés" :icon="Truck">
                <DetailRow label="Total coût véhicule" :value="formatCurrency(driver.vehicleCost)" />
                <DetailRow label="Véhicules" :value="driver.vehicleRegistrations.join(', ') || 'Aucun véhicule'"
                    :break-value="true" />
            </DetailSection>

            <DetailSection v-if="!isUnassigned" title="Résultat de la période" :icon="CircleDollarSign">
                <DetailRow label="Chiffre d'affaires" :value="formatCurrency(driver.totalRevenue)" />
                <DetailRow label="Coûts totaux" :value="formatCurrency(driver.totalCost)" />
            </DetailSection>

            <section class="rounded-xl border p-4"
                :class="result >= 0 ? 'border-emerald-200 bg-emerald-50' : 'border-red-200 bg-red-50'">
                <div class="flex items-center justify-between gap-4">
                    <p class="text-sm font-semibold" :class="result >= 0 ? 'text-emerald-800' : 'text-red-800'">Résultat
                    </p>
                    <p class="text-lg font-bold" :class="result >= 0 ? 'text-emerald-700' : 'text-red-700'">{{
                        formatCurrency(result) }}</p>
                </div>
            </section>
        </div>
    </AppDetailDrawer>
</template>