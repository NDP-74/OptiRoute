import axios from 'axios'
import api from '../axios'
import type { RouteRequest, RouteResponse } from '@/models/route/Route'

// Variables

const API_KEY = import.meta.env.VITE_HERE_API_KEY

// Functions

export async function calculateRoute(payload: RouteRequest): Promise<RouteResponse> {
    const response = await api.post('routes/calculate', payload)
    return response.data
}

export async function autocompletePlaces(query: string) {

    if (!query || query.length < 3) {
        return []
    }

    const response = await axios.get('https://autocomplete.search.hereapi.com/v1/autocomplete',
        {
            params: {
                q: query,
                apiKey: API_KEY,
                limit: 5,
                in: 'countryCode:FRA'
            }
        }
    )

    return response.data.items
}

export async function geocodePlace(id: string) {
    const response = await axios.get('https://lookup.search.hereapi.com/v1/lookup',
        {
            params: {
                id,
                apiKey: API_KEY
            }
        }
    )

    return response.data
}