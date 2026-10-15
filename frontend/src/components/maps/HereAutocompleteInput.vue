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

const emit = defineEmits(['selected', 'update:modelValue', 'input'])
const inputValue = ref('')
const results = ref<any[]>([])
const loading = ref(false)
const isSelecting = ref(false)
let requestVersion = 0

//Functions

watch(inputValue, (value) => {
    if (isSelecting.value) return

    requestVersion++
    const coordinates = parseCoordinates(value)
    if (coordinates) {
        fetchPlaces.cancel?.()
        emit('update:modelValue', coordinates)
        results.value = []
        return
    }

    fetchPlaces(value)
})

const fetchPlaces = debounce(
    async (value: string) => {

        if (!value || value.length < 3) {
            results.value = []
            return
        }

        loading.value = true
        const version = requestVersion

        try {
            const places = await autocompletePlaces(value)
            if (version === requestVersion) results.value = places
        } catch (e) {
            console.error(e)
        } finally {
            if (version === requestVersion) loading.value = false
        }

    },
    800, // debounce
    {
        maxWait: 5000 // équivalent VueUse maxWait
    }
)

watch(() => props.modelValue, (value) => {
    requestVersion++
    fetchPlaces.cancel()
    isSelecting.value = true
    inputValue.value = value?.address ?? ''
    results.value = []
    loading.value = false

    setTimeout(() => {
        isSelecting.value = false
    }, 0)
}, { immediate: true })

function parseCoordinates(value: string) {
    const parts = value.split(',').map(part => part.trim())
    if (parts.length !== 2 || parts.some(part => part === '')) return null

    const latitude = Number(parts[0])
    const longitude = Number(parts[1])

    if (!Number.isFinite(latitude) || !Number.isFinite(longitude)
        || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
        return null
    }

    return {
        name: value.trim(),
        address: value.trim(),
        position: {
            lat: latitude,
            lng: longitude
        }
    }
}

async function selectPlace(item: any) {
    const version = ++requestVersion
    try {
        isSelecting.value = true
        fetchPlaces.cancel()

        const place = await geocodePlace(item.id)
        if (version !== requestVersion) return
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

        <input v-model="inputValue" type="text" :disabled="props.disabled" @input="emit('input')"
            class="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm shadow-sm transition focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 disabled:cursor-not-allowed disabled:bg-slate-100"
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