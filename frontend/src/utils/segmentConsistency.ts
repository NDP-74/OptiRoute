import type { TransportRouteDetail } from '@/models/transport/TransportDetail'

export type SegmentTiming = Pick<TransportRouteDetail,
    'originLat' | 'originLng' | 'destinationLat' | 'destinationLng' | 'departureStart' | 'arrivalStart'>

// ~100 m : au-delà, deux segments consécutifs ne sont plus considérés comme reliés
const SAME_PLACE_TOLERANCE = 0.001

function samePlace(lat1: number, lng1: number, lat2: number, lng2: number): boolean {
    return Math.abs(lat1 - lat2) <= SAME_PLACE_TOLERANCE && Math.abs(lng1 - lng2) <= SAME_PLACE_TOLERANCE
}

function isAfter(first: string | null, second: string | null): boolean {
    return Boolean(first && second && new Date(first).getTime() > new Date(second).getTime())
}

export interface SegmentWarning {
    // Index (0-based) du segment qui suit la rupture
    segmentIndex: number
    message: string
}

// Incohérences entre segments consécutifs d'un même service (chevauchement horaire ou rupture géographique)
export function segmentWarnings(routes: SegmentTiming[]): SegmentWarning[] {
    const warnings: SegmentWarning[] = []
    for (let index = 1; index < routes.length; index++) {
        const previous = routes[index - 1]!
        const current = routes[index]!
        if (isAfter(previous.arrivalStart, current.departureStart)) {
            warnings.push({ segmentIndex: index, message: `Le trajet ${index + 1} part avant l’arrivée du trajet ${index}.` })
        }
        if (!samePlace(previous.destinationLat, previous.destinationLng, current.originLat, current.originLng)) {
            warnings.push({ segmentIndex: index, message: `Le trajet ${index + 1} ne part pas du point d’arrivée du trajet ${index}.` })
        }
    }
    return warnings
}
