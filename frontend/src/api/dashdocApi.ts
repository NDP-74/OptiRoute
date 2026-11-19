import api from '@/api/axios'

export interface DashdocSyncResponse {
    created: number
    updated: number
    skipped: number
    skippedReasons: Record<string, number>
    failures: string[]
}

export const synchronizeDashdoc = async (startDate: string, endDate: string): Promise<DashdocSyncResponse> => {
    const response = await api.post<DashdocSyncResponse>('/dashdoc/sync', null, {
        params: { startDate, endDate },
    })

    return response.data
}
