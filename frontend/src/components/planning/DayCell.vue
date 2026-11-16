<template>
  <div :class="[
    'relative border-r border-slate-200 transition-colors',
    selected ? 'bg-blue-100 ring-1 ring-inset ring-blue-300' : 'bg-slate-50/60',
    selectionEnabled ? 'cursor-pointer hover:bg-blue-50' : ''
  ]" @click="handleCellClick">
    <label v-if="selectionEnabled" :for="checkboxId" class="absolute right-2 top-2 z-20 flex cursor-pointer items-center gap-2 rounded-md bg-white/90 p-1.5 text-xs text-slate-700 shadow-sm">
      <span class="sr-only">{{ selectionLabel }}</span>
      <input :id="checkboxId" type="checkbox" :checked="selected"
        class="h-4 w-4 cursor-pointer rounded border-slate-300 text-blue-600 focus:ring-blue-500"
        :aria-label="selectionLabel" @click.stop @change="emit('selection-toggle')" />
    </label>
  </div>
</template>

<script setup lang="ts">
import { useId } from "vue";

const props = defineProps<{
  selectionEnabled: boolean;
  selected: boolean;
  selectionLabel: string;
}>();

const emit = defineEmits<{
  "selection-toggle": [];
}>();

const checkboxId = useId();

function handleCellClick(): void {
  if (props.selectionEnabled) {
    emit("selection-toggle");
  }
}
</script>
