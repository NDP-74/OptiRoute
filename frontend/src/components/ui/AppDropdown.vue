<script setup lang="ts">
import { ref, watch, nextTick, onMounted, onBeforeUnmount } from "vue"

const props = withDefaults(
    defineProps<{
        open?: boolean
    }>(),
    {
        open: false,
    },
)

const emit = defineEmits<{
    "update:open": [value: boolean]
}>()

const dropdown = ref<HTMLElement | null>(null)
const menu = ref<HTMLElement | null>(null)
const menuStyle = ref<Record<string, string>>({})

const toggle = () => {
    emit("update:open", !props.open)
}

const close = () => {
    emit("update:open", false)
}

const updateMenuPosition = () => {
    const trigger = dropdown.value
    const panel = menu.value
    if (!trigger || !panel) {
        return
    }

    const triggerRect = trigger.getBoundingClientRect()
    const panelRect = panel.getBoundingClientRect()
    const gap = 8
    const horizontalPadding = 8
    const left = Math.max(
        horizontalPadding,
        Math.min(triggerRect.right - panelRect.width, window.innerWidth - panelRect.width - horizontalPadding),
    )
    const below = triggerRect.bottom + gap
    const top = below + panelRect.height <= window.innerHeight - horizontalPadding
        ? below
        : Math.max(horizontalPadding, triggerRect.top - panelRect.height - gap)

    menuStyle.value = {
        left: `${left}px`,
        top: `${top}px`,
    }
}

const handleClickOutside = (event: MouseEvent) => {
    const target = event.target as Node
    if (!dropdown.value?.contains(target) && !menu.value?.contains(target)) {
        close()
    }
}

watch(() => props.open, async (open) => {
    if (open) {
        await nextTick()
        updateMenuPosition()
    }
}, { immediate: true })

onMounted(() => {
    document.addEventListener("click", handleClickOutside)
    document.addEventListener("scroll", updateMenuPosition, true)
    window.addEventListener("resize", updateMenuPosition)
})

onBeforeUnmount(() => {
    document.removeEventListener("click", handleClickOutside)
    document.removeEventListener("scroll", updateMenuPosition, true)
    window.removeEventListener("resize", updateMenuPosition)
})
</script>

<template>

    <div ref="dropdown" class="relative inline-block">

        <slot name="trigger" :toggle="toggle">
            <button type="button" class="rounded-lg p-2 text-gray-500 transition hover:bg-gray-100 hover:text-gray-800"
                @click.stop="toggle">
                ⋮
            </button>
        </slot>

    </div>

    <Teleport to="body">
        <div v-if="props.open" ref="menu" :style="menuStyle"
            class="fixed z-[1000] w-44 overflow-hidden rounded-xl border border-gray-200 bg-white shadow-lg">
            <slot :close="close" />
        </div>
    </Teleport>

</template>