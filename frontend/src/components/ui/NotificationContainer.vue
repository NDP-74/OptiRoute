<script setup lang="ts">
import {
    CheckCircleIcon,
    ExclamationCircleIcon,
    ExclamationTriangleIcon,
    InformationCircleIcon,
    XMarkIcon
} from '@heroicons/vue/24/outline'

import { type NotificationType, useNotification } from '@/composables/useNotification.ts'

const { notifications, removeNotification } = useNotification()

function notificationClasses(type: NotificationType) {
    switch (type) {
        case 'success':
            return {
                container: 'border-emerald-200 bg-white',
                icon: 'text-emerald-500'
            }

        case 'error':
            return {
                container: 'border-red-200 bg-white',
                icon: 'text-red-500'
            }

        case 'warning':
            return {
                container: 'border-amber-200 bg-white',
                icon: 'text-amber-500'
            }

        case 'info':
        default:
            return {
                container: 'border-blue-200 bg-white',
                icon: 'text-blue-500'
            }
    }
}

function notificationIcon(type: NotificationType) {
    switch (type) {
        case 'success':
            return CheckCircleIcon

        case 'error':
            return ExclamationCircleIcon

        case 'warning':
            return ExclamationTriangleIcon

        case 'info':
        default:
            return InformationCircleIcon
    }
}
</script>

<template>
    <Teleport to="body">
        <div class="pointer-events-none fixed inset-x-0 top-4 z-[9999] flex flex-col items-center gap-3 px-4">
            <TransitionGroup enter-active-class="transition duration-200 ease-out"
                enter-from-class="-translate-y-3 opacity-0" enter-to-class="translate-y-0 opacity-100"
                leave-active-class="transition duration-150 ease-in" leave-from-class="translate-y-0 opacity-100"
                leave-to-class="-translate-y-3 opacity-0">
                <div v-for="notification in notifications" :key="notification.id"
                    :class="notificationClasses(notification.type).container"
                    class="pointer-events-auto flex w-full max-w-md items-start gap-3 rounded-xl border p-4 shadow-lg"
                    role="status">
                    <component :is="notificationIcon(notification.type)"
                        :class="notificationClasses(notification.type).icon" class="mt-0.5 size-6 shrink-0" />

                    <div class="min-w-0 flex-1">
                        <p class="font-semibold text-slate-900">
                            {{ notification.title }}
                        </p>

                        <p v-if="notification.message" class="mt-1 text-sm text-slate-600">
                            {{ notification.message }}
                        </p>
                    </div>

                    <button type="button"
                        class="rounded-md p-1 text-slate-400 transition hover:bg-slate-100 hover:text-slate-600"
                        aria-label="Fermer la notification" @click="removeNotification(notification.id)">
                        <XMarkIcon class="size-5" />
                    </button>
                </div>
            </TransitionGroup>
        </div>
    </Teleport>
</template>