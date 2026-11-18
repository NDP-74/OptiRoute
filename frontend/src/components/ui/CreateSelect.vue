<script setup lang="ts" generic="T extends string | number">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, useId, watch } from "vue"
import { Pencil } from "lucide-vue-next"

const selected = defineModel<T | null>({ required: true })

const props = defineProps<{
    options: { value: T; label: string }[]
    label: string
    placeholder: string
    createLabel: string
    disabled?: boolean
    editable?: boolean
}>()

const emit = defineEmits<{ create: []; edit: [value: T] }>()
const id = useId()
const root = ref<HTMLElement | null>(null)
const trigger = ref<HTMLButtonElement | null>(null)
const menu = ref<HTMLElement | null>(null)
const open = ref(false)
const selectedLabel = computed(() => props.options.find((option) => option.value === selected.value)?.label)

const close = () => {
    open.value = false
}

const closeAndFocus = () => {
    close()
    trigger.value?.focus()
}

const choose = (value: T | null) => {
    selected.value = value
    closeAndFocus()
}

const create = () => {
    closeAndFocus()
    emit("create")
}

const edit = (value: T) => {
    closeAndFocus()
    emit("edit", value)
}

const moveFocus = async (direction: number) => {
    if (props.disabled) return
    open.value = true
    await nextTick()
    const buttons = Array.from(menu.value?.querySelectorAll<HTMLButtonElement>("button") ?? [])
    const current = buttons.findIndex((button) => button === document.activeElement)
    const index = current < 0
        ? (direction > 0 ? 0 : buttons.length - 1)
        : (current + direction + buttons.length) % buttons.length
    buttons[index]?.focus()
}

const onOutsideClick = (event: MouseEvent) => {
    if (event.target instanceof Node && !root.value?.contains(event.target)) close()
}

const onFocusOut = (event: FocusEvent) => {
    if (!(event.relatedTarget instanceof Node) || !root.value?.contains(event.relatedTarget)) close()
}

watch(() => props.disabled, (disabled) => {
    if (disabled) close()
})

onMounted(() => document.addEventListener("mousedown", onOutsideClick))
onBeforeUnmount(() => document.removeEventListener("mousedown", onOutsideClick))
</script>

<template>
    <div ref="root" class="relative" @focusout="onFocusOut" @keydown.esc.stop.prevent="closeAndFocus"
        @keydown.down.prevent="moveFocus(1)" @keydown.up.prevent="moveFocus(-1)">
        <button ref="trigger" type="button" :disabled="disabled" :aria-label="label" :aria-expanded="open"
            :aria-controls="id"
            class="flex w-full items-center justify-between rounded-xl border border-slate-300 bg-white px-4 py-2.5 text-left text-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100 disabled:cursor-not-allowed disabled:bg-slate-100"
            @click="open = !open">
            <span class="truncate" :class="selectedLabel ? 'text-slate-900' : 'text-slate-400'">
                {{ selectedLabel ?? placeholder }}
            </span>
            <span aria-hidden="true" class="ml-2 text-xs text-slate-400">&#9662;</span>
        </button>

        <div v-if="open" :id="id" ref="menu"
            class="absolute z-30 mt-1 w-full min-w-64 overflow-hidden rounded-xl border border-slate-200 bg-white shadow-lg">
            <ul class="max-h-60 overflow-y-auto py-1">
                <li>
                    <button type="button" :aria-pressed="selected === null"
                        class="w-full px-4 py-2 text-left text-sm text-slate-400 hover:bg-slate-50 focus:bg-slate-50"
                        @click="choose(null)">
                        {{ placeholder }}
                    </button>
                </li>
                <li v-for="option in options" :key="option.value" class="flex items-center"
                    :class="{ 'bg-blue-50': editable && selected === option.value }">
                    <button type="button" :aria-pressed="selected === option.value"
                        class="min-w-0 flex-1 truncate px-4 py-2 text-left text-sm hover:bg-slate-50 focus:bg-slate-50"
                        :class="{ 'bg-blue-50 font-medium text-blue-700': selected === option.value }"
                        @click="choose(option.value)">
                        {{ option.label }}
                    </button>
                    <button v-if="editable" type="button" :aria-label="`Modifier ${option.label}`"
                        :title="`Modifier ${option.label}`"
                        class="mr-2 shrink-0 rounded-md p-1.5 text-slate-500 hover:bg-blue-100 hover:text-blue-700 focus-visible:bg-blue-100"
                        @click="edit(option.value)">
                        <Pencil :size="14" aria-hidden="true" />
                    </button>
                </li>
            </ul>
            <div class="border-t border-slate-100 p-2">
                <button type="button"
                    class="w-full rounded-lg px-2 py-1.5 text-left text-sm text-blue-600 hover:bg-blue-50 focus:bg-blue-50"
                    @click="create">
                    + {{ createLabel }}
                </button>
            </div>
        </div>
    </div>
</template>
