<template>
    <div ref="mapContainer" class="h-full w-full" />
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps<{
    polyline: string | null
}>()

const mapContainer = ref<HTMLElement | null>(null)

let map: any = null
let routePolylines: any[] = []
let resizeObserver: ResizeObserver | null = null

function clearRoute() {
    if (!map) return

    for (const routePolyline of routePolylines) {
        map.removeObject(routePolyline)
    }

    routePolylines = []
}

function createLineString(polyline: string) {
    let parsedPolyline: any

    try {
        parsedPolyline = JSON.parse(polyline)
    } catch {
        return H.geo.LineString.fromFlexiblePolyline(polyline)
    }

    const geometry = parsedPolyline.type === 'Feature' ? parsedPolyline.geometry : parsedPolyline

    if (geometry?.type === 'LineString' && Array.isArray(geometry.coordinates)) {
        const lineString = new H.geo.LineString()

        for (const coordinate of geometry.coordinates) {
            if (Array.isArray(coordinate) && coordinate.length >= 2) {
                lineString.pushLatLngAlt(coordinate[1], coordinate[0], coordinate[2] ?? 0)
            }
        }

        if (lineString.getPointCount() > 1) {
            return lineString
        }
    }

    return H.geo.LineString.fromFlexiblePolyline(polyline)
}

function displayRoute() {
    if (!map) return

    clearRoute()

    if (!props.polyline) return

    let encodedPolylines: string[]

    try {
        const parsedPolyline = JSON.parse(props.polyline)
        encodedPolylines = Array.isArray(parsedPolyline)
            ? parsedPolyline
            : [props.polyline]
    } catch {
        encodedPolylines = [props.polyline]
    }

    for (const encodedPolyline of encodedPolylines) {
        const lineString = createLineString(encodedPolyline)
        const routePolyline = new H.map.Polyline(lineString, {
            style: {
                strokeColor: '#2563eb',
                lineWidth: 5,
                lineCap: 'round',
                lineJoin: 'round'
            }
        })

        routePolylines.push(routePolyline)
        map.addObject(routePolyline)
    }

    const bounds = routePolylines.reduce(
        (currentBounds, routePolyline) => currentBounds
            ? currentBounds.merge(routePolyline.getBoundingBox())
            : routePolyline.getBoundingBox(),
        null
    )

    map.getViewModel().setLookAtData({
        bounds,
        padding: {
            top: 30,
            right: 30,
            bottom: 30,
            left: 30
        }
    })
}

onMounted(async () => {
    const platform = new H.service.Platform({
        apikey: import.meta.env.VITE_HERE_API_KEY
    })

    const defaultLayers = platform.createDefaultLayers()

    map = new H.Map(
        mapContainer.value!,
        defaultLayers.vector.normal.map,
        {
            center: {
                lat: 46.6,
                lng: 2.5
            },
            zoom: 6,
            pixelRatio: window.devicePixelRatio || 1
        }
    )

    new H.mapevents.Behavior(
        new H.mapevents.MapEvents(map)
    )

    resizeObserver = new ResizeObserver(() => {
        map?.getViewPort().resize()
    })

    resizeObserver.observe(mapContainer.value!)

    await nextTick()

    map.getViewPort().resize()
    displayRoute()
})

watch(() => props.polyline, displayRoute)

onBeforeUnmount(() => {
    resizeObserver?.disconnect()
    map?.dispose()
})
</script>