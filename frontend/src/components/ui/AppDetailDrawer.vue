<script setup lang="ts">
defineProps<{
    open: boolean
    title?: string
}>()

const emit = defineEmits<{
    close: []
}>()
</script>

<template>
    <Teleport to="body">
        <div class="pointer-events-none fixed inset-0 z-50 overflow-hidden">

            <!-- Zone de clic en dehors du drawer -->
            <div v-if="open" class="pointer-events-auto absolute inset-0" @click="emit('close')" />

            <Transition enter-active-class="transition-transform duration-300 ease-out"
                enter-from-class="translate-x-full" enter-to-class="translate-x-0"
                leave-active-class="transition-transform duration-200 ease-in" leave-from-class="translate-x-0"
                leave-to-class="translate-x-full">
                <aside v-if="open"
                    class="pointer-events-auto absolute inset-y-0 right-0 flex w-full flex-col overflow-hidden border-l border-slate-200 bg-slate-50 shadow-2xl sm:w-[37vw] sm:min-w-[440px] sm:max-w-[630px]"
                    role="dialog" aria-modal="true" :aria-label="title || 'Panneau de détails'">
                    <header
                        class="flex shrink-0 items-start justify-between border-b border-slate-200 bg-white px-5 py-4">
                        <div class="min-w-0 flex-1">
                            <slot name="header">
                                <h2 class="truncate text-lg font-semibold text-slate-900">
                                    {{ title }}
                                </h2>
                            </slot>
                        </div>

                        <button type="button"
                            class="ml-4 flex h-9 w-9 shrink-0 items-center justify-center rounded-lg text-xl text-slate-500 transition hover:bg-slate-100 hover:text-slate-800"
                            aria-label="Fermer le panneau" @click="emit('close')">
                            ×
                        </button>
                    </header>

                    <div class="min-h-0 flex-1 overflow-y-auto overflow-x-hidden">
                        <slot />
                    </div>

                    <footer v-if="$slots.footer" class="shrink-0 border-t border-slate-200 bg-white px-5 py-4">
                        <slot name="footer" />
                    </footer>
                </aside>
            </Transition>
        </div>
    </Teleport>
</template>