<script setup lang="ts">
import { ref, watch } from 'vue'
import { debounce } from 'lodash-es'
import { autocompletePlaces, geocodePlace } from '@/api/here/mapsApi'

//Variables

const props = defineProps<{
    label?: string
    modelValue?: any
    disabled?: boolean
}>()

const emit = defineEmits(['selected', 'update:modelValue'])
const inputValue = ref('')
const results = ref<any[]>([])
const loading = ref(false)
const isSelecting = ref(false)

watch(() => props.modelValue, (value) => {
    isSelecting.value = true
    inputValue.value = value?.address ?? ''
    results.value = []

    setTimeout(() => {
        isSelecting.value = false
    }, 0)
}, { immediate: true })

//Functions

watch(inputValue, (value) => {
    if (isSelecting.value) return

    fetchPlaces(value)
})

const fetchPlaces = debounce(
    async (value: string) => {

        if (!value || value.length < 3) {
            results.value = []
            return
        }

        loading.value = true

        try {
            results.value = await autocompletePlaces(value)
        } catch (e) {
            console.error(e)
        } finally {
            loading.value = false
        }

    },
    800, // debounce
    {
        maxWait: 5000 // équivalent VueUse maxWait
    }
)

async function selectPlace(item: any) {
    try {
        isSelecting.value = true

        const place = await geocodePlace(item.id)
        const label = place.address.label

        // Important: on stop le debounce en cours
        fetchPlaces.cancel?.()

        inputValue.value = label
        results.value = []

        const selectedPlace = {
            name: place.address.city ?? place.title ?? label,
            address: label,
            position: {
                lat: place.position.lat,
                lng: place.position.lng
            }
        }

        emit('selected', selectedPlace)
        emit('update:modelValue', selectedPlace)

    } finally {
        loading.value = false

        setTimeout(() => {
            isSelecting.value = false
        }, 0)
    }
}
</script>

<template>
    <div class="relative">

        <label class="block text-sm font-medium mb-2">
            {{ label }}
        </label>

        <input v-model="inputValue" type="text" :disabled="props.disabled"
            class="w-full rounded-xl border border-slate-300 p-3 disabled:cursor-not-allowed disabled:bg-slate-100"
            :placeholder="label" />

        <!-- RESULTS -->
        <div v-if="results.length > 0"
            class="absolute z-50 mt-2 w-full bg-white border border-slate-200 rounded-xl shadow-xl overflow-hidden">

            <button v-for="item in results" :key="item.id" type="button" :disabled="props.disabled"
                @click="selectPlace(item)"
                class="w-full text-left px-4 py-3 hover:bg-slate-50 border-b border-slate-100 disabled:cursor-not-allowed">

                <div class="text-sm font-medium">
                    {{ item.title }}
                </div>

                <div class="text-xs text-slate-500">
                    {{ item.address.label }}
                </div>

            </button>

        </div>

    </div>
</template>