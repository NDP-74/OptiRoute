<script setup lang="ts">
import { onMounted, ref } from "vue"

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

    <div>
        <div class="mb-6 flex items-center justify-between">
            <h1 class="text-3xl font-bold text-gray-800">
                Donneurs d’ordres
            </h1>
            <button type="button"
                class="rounded-xl bg-blue-600 px-5 py-3 text-white shadow transition hover:bg-blue-700"
                @click="showCreateModal = true">
                Ajouter un donneur d’ordre
            </button>
        </div>

        <div class="rounded-2xl bg-white shadow">
            <table class="w-full">
                <thead class="border-b bg-gray-50">
                    <tr>
                        <th class="whitespace-nowrap px-6 py-4 text-left">Nom</th>
                        <th class="whitespace-nowrap px-6 py-4 text-left">Code</th>
                        <th class="whitespace-nowrap px-6 py-4 text-left">Ville</th>
                        <th class="whitespace-nowrap px-6 py-4 text-right">Actions</th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-for="customer in customers" :key="customer.id" tabindex="0"
                        class="cursor-pointer border-b transition last:border-b-0 hover:bg-gray-100"
                        :class="{ 'bg-blue-50': selectedCustomerId === customer.id }"
                        @click="openCustomerDetails(customer)" @keydown.enter="openCustomerDetails(customer)"
                        @keydown.space.prevent="openCustomerDetails(customer)">
                        <td class="whitespace-nowrap px-6 py-4 font-medium text-gray-900">
                            {{ customer.name }}
                        </td>
                        <td class="whitespace-nowrap px-6 py-4">
                            {{ customer.code || 'Non renseigné' }}
                        </td>
                        <td class="whitespace-nowrap px-6 py-4">
                            {{ customer.city || 'Non renseignée' }}
                        </td>
                        <td class="whitespace-nowrap px-6 py-4 text-right" @click.stop @keydown.stop>
                            <AppDropdown :open="openDropdownId === customer.id" @update:open="value => {
                                openDropdownId = value ? customer.id : null
                            }" v-slot="{ close }">
                                <button type="button"
                                    class="flex w-full items-center px-4 py-2 text-sm text-red-600 hover:bg-red-50"
                                    @click="close(); askDeleteCustomer(customer)">
                                    Supprimer
                                </button>
                            </AppDropdown>
                        </td>
                    </tr>
                    <tr v-if="customers.length === 0">
                        <td colspan="4" class="px-6 py-12 text-center text-sm text-gray-500">
                            Aucun donneur d’ordre enregistré.
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