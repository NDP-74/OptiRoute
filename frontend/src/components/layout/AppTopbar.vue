<script setup lang="ts">
import { useAuthStore } from '@/stores/authStore'
import { useRoute, useRouter } from 'vue-router'
import { computed, ref } from 'vue'
import { UserRound } from 'lucide-vue-next'
import AppDropdown from '@/components/ui/AppDropdown.vue'
import { CalendarDays, Settings2, Route, ShieldCheck } from 'lucide-vue-next'

// Variables

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()
const userMenuOpen = ref(false)

const currentUserName = computed(() =>
    `${authStore.firstName} ${authStore.lastName}`.trim() || 'Mon compte'
)

const isRoutesSection = computed(() =>
    route.path.startsWith('/routes')
)

const isHomepageSection = computed(() =>
    route.path.startsWith('/homepage')
)

const isGestionSection = computed(() =>
    route.path.startsWith('/gestion')
)

const isAdminSection = computed(() =>
    route.path.startsWith('/admin')
)

// Functions

function goToRoutes() {
    router.push('/routes')
}

function goToHomepage() {
    router.push('/homepage')
}

function goToGestion() {
    router.push('/gestion')
}

function goToAdmin() {
    router.push('/admin')
}

function handleLogout() {
    userMenuOpen.value = false
    authStore.logout()
    router.push('/login')
}

function goToAccount(close: () => void) {
    close()
    router.push('/user')
}

</script>

<template>
    <header
        class="relative z-10 flex h-14 shrink-0 items-center justify-between gap-2 border-b border-blue-100 bg-gradient-to-r from-[#f8fbff] via-[#f2f7ff] to-[#eaf2ff] px-3 shadow-sm shadow-blue-950/5 sm:px-5">

        <nav aria-label="Navigation principale"
            class="flex min-w-0 items-center gap-0.5 rounded-xl border border-white/80 bg-white/65 p-1 shadow-sm shadow-blue-950/5 sm:gap-1">
            <button type="button" aria-label="Planification" :aria-current="isHomepageSection ? 'page' : undefined"
                class="flex items-center gap-2 rounded-lg px-2.5 py-2 text-sm font-medium transition-colors sm:px-3"
                :class="isHomepageSection ? 'bg-blue-700 text-white shadow-sm shadow-blue-900/15' : 'text-slate-600 hover:bg-blue-50 hover:text-blue-800'"
                @click="goToHomepage">
                <CalendarDays :size="17" />
                <span class="hidden sm:inline">Planification</span>
            </button>

            <button type="button" aria-label="Gestion" :aria-current="isGestionSection ? 'page' : undefined"
                class="flex items-center gap-2 rounded-lg px-2.5 py-2 text-sm font-medium transition-colors sm:px-3"
                :class="isGestionSection ? 'bg-blue-700 text-white shadow-sm shadow-blue-900/15' : 'text-slate-600 hover:bg-blue-50 hover:text-blue-800'"
                @click="goToGestion">
                <Settings2 :size="17" />
                <span class="hidden sm:inline">Gestion</span>
            </button>

            <button type="button" aria-label="Itinéraires" :aria-current="isRoutesSection ? 'page' : undefined"
                class="flex items-center gap-2 rounded-lg px-2.5 py-2 text-sm font-medium transition-colors sm:px-3"
                :class="isRoutesSection ? 'bg-blue-700 text-white shadow-sm shadow-blue-900/15' : 'text-slate-600 hover:bg-blue-50 hover:text-blue-800'"
                @click="goToRoutes">
                <Route :size="17" />
                <span class="hidden sm:inline">Itinéraires</span>
            </button>

            <button type="button" aria-label="Administration" :aria-current="isAdminSection ? 'page' : undefined"
                class="flex items-center gap-2 rounded-lg px-2.5 py-2 text-sm font-medium transition-colors sm:px-3"
                :class="isAdminSection ? 'bg-blue-700 text-white shadow-sm shadow-blue-900/15' : 'text-slate-600 hover:bg-blue-50 hover:text-blue-800'"
                @click="goToAdmin">
                <ShieldCheck :size="17" />
                <span class="hidden sm:inline">Admin</span>
            </button>
        </nav>

        <div class="flex shrink-0 items-center">

            <AppDropdown v-model:open="userMenuOpen">
                <template #default="{ close }">
                    <button type="button"
                        class="flex w-full items-center gap-2 px-4 py-2.5 text-left text-sm text-slate-700 transition-colors hover:bg-blue-50 hover:text-blue-800"
                        @click="goToAccount(close)">
                        Mon compte
                    </button>

                    <button type="button"
                        class="flex w-full items-center gap-2 px-4 py-2.5 text-left text-sm text-red-600 transition-colors hover:bg-red-50"
                        @click="handleLogout(); close()">
                        Se déconnecter
                    </button>
                </template>

                <template #trigger="{ toggle }">
                    <button type="button"
                        class="inline-flex items-center gap-2 rounded-xl border border-blue-100/80 bg-white/70 px-2 py-1.5 text-slate-700 transition-colors hover:border-blue-200 hover:bg-white sm:px-3"
                        aria-label="Menu utilisateur" @click.stop="toggle">
                        <span class="flex h-8 w-8 items-center justify-center rounded-lg bg-blue-100 text-blue-800">
                            <UserRound class="h-4 w-4" />
                        </span>
                        <span class="hidden max-w-36 truncate text-sm font-medium md:inline">{{ currentUserName
                            }}</span>
                    </button>
                </template>
            </AppDropdown>

        </div>

    </header>
</template>