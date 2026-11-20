<script setup lang="ts">
import { ref, watch } from "vue"

import AppModal from "@/components/ui/AppModal.vue"

import { createUser, updateUser } from "@/api/userApi"
import { getApiErrorMessage } from "@/api/utils"

import type { CreateUserRequest, User } from "@/models/User"
import { useNotification } from "@/composables/useNotification"

const notification = useNotification()

const props = defineProps<{
    show: boolean
    user?: User | null
}>()

const emit = defineEmits<{
    close: []
    created: []
    updated: []
}>()

const email = ref("")
const password = ref("")
const firstName = ref("")
const lastName = ref("")
const loading = ref(false)

const resetForm = () => {
    email.value = ""
    password.value = ""
    firstName.value = ""
    lastName.value = ""
}

const syncForm = () => {
    if (props.user) {
        email.value = props.user.email
        firstName.value = props.user.firstName
        lastName.value = props.user.lastName
        password.value = ""
    } else {
        resetForm()
    }
}

const closeModal = () => {
    if (!loading.value) {
        emit("close")
    }
}

const submitUser = async () => {
    try {
        loading.value = true

        if (props.user) {
            await updateUser(props.user.id, {
                email: email.value,
                firstName: firstName.value,
                lastName: lastName.value,
                password: password.value || undefined,
            })

            notification.success(
                "Utilisateur modifié",
                `L'utilisateur « ${firstName.value} ${lastName.value} » a bien été mis à jour.`,
            )
            emit("updated")
            emit("close")
        } else {
            const payload: CreateUserRequest = {
                email: email.value,
                password: password.value,
                firstName: firstName.value,
                lastName: lastName.value,
            }

            await createUser(payload)

            notification.success(
                "Utilisateur enregistré",
                `L'utilisateur « ${firstName.value} ${lastName.value} » a bien été ajouté.`,
            )
            emit("created")
            emit("close")
        }
    } catch (error: unknown) {
        const action = props.user ? "Modification" : "Enregistrement"
        const fallback = props.user
            ? "L'utilisateur n’a pas pu être modifié."
            : "L'utilisateur n’a pas pu être ajouté."

        notification.error(action === "Modification" ? "Modification impossible" : "Enregistrement impossible",
            getApiErrorMessage(error, fallback))
    } finally {
        loading.value = false
    }
}

watch(
    [() => props.show, () => props.user],
    ([show]) => {
        if (show) {
            syncForm()
        } else {
            resetForm()
        }
    },
    { immediate: true },
)
</script>

<template>
    <AppModal :show="props.show" @close="closeModal">
        <form @submit.prevent="submitUser">
            <h2 class="mb-6 text-2xl font-bold">
                {{ props.user ? "Modifier l'utilisateur" : "Ajouter un nouvel utilisateur" }}
            </h2>

            <div class="space-y-4">
                <div>
                    <label class="mb-1 block text-sm font-medium text-gray-700">
                        Email
                    </label>

                    <input v-model="email" type="email" required :disabled="loading"
                        class="w-full rounded-xl border px-4 py-3 outline-none focus:border-blue-500" />
                </div>

                <div>
                    <label class="mb-1 block text-sm font-medium text-gray-700">
                        {{ props.user ? "Nouveau mot de passe" : "Mot de passe" }}
                    </label>

                    <input v-model="password" type="password" :required="!props.user" :disabled="loading"
                        :placeholder="props.user ? 'Optionnel' : undefined"
                        class="w-full rounded-xl border px-4 py-3 outline-none focus:border-blue-500" />
                </div>

                <div>
                    <label class="mb-1 block text-sm font-medium text-gray-700">
                        Prénom
                    </label>

                    <input v-model="firstName" type="text" required :disabled="loading"
                        class="w-full rounded-xl border px-4 py-3 outline-none focus:border-blue-500" />
                </div>

                <div>
                    <label class="mb-1 block text-sm font-medium text-gray-700">
                        Nom
                    </label>

                    <input v-model="lastName" type="text" required :disabled="loading"
                        class="w-full rounded-xl border px-4 py-3 outline-none focus:border-blue-500" />
                </div>
            </div>

            <div class="mt-6 flex justify-end gap-3">
                <button type="button" :disabled="loading" class="rounded-xl border px-4 py-2" @click="closeModal">
                    Annuler
                </button>

                <button type="submit" :disabled="loading"
                    class="rounded-xl bg-blue-600 px-4 py-2 text-white transition hover:bg-blue-700 disabled:opacity-50">
                    {{ loading ? "Enregistrement" : "Enregistrer" }}
                </button>
            </div>
        </form>
    </AppModal>
</template>
