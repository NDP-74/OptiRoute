<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref } from "vue"

import { createTrailerType, deleteTrailerType, updateTrailerType } from "@/api/vehicle/semiTrailerApi"
import type { TrailerTypeOption } from "@/api/vehicle/semiTrailerApi"
import { getApiErrorMessage } from "@/api/utils"

const selected = defineModel<string | null>({ default: null })
const types = defineModel<TrailerTypeOption[]>("types", { default: () => [] })

defineProps<{ disabled?: boolean }>()

const root = ref<HTMLElement | null>(null)
const open = ref(false)
const busy = ref(false)
const error = ref("")

// Only one row action at a time: adding, renaming a type or confirming its deletion
const adding = ref(false)
const renamingId = ref<number | null>(null)
const deletingId = ref<number | null>(null)
const label = ref("")
const inputs = ref<HTMLInputElement[]>([])
const addInput = ref<HTMLInputElement | null>(null)

const resetActions = () => {
    adding.value = false
    renamingId.value = null
    deletingId.value = null
    label.value = ""
    error.value = ""
}

const toggle = () => {
    open.value = !open.value
    if (!open.value) resetActions()
}

const choose = (type: TrailerTypeOption | null) => {
    selected.value = type?.label ?? null
    open.value = false
    resetActions()
}

const startAdd = async () => {
    resetActions()
    adding.value = true
    await nextTick()
    addInput.value?.focus()
}

const startRename = async (type: TrailerTypeOption) => {
    resetActions()
    renamingId.value = type.id
    label.value = type.label
    await nextTick()
    inputs.value[0]?.focus()
    inputs.value[0]?.select()
}

const startDelete = (type: TrailerTypeOption) => {
    resetActions()
    deletingId.value = type.id
}

const sorted = (list: TrailerTypeOption[]) => [...list].sort((a, b) => a.label.localeCompare(b.label))

const save = async () => {
    const value = label.value.trim()
    if (!value || busy.value) return

    const duplicate = types.value.find((t) => t.label.toLowerCase() === value.toLowerCase() && t.id !== renamingId.value)
    if (duplicate) {
        if (adding.value) {
            choose(duplicate)
        } else {
            error.value = "Ce type existe déjà."
        }
        return
    }

    try {
        busy.value = true
        if (renamingId.value !== null) {
            const wasSelected = types.value.find((t) => t.id === renamingId.value)?.label === selected.value
            const updated = await updateTrailerType(renamingId.value, { label: value })
            types.value = sorted(types.value.map((t) => (t.id === updated.id ? updated : t)))
            if (wasSelected) selected.value = updated.label
            resetActions()
        } else {
            const created = await createTrailerType({ label: value })
            types.value = sorted([...types.value, created])
            choose(created)
        }
    } catch (e: unknown) {
        error.value = getApiErrorMessage(e, "Le type n’a pas pu être enregistré.")
    } finally {
        busy.value = false
    }
}

const confirmDelete = async (type: TrailerTypeOption) => {
    if (busy.value) return

    try {
        busy.value = true
        await deleteTrailerType(type.id)
        types.value = types.value.filter((t) => t.id !== type.id)
        if (selected.value === type.label) selected.value = null
        resetActions()
    } catch (e: unknown) {
        deletingId.value = null
        error.value = getApiErrorMessage(e, "Ce type est utilisé par une semi-remorque et ne peut pas être supprimé.")
    } finally {
        busy.value = false
    }
}

const onOutsideClick = (event: MouseEvent) => {
    if (open.value && root.value && !root.value.contains(event.target as Node)) {
        open.value = false
        resetActions()
    }
}

onMounted(() => document.addEventListener("mousedown", onOutsideClick))
onBeforeUnmount(() => document.removeEventListener("mousedown", onOutsideClick))
</script>

<template>
    <div ref="root" class="relative" @keydown.esc="open = false; resetActions()">
        <button type="button" :disabled="disabled"
            class="flex w-full items-center justify-between rounded-xl border border-slate-300 bg-white px-4 py-3 text-left outline-none transition focus:border-blue-500 disabled:cursor-not-allowed disabled:bg-slate-100"
            @click="toggle">
            <span :class="selected ? 'text-slate-900' : 'text-slate-400'" class="truncate">
                {{ selected ?? 'Sélectionner un type' }}
            </span>
            <span class="ml-2 text-xs text-slate-400">▾</span>
        </button>

        <div v-if="open"
            class="absolute z-30 mt-1 w-full min-w-64 overflow-hidden rounded-xl border border-slate-200 bg-white shadow-lg">
            <ul class="max-h-60 overflow-y-auto py-1">
                <li class="cursor-pointer px-4 py-2 text-sm text-slate-400 hover:bg-slate-50" @click="choose(null)">
                    Aucun type
                </li>

                <li v-for="type in types" :key="type.id" class="group px-2">
                    <div v-if="renamingId === type.id" class="flex items-center gap-1 py-1">
                        <input ref="inputs" v-model="label" type="text" maxlength="100" :disabled="busy"
                            class="min-w-0 flex-1 rounded-lg border border-slate-300 px-2 py-1 text-sm outline-none focus:border-blue-500"
                            @keydown.enter.prevent="save" @keydown.esc.stop.prevent="resetActions" />
                        <button type="button" :disabled="busy || !label.trim()" title="Enregistrer"
                            class="rounded-lg px-2 py-1 text-blue-600 hover:bg-blue-50 disabled:opacity-40"
                            @click="save">✓</button>
                        <button type="button" :disabled="busy" title="Annuler"
                            class="rounded-lg px-2 py-1 text-slate-500 hover:bg-slate-100"
                            @click="resetActions">✕</button>
                    </div>

                    <div v-else-if="deletingId === type.id"
                        class="my-1 flex items-center gap-1 rounded-lg bg-red-50 px-2 py-1 text-sm text-red-700">
                        <span class="min-w-0 flex-1 truncate">Supprimer « {{ type.label }} » ?</span>
                        <button type="button" :disabled="busy"
                            class="rounded-md bg-red-600 px-2 py-0.5 text-white hover:bg-red-700 disabled:opacity-50"
                            @click="confirmDelete(type)">Oui</button>
                        <button type="button" :disabled="busy"
                            class="rounded-md border border-slate-300 bg-white px-2 py-0.5 text-slate-600 hover:bg-slate-100"
                            @click="resetActions">Non</button>
                    </div>

                    <div v-else
                        class="flex cursor-pointer items-center gap-1 rounded-lg px-2 py-2 text-sm hover:bg-slate-50"
                        :class="{ 'bg-blue-50 font-medium text-blue-700': selected === type.label }"
                        @click="choose(type)">
                        <span class="min-w-0 flex-1 truncate">{{ type.label }}</span>
                        <button type="button" title="Renommer"
                            class="invisible rounded-md px-1.5 text-slate-500 hover:bg-slate-200 group-hover:visible"
                            @click.stop="startRename(type)">✎</button>
                        <button type="button" title="Supprimer"
                            class="invisible rounded-md px-1.5 text-red-500 hover:bg-red-100 group-hover:visible"
                            @click.stop="startDelete(type)">🗑</button>
                    </div>
                </li>
            </ul>

            <p v-if="error" class="border-t border-slate-100 px-4 py-2 text-sm text-red-600">{{ error }}</p>

            <div class="border-t border-slate-100 p-2">
                <div v-if="adding" class="flex items-center gap-1">
                    <input ref="addInput" v-model="label" type="text" maxlength="100" placeholder="Nouveau type"
                        :disabled="busy"
                        class="min-w-0 flex-1 rounded-lg border border-slate-300 px-2 py-1 text-sm outline-none focus:border-blue-500"
                        @keydown.enter.prevent="save" @keydown.esc.stop.prevent="resetActions" />
                    <button type="button" :disabled="busy || !label.trim()" title="Enregistrer"
                        class="rounded-lg px-2 py-1 text-blue-600 hover:bg-blue-50 disabled:opacity-40"
                        @click="save">✓</button>
                    <button type="button" :disabled="busy" title="Annuler"
                        class="rounded-lg px-2 py-1 text-slate-500 hover:bg-slate-100"
                        @click="resetActions">✕</button>
                </div>
                <button v-else type="button"
                    class="w-full rounded-lg px-2 py-1.5 text-left text-sm text-blue-600 hover:bg-blue-50"
                    @click="startAdd">
                    + Ajouter un type
                </button>
            </div>
        </div>
    </div>
</template>
