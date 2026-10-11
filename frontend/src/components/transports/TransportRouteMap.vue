<template>
    <div ref="mapContainer" class="h-full w-full" />
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps<{
    polyline: string | null
    events?: { pointOfInterestLabel: string | null, pointOfInterestLatitude: number | null, pointOfInterestLongitude: number | null }[]
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

let eventGroup: any = null
const labelCanvas = document.createElement('canvas').getContext('2d')!

function createEventIcon(label: string) {
    labelCanvas.font = '600 12px sans-serif'
    const textWidth = Math.ceil(labelCanvas.measureText(label).width)
    const width = Math.max(textWidth + 12, 28)
    const height = 48
    const cx = width / 2
    const escaped = label.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')

    const svg =
        `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}">` +
        `<circle cx="${cx}" cy="14" r="11" fill="#10b981" stroke="#065f46" stroke-width="3"/>` +
        `<circle cx="${cx}" cy="14" r="4" fill="#ecfdf5"/>` +
        `<text x="${cx}" y="42" text-anchor="middle" font-family="sans-serif" font-size="12" font-weight="600" ` +
        `fill="#064e3b" stroke="#ffffff" stroke-width="3" paint-order="stroke">${escaped}</text>` +
        '</svg>'

    return new H.map.Icon('data:image/svg+xml;charset=UTF-8,' + encodeURIComponent(svg), { size: { w: width, h: height }, anchor: { x: cx, y: 14 } })
}

function displayEvents() {
    if (!map) return

    if (eventGroup) {
        map.removeObject(eventGroup)
        eventGroup = null
    }

    const located = (props.events ?? []).filter(event => event.pointOfInterestLatitude != null && event.pointOfInterestLongitude != null)
    if (located.length === 0) return

    eventGroup = new H.map.Group()
    for (const event of located) {
        eventGroup.addObject(new H.map.Marker(
            { lat: Number(event.pointOfInterestLatitude), lng: Number(event.pointOfInterestLongitude) },
            { icon: createEventIcon(event.pointOfInterestLabel ?? '') }
        ))
    }
    map.addObject(eventGroup)
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
    displayEvents()
})

watch(() => props.polyline, displayRoute)
watch(() => props.events, displayEvents, { deep: true })

onBeforeUnmount(() => {
    resizeObserver?.disconnect()
    map?.dispose()
})
</script>