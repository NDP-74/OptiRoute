<template>
    <RouterLink :to="to" class="flex items-center gap-3 rounded-lg px-4 py-2.5 font-medium transition-colors" :class="isActive
        ? 'bg-[#1b426a] text-white shadow-[inset_3px_0_0_#48a3ff]'
        : 'text-slate-200 hover:bg-[#1d3555] hover:text-white'">

        <component v-if="icon" :is="icon" class="h-4 w-4" />
        <span>{{ label }}</span>

    </RouterLink>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import type { Component } from 'vue'

const props = defineProps<{
    to: string
    label: string
    icon?: Component
}>()

const route = useRoute()

const isActive = computed(() =>
    route.path === props.to
)
</script>