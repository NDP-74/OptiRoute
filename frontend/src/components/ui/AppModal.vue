<script setup lang="ts">
withDefaults(defineProps<{
  show: boolean
  panelClass?: string
}>(), {
  panelClass: 'max-w-3xl'
})

const emit = defineEmits(['close'])

</script>

<template>
  <Teleport to="body">
    <Transition enter-active-class="transition duration-200 ease-out" enter-from-class="opacity-0"
      enter-to-class="opacity-100" leave-active-class="transition duration-150 ease-in" leave-from-class="opacity-100"
      leave-to-class="opacity-0">
      <div v-if="show" class="fixed inset-0 z-[100] flex items-center justify-center p-4" @keydown.esc="emit('close')">
        <div class="absolute inset-0 bg-slate-950/50 backdrop-blur-sm" @click="emit('close')" />

        <div :class="['relative z-10 w-full rounded-2xl bg-white p-6 shadow-2xl', panelClass]" role="dialog"
          aria-modal="true" @click.stop>
          <slot />
        </div>
      </div>
    </Transition>
  </Teleport>
</template>