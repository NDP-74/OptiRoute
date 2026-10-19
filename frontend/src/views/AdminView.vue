<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Search, UserRound, Users, X } from 'lucide-vue-next'

import { getUsers } from '@/api/userApi'

import type { User } from '@/models/User'

import CreateUserModal from '@/components/users/CreateUserModal.vue'
import DeleteUserModal from '@/components/users/DeleteUserModal.vue'
import EditUserModal from '@/components/users/EditUserModal.vue'
import AppDropdown from '@/components/ui/AppDropdown.vue'

const users = ref<User[]>([])
const searchQuery = ref('')

const showCreateModal = ref(false)

const filteredUsers = computed(() => {
    const query = searchQuery.value.trim().toLocaleLowerCase()
    if (!query) {
        return users.value
    }

    return users.value.filter(user =>
        `${user.firstName} ${user.lastName} ${user.email}`.toLocaleLowerCase().includes(query),
    )
})

const loadUsers = async () => {
    users.value = await getUsers()
}

onMounted(async () => {
    await loadUsers()
})

type UserAction = 'edit' | 'delete' | null

const activeAction = ref<UserAction>(null)
const selectedUser = ref<User | null>(null)
const openDropdownId = ref<number | null>(null)

const openEditUserModal = (user: User) => {
    selectedUser.value = user
    activeAction.value = 'edit'
}

const askDeleteUser = (user: User) => {
    selectedUser.value = user
    activeAction.value = 'delete'
}

const closeUserAction = () => {
    selectedUser.value = null
    activeAction.value = null
}

</script>

<template>
    <div class="mx-auto max-w-5xl space-y-6">
        <header class="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
            <div>
                <h1 class="text-2xl font-semibold tracking-tight text-slate-900">Utilisateurs</h1>
            </div>

            <button type="button" @click="showCreateModal = true"
                class="inline-flex items-center justify-center gap-2 rounded-xl bg-blue-600 px-4 py-2.5 text-sm font-medium text-white shadow-sm transition hover:bg-blue-700 focus:outline-none focus:ring-4 focus:ring-blue-100">
                <Users class="h-4 w-4" />
                Ajouter un utilisateur
            </button>
        </header>

        <section class="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
            <div
                class="flex flex-col gap-3 border-b border-slate-100 px-4 py-4 sm:flex-row sm:items-center sm:justify-between sm:px-5">
                <div class="flex items-center gap-2 text-sm text-slate-500">
                    <span class="font-semibold text-slate-900">{{ users.length }}</span>
                    <span>{{ users.length === 1 ? 'utilisateur' : 'utilisateurs' }}</span>
                    <span v-if="searchQuery.trim()" class="text-slate-400">· {{ filteredUsers.length }}
                        résultat(s)</span>
                </div>

                <div class="relative w-full sm:max-w-xs">
                    <Search
                        class="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                    <input v-model="searchQuery" type="search" placeholder="Rechercher un nom ou un email"
                        aria-label="Rechercher un utilisateur"
                        class="w-full rounded-xl border border-slate-200 bg-slate-50 py-2.5 pl-9 pr-9 text-sm text-slate-700 outline-none transition placeholder:text-slate-400 focus:border-blue-400 focus:bg-white focus:ring-4 focus:ring-blue-50" />
                    <button v-if="searchQuery" type="button" aria-label="Effacer la recherche" @click="searchQuery = ''"
                        class="absolute right-2 top-1/2 -translate-y-1/2 rounded-lg p-1 text-slate-400 transition hover:bg-slate-200 hover:text-slate-700">
                        <X class="h-4 w-4" />
                    </button>
                </div>
            </div>

            <div class="overflow-x-auto">
                <table class="w-full min-w-[520px] text-left">
                    <thead class="bg-slate-50/80 text-xs font-semibold uppercase tracking-wide text-slate-500">
                        <tr>
                            <th scope="col" class="px-5 py-3">Utilisateur</th>
                            <th scope="col" class="px-5 py-3">Email</th>
                            <th scope="col" class="w-16 px-5 py-3 text-right">Actions</th>
                        </tr>
                    </thead>

                    <tbody class="divide-y divide-slate-100">
                        <tr v-for="user in filteredUsers" :key="user.id" class="transition hover:bg-slate-50/70">
                            <td class="px-5 py-3.5">
                                <div class="flex items-center gap-3">
                                    <span
                                        class="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-blue-50 text-xs font-semibold text-blue-700">
                                        {{ `${user.firstName.charAt(0)}${user.lastName.charAt(0)}`.toLocaleUpperCase()
                                        }}
                                    </span>
                                    <span class="font-medium text-slate-800">{{ user.firstName }} {{ user.lastName
                                        }}</span>
                                </div>
                            </td>
                            <td class="px-5 py-3.5 text-sm text-slate-500">{{ user.email }}</td>
                            <td class="px-5 py-3.5 text-right">
                                <AppDropdown :open="openDropdownId === user.id" @update:open="value => {
                                    openDropdownId = value ? user.id : null
                                }" v-slot="{ close }">
                                    <button type="button"
                                        class="flex w-full items-center px-4 py-2 text-sm text-slate-700 hover:bg-slate-50"
                                        @click="close(); openEditUserModal(user)">
                                        Modifier
                                    </button>
                                    <button type="button"
                                        class="flex w-full items-center px-4 py-2 text-sm text-red-600 hover:bg-red-50"
                                        @click="close(); askDeleteUser(user)">
                                        Supprimer
                                    </button>
                                </AppDropdown>
                            </td>
                        </tr>

                        <tr v-if="filteredUsers.length === 0">
                            <td colspan="3" class="px-5 py-12 text-center">
                                <div
                                    class="mx-auto flex h-11 w-11 items-center justify-center rounded-full bg-slate-100 text-slate-500">
                                    <UserRound class="h-5 w-5" />
                                </div>
                                <p class="mt-3 text-sm font-medium text-slate-800">
                                    {{ users.length === 0 ? 'Aucun utilisateur pour le moment' : 'Aucun résultat' }}
                                </p>
                                <p class="mt-1 text-sm text-slate-500">
                                    {{ users.length === 0 ? 'Ajoutez un utilisateur pour lui donner accès à OptiRoute.'
                                        : 'Essayez avec un autre nom ou une autre adresse email.' }}
                                </p>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </section>

        <CreateUserModal :show="showCreateModal" @close="showCreateModal = false" @created="loadUsers" />

        <DeleteUserModal :show="activeAction === 'delete'" :user="selectedUser" @close="closeUserAction"
            @deleted="loadUsers" />

        <EditUserModal :show="activeAction === 'edit'" :user="selectedUser" @close="closeUserAction"
            @updated="loadUsers" />
    </div>
</template>