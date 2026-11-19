<script setup lang="ts">
import { ref, watch } from "vue";

import { fr } from "date-fns/locale";
import { VueDatePicker } from "@vuepic/vue-datepicker";
import "@vuepic/vue-datepicker/dist/main.css";

import CreateSelect from "@/components/ui/CreateSelect.vue";
import type { SavedPlanningView } from "@/models/planning/PlanningView";

type DateRange = [Date, Date] | null;

const props = defineProps<{
    startDate: Date;
    endDate: Date;
    views: SavedPlanningView[];
    loadingViews: boolean;
    daySelectionMode: boolean;
    selectedDayCount: number;
    isCompletingDays: boolean;
    isSyncingDashdoc: boolean;
}>();

const emit = defineEmits<{
    "update:range": [value: [Date, Date]];
    "create-view": [];
    "edit-view": [id: number];
    "today-range": [startOffset: number, additionalDays: number];
    "create-route": [];
    "start-day-selection": [];
    "cancel-day-selection": [];
    "complete-days": [];
    "sync-dashdoc": [];
}>();

const selectedViewId = defineModel<number | null>("viewId", { required: true });

const selectedRange = ref<DateRange>([
    new Date(props.startDate),
    new Date(props.endDate),
]);

function startOfDay(date: Date): Date {
    const result = new Date(date);
    result.setHours(0, 0, 0, 0);

    return result;
}

function handleRangeChange(value: DateRange): void {
    selectedRange.value = value;

    if (!value) {
        return;
    }

    const [rawStart, rawEnd] = value;

    const start = startOfDay(rawStart);
    const requestedEnd = startOfDay(rawEnd);

    const maximumEnd = new Date(start);
    maximumEnd.setDate(maximumEnd.getDate() + 6);

    const end = requestedEnd > maximumEnd ? maximumEnd : requestedEnd;

    selectedRange.value = [start, end];

    emit("update:range", [start, end]);
}

/*
 * Synchronisation lorsque le parent modifie les dates,
 * notamment après l'utilisation d'un raccourci.
 */
watch(
    [() => props.startDate, () => props.endDate,],
    ([startDate, endDate]) => {
        selectedRange.value = [new Date(startDate), new Date(endDate)];
    }
);
</script>

<template>
    <div class="relative z-50 border-b border-slate-200 bg-white px-4 py-3">
        <div class="flex flex-wrap items-end justify-between gap-4">

            <div class="flex flex-wrap items-end gap-3">
                <div>
                    <span class="mb-1 block text-xs font-medium text-slate-600">Vue du planning</span>
                    <CreateSelect v-model="selectedViewId" class="w-56"
                        :options="views.map(view => ({ value: view.id, label: view.name }))"
                        :disabled="loadingViews || isCompletingDays" label="Vue du planning"
                        placeholder="Planning complet" create-label="Créer une vue" editable
                        @create="emit('create-view')" @edit="emit('edit-view', $event)" />
                </div>

                <div>
                    <label class="mb-1 block text-xs font-medium text-slate-600">
                        Période affichée
                    </label>

                    <VueDatePicker :model-value="selectedRange" :range="{ partialRange: false, maxRange: 6 }" auto-apply
                        :locale="fr" :time-config="{
                            enableTimePicker: false,
                        }" :formats="{
                            input: 'dd/MM/yyyy',
                        }" placeholder="Sélectionner une période" @update:model-value="handleRangeChange" />
                </div>

                <div>
                    <p class="mb-1 text-xs font-medium text-slate-600">
                        Raccourcis
                    </p>

                    <div class="flex flex-wrap gap-2">
                        <button type="button"
                            class="rounded-lg border border-slate-300 bg-white px-3 py-2 text-xs font-medium text-slate-700 transition hover:border-blue-300 hover:bg-blue-50 hover:text-blue-700"
                            @click="emit('today-range', -1, 1)">
                            Aujourd’hui - 1 jour
                        </button>

                        <button type="button"
                            class="rounded-lg border border-slate-300 bg-white px-3 py-2 text-xs font-medium text-slate-700 transition hover:border-blue-300 hover:bg-blue-50 hover:text-blue-700"
                            @click="emit('today-range', 0, 0)">
                            Aujourd’hui
                        </button>

                        <button type="button"
                            class="rounded-lg border border-slate-300 bg-white px-3 py-2 text-xs font-medium text-slate-700 transition hover:border-blue-300 hover:bg-blue-50 hover:text-blue-700"
                            @click="emit('today-range', 0, 1)">
                            Aujourd’hui + 1 jour
                        </button>

                        <button type="button"
                            class="rounded-lg border border-slate-300 bg-white px-3 py-2 text-xs font-medium text-slate-700 transition hover:border-blue-300 hover:bg-blue-50 hover:text-blue-700"
                            @click="emit('today-range', 0, 2)">
                            Aujourd’hui + 2 jours
                        </button>
                    </div>
                </div>
            </div>

            <div class="flex flex-wrap items-center gap-2">
                <button type="button" :disabled="props.isSyncingDashdoc || props.isCompletingDays"
                    :aria-busy="props.isSyncingDashdoc"
                    class="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-indigo-700 disabled:cursor-not-allowed disabled:opacity-60"
                    @click="emit('sync-dashdoc')">
                    <svg v-if="props.isSyncingDashdoc" class="mr-2 inline-block h-4 w-4 animate-spin align-[-3px]"
                        xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" aria-hidden="true">
                        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
                        <path class="opacity-75" fill="currentColor"
                            d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                    </svg>
                    {{ props.isSyncingDashdoc ? "Synchronisation..." : "Synchroniser" }}
                </button>

                <span v-if="props.selectedDayCount > 0" class="text-sm text-slate-600" aria-live="polite">
                    {{ props.selectedDayCount }} journée{{ props.selectedDayCount > 1 ? "s" : "" }} sélectionnée{{
                        props.selectedDayCount > 1 ? "s" : "" }}
                </span>

                <button v-if="props.daySelectionMode" type="button" :disabled="props.isCompletingDays"
                    class="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-semibold text-slate-700 transition hover:bg-slate-50"
                    @click="emit('cancel-day-selection')">
                    Annuler la sélection
                </button>

                <button v-else type="button" :disabled="props.isCompletingDays" :class="[
                    'rounded-lg border border-blue-200 bg-blue-50 px-4 py-2 text-sm font-semibold text-blue-700 transition hover:bg-blue-100'
                ]" @click="emit('start-day-selection')">
                    Compléter des journées
                </button>

                <button v-if="props.selectedDayCount > 0" type="button" :disabled="props.isCompletingDays"
                    class="rounded-lg bg-emerald-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-60"
                    @click="emit('complete-days')">
                    {{ props.isCompletingDays ? "Création des trajets à vide..." : "Générer les trajets à vide" }}
                </button>

                <button type="button"
                    class="rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-blue-700"
                    @click="emit('create-route')">
                    Créer et assigner un itinéraire
                </button>
            </div>
        </div>
    </div>
</template>