<script setup lang="ts">
import { computed, onMounted, ref } from "vue"
import { MoreHorizontal, Plus, Search } from "lucide-vue-next"

import { getCustomers } from "@/api/customerApi"

import type { CustomerSummary } from "@/models/Customer"

import CreateCustomerModal from "@/components/customers/CreateCustomerModal.vue"
import CustomerDetailDrawer from "@/components/customers/CustomerDetailDrawer.vue"
import DeleteCustomerModal from "@/components/customers/DeleteCustomerModal.vue"
import AppDropdown from "@/components/ui/AppDropdown.vue"


const customers = ref<CustomerSummary[]>([])

const loadCustomers = async () => {
    customers.value = await getCustomers()
}

const searchQuery = ref("")
const visibleCustomers = computed(() => {
    const normalizedQuery = searchQuery.value.trim().toLocaleLowerCase("fr")

    return customers.value.filter(customer =>
        customer.name.toLocaleLowerCase("fr").includes(normalizedQuery),
    )
})

const showCreateModal = ref(false)
const selectedCustomerId = ref<number | null>(null)
const customerToDelete = ref<CustomerSummary | null>(null)
const openDropdownId = ref<number | null>(null)

const openCustomerDetails = (customer: CustomerSummary) => {
    selectedCustomerId.value = customer.id
}

const closeCustomerDetails = () => {
    selectedCustomerId.value = null
}

const askDeleteCustomer = (customer: CustomerSummary) => {
    customerToDelete.value = customer
}

const closeDeleteCustomer = () => {
    customerToDelete.value = null
}

const handleCustomerCreated = async () => {
    showCreateModal.value = false
    await loadCustomers()
}

const handleCustomerDeleted = async () => {
    closeDeleteCustomer()
    closeCustomerDetails()
    await loadCustomers()
}

onMounted(async () => {
    await loadCustomers()
})

</script>

<template>

    <div class="w-full max-w-3xl">
        <div class="mb-6 flex flex-col items-start gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div>
                <h1 class="text-2xl font-semibold leading-7 tracking-tight text-gray-900">
                    Donneurs d’ordres
                </h1>
                <p class="mt-1 text-sm leading-6 text-gray-500">
                    Consultez et gérez vos donneurs d’ordres.
                </p>
            </div>
            <button type="button"
                class="inline-flex shrink-0 items-center justify-center gap-2 rounded-md bg-indigo-600 px-3 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-indigo-500 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-600"
                @click="showCreateModal = true">
                <Plus class="h-4 w-4" aria-hidden="true" />
                Ajouter un donneur d’ordre
            </button>
        </div>

        <div class="mb-4 rounded-xl border border-gray-200 bg-white p-4 shadow-sm">
            <div class="w-full sm:max-w-xs">
                <label for="customer-search" class="mb-1.5 block text-sm font-medium text-gray-700">
                    Rechercher
                </label>
                <div class="relative">
                    <Search class="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400"
                        aria-hidden="true" />
                    <input id="customer-search" v-model="searchQuery" type="search"
                        placeholder="Nom du donneur d’ordre..."
                        class="w-full rounded-md border border-gray-300 bg-white py-2 pl-9 pr-3 text-sm text-gray-700 shadow-sm outline-none transition placeholder:text-gray-400 hover:border-gray-400 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20" />
                </div>
            </div>
        </div>

        <div class="max-h-[calc(100vh-18rem)] w-full max-w-3xl overflow-auto rounded-xl border border-gray-200 bg-white shadow-sm">
            <table class="min-w-full divide-y divide-gray-200">
                <thead class="sticky top-0 z-10 bg-gray-50">
                    <tr>
                        <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-semibold text-gray-700">Nom</th>
                        <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-semibold text-gray-700">Code</th>
                        <th class="whitespace-nowrap px-4 py-3 text-right text-sm font-semibold text-gray-700">Actions</th>
                    </tr>
                </thead>
                <tbody class="divide-y divide-gray-100">
                    <tr v-for="customer in visibleCustomers" :key="customer.id" tabindex="0"
                        class="cursor-pointer transition-colors hover:bg-gray-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-indigo-500"
                        :class="{ 'bg-indigo-50': selectedCustomerId === customer.id }"
                        @click="openCustomerDetails(customer)" @keydown.enter="openCustomerDetails(customer)"
                        @keydown.space.prevent="openCustomerDetails(customer)">
                        <td class="whitespace-nowrap px-4 py-3 text-sm font-medium text-gray-900">
                            {{ customer.name }}
                        </td>
                        <td class="whitespace-nowrap px-4 py-3 text-sm text-gray-600">
                            {{ customer.code || 'Non renseigné' }}
                        </td>
                        <td class="whitespace-nowrap px-4 py-3 text-right" @click.stop @keydown.stop>
                            <AppDropdown :open="openDropdownId === customer.id" @update:open="value => {
                                openDropdownId = value ? customer.id : null
                            }">
                                <template #trigger="{ toggle }">
                                    <button type="button"
                                        class="rounded-md p-1.5 text-gray-400 transition hover:bg-gray-100 hover:text-gray-700 focus-visible:outline focus-visible:outline-2 focus-visible:outline-indigo-600"
                                        :aria-label="`Actions pour ${customer.name}`" @click.stop="toggle">
                                        <MoreHorizontal class="h-5 w-5" aria-hidden="true" />
                                    </button>
                                </template>
                                <template #default="{ close }">
                                    <button type="button"
                                        class="flex w-full items-center px-3 py-2.5 text-left text-sm text-red-600 transition hover:bg-red-50 focus:bg-red-50 focus:outline-none"
                                        @click="close(); askDeleteCustomer(customer)">
                                        Supprimer
                                    </button>
                                </template>
                            </AppDropdown>
                        </td>
                    </tr>
                    <tr v-if="visibleCustomers.length === 0">
                        <td colspan="3" class="px-4 py-10 text-center text-sm text-gray-500">
                            {{ customers.length === 0
                                ? "Aucun donneur d’ordre enregistré."
                                : "Aucun donneur d’ordre ne correspond à votre recherche." }}
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>

        <CustomerDetailDrawer :open="selectedCustomerId !== null" :customer-id="selectedCustomerId"
            @close="closeCustomerDetails" @updated="loadCustomers" @deleted="handleCustomerDeleted" />
        <CreateCustomerModal :show="showCreateModal" @close="showCreateModal = false"
            @created="handleCustomerCreated" />
        <DeleteCustomerModal :show="customerToDelete !== null" :customer="customerToDelete" @close="closeDeleteCustomer"
            @deleted="handleCustomerDeleted" />
    </div>
</template>