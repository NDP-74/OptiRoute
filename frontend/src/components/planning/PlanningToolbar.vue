<script setup lang="ts">
import { ref, watch } from "vue";

import { fr } from "date-fns/locale";
import { VueDatePicker } from "@vuepic/vue-datepicker";
import "@vuepic/vue-datepicker/dist/main.css";

type DateRange = [Date, Date] | null;

const props = defineProps<{
    startDate: Date;
    endDate: Date;
}>();

const emit = defineEmits<{
    "update:range": [value: [Date, Date]];
    "today-range": [startOffset: number, additionalDays: number];
    "create-route": [];
    "create-event": [];
}>();

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
    <div class="border-b border-slate-200 bg-white px-4 py-3">
        <div class="flex flex-wrap items-end justify-between gap-4">

            <div class="flex flex-wrap items-end gap-3">
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

                        <button type="button"
                            class="rounded-lg border border-slate-300 bg-white px-3 py-2 text-xs font-medium text-slate-700 transition hover:border-blue-300 hover:bg-blue-50 hover:text-blue-700"
                            @click="emit('today-range', 0, 3)">
                            Aujourd’hui + 3 jours
                        </button>
                    </div>
                </div>
            </div>

            <div class="flex flex-wrap gap-2">
                <button type="button"
                    class="rounded-lg border border-blue-200 bg-white px-4 py-2 text-sm font-semibold text-blue-700 transition hover:bg-blue-50"
                    @click="emit('create-event')">
                    Créer un événement
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