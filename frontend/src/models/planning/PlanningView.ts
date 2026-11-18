export type PlanningViewRow =
    | { type: 'DRIVER'; driverId: number }
    | { type: 'UNUSED_VEHICLES'; driverId: null }

export interface PlanningViewDraft {
    name: string
    rows: PlanningViewRow[]
}

export interface SavedPlanningView extends PlanningViewDraft {
    id: number
}
