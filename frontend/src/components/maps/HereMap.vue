<template>
    <div ref="mapContainer" class="h-full w-full" />
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'

//Variables

const mapContainer = ref<HTMLElement | null>(null)
let map: any

const routePolylines: any[] = []

let startMarker: any = null
let endMarker: any = null
const waypointMarkers: any[] = []

const emit = defineEmits([
    'route-selected'
])

//Functions

function emitRouteSelected(index: number) {
    emit('route-selected', index)
}

function clearRoutes() {
    for (const route of routePolylines) {
        map.removeObject(route)
    }

    routePolylines.length = 0
}

function clearMarkers() {
    if (startMarker) {
        map.removeObject(startMarker)
    }

    if (endMarker) {
        map.removeObject(endMarker)
    }

    for (const marker of waypointMarkers) {
        map.removeObject(marker)
    }

    waypointMarkers.length = 0
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

function setMarkers(origin: any, destination: any, waypoints: any[] = []) {
    clearMarkers()

    startMarker = new H.map.Marker(origin)
    endMarker = new H.map.Marker(destination)

    map.addObject(startMarker)
    map.addObject(endMarker)

    const waypointIcon = new H.map.Icon(
        'data:image/svg+xml;charset=UTF-8,' + encodeURIComponent(
            '<svg xmlns="http://www.w3.org/2000/svg" width="32" height="40" viewBox="0 0 32 40">' +
            '<path d="M16 1C8.3 1 2 7.3 2 15c0 10.2 14 23 14 23s14-12.8 14-23C30 7.3 23.7 1 16 1Z" fill="#f59e0b" stroke="#92400e" stroke-width="2"/>' +
            '<circle cx="16" cy="15" r="5" fill="#fff7ed" stroke="#92400e" stroke-width="2"/>' +
            '</svg>'
        ),
        { size: { w: 32, h: 40 }, anchor: { x: 16, y: 40 } }
    )

    for (const waypoint of waypoints.filter(Boolean)) {
        const marker = new H.map.Marker(waypoint, { icon: waypointIcon })
        waypointMarkers.push(marker)
        map.addObject(marker)
    }
}

function displayRoutes(routes: any[], selectedRoute: any) {
    if (!map) return
    clearRoutes()

    const selectedIndex = routes.findIndex(route => route === selectedRoute)

    const orderedRoutes = routes.map((route, index) => ({ route, index, isSelected: index === selectedIndex })).sort((a, b) => Number(a.isSelected) - Number(b.isSelected))

    orderedRoutes.forEach(({ route, index, isSelected }) => {
        const routeGroup = new H.map.Group()
        let polylines: string[]

        try {
            const parsedPolyline = JSON.parse(route.polyline)
            polylines = Array.isArray(parsedPolyline) ? parsedPolyline : [route.polyline]
        } catch {
            polylines = [route.polyline]
        }

        for (const encodedPolyline of polylines.filter(Boolean)) {
            const lineString = createLineString(encodedPolyline)
            const polyline = new H.map.Polyline(lineString,
                {
                    style: {
                        lineWidth: isSelected ? 8 : 5,
                        strokeColor: isSelected
                            ? '#2563eb'
                            : '#38bdf8',
                        lineCap: 'round',
                        lineJoin: 'round'
                    }
                }
            )

            polyline.addEventListener('pointerenter', () => {
                if (mapContainer.value) {
                    mapContainer.value.style.cursor = 'pointer'
                }
            })

            polyline.addEventListener('pointerleave', () => {
                if (mapContainer.value) {
                    mapContainer.value.style.cursor = ''
                }
            })

            polyline.addEventListener('tap', () => { emitRouteSelected(index) })
            routeGroup.addObject(polyline)
        }

        routePolylines.push(routeGroup)
        map.addObject(routeGroup)
    })

    const selectedPolyline = routePolylines[routePolylines.length - 1]

    if (selectedPolyline) {
        const bounds = selectedPolyline.getBoundingBox()

        const latitudePadding = (bounds.getTop() - bounds.getBottom()) * 0.15
        const longitudePadding = (bounds.getRight() - bounds.getLeft()) * 0.15

        const paddedBounds = new H.geo.Rect(
            bounds.getTop() + latitudePadding,
            bounds.getLeft() - longitudePadding,
            bounds.getBottom() - latitudePadding,
            bounds.getRight() + longitudePadding
        )

        map.getViewModel().setLookAtData(
            {
                bounds: paddedBounds
            }
        )
    }

}

defineExpose({
    displayRoutes,
    setMarkers
})

onMounted(() => {

    const platform = new H.service.Platform({ apikey: import.meta.env.VITE_HERE_API_KEY })

    const defaultLayers = platform.createDefaultLayers()

    map = new H.Map(mapContainer.value!, defaultLayers.vector.normal.map,
        {
            center: { lat: 46.6, lng: 2.5 },
            zoom: 7,
            pixelRatio: window.devicePixelRatio || 1,
        }
    )

    map.addEventListener('mapviewchangeend', () => {
        const bounds = map.getViewModel().getLookAtData().bounds
        if (!bounds) return


        const center = map.getCenter()
        const lat = Math.min(Math.max(center.lat, 41.0), 51.2)
        const lng = Math.min(Math.max(center.lng, -5.5), 9.8)
        map.setCenter({ lat, lng })

        const zoom = map.getZoom()

        if (zoom < 6) {
            map.setZoom(6)
        } else if (zoom > 15) {
            map.setZoom(15)
        }
    })

    new H.mapevents.Behavior(new H.mapevents.MapEvents(map))

    H.ui.UI.createDefault(map, defaultLayers)

    window.addEventListener('resize', () => map.getViewPort().resize())
})
</script>