<script setup lang="ts">
import { nextTick, ref } from "vue"

import { formatVehicleRegistrationInput, normalizeVehicleRegistration } from "@/utils/vehicleUtils"

const registration = defineModel<string>({
    required: true,
})

withDefaults(
    defineProps<{ disabled?: boolean }>(),
    {
        disabled: false,
    },
)

const registrationInput = ref<HTMLInputElement | null>(null)

const getFormattedCaretPosition = (characterCount: number): number =>
    characterCount + Number(characterCount >= 2) + Number(characterCount >= 5)

const updateRegistration = async (value: string, caretCharacterCount: number) => {
    registration.value = normalizeVehicleRegistration(value)

    await nextTick()
    const caretPosition = getFormattedCaretPosition(caretCharacterCount)
    registrationInput.value?.setSelectionRange(caretPosition, caretPosition)
}

const handleInput = (event: Event) => {
    if (!(event.currentTarget instanceof HTMLInputElement)) {
        return
    }

    const input = event.currentTarget
    const caretPosition = input.selectionStart ?? input.value.length
    const caretCharacterCount = normalizeVehicleRegistration(input.value.slice(0, caretPosition)).length

    void updateRegistration(input.value, caretCharacterCount)
}

const handleKeydown = (event: KeyboardEvent) => {
    const input = registrationInput.value
    if (!input || input.selectionStart !== input.selectionEnd) {
        return
    }

    const caretPosition = input.selectionStart ?? input.value.length
    const isBackspaceBeforeSeparator = event.key === "Backspace" && input.value[caretPosition - 1] === "-"
    const isDeleteBeforeSeparator = event.key === "Delete" && input.value[caretPosition] === "-"

    if (!isBackspaceBeforeSeparator && !isDeleteBeforeSeparator) {
        return
    }

    event.preventDefault()

    const rawRegistration = normalizeVehicleRegistration(registration.value)
    const rawCharacterIndex = isBackspaceBeforeSeparator
        ? normalizeVehicleRegistration(input.value.slice(0, caretPosition - 1)).length - 1
        : normalizeVehicleRegistration(input.value.slice(0, caretPosition + 1)).length
    const caretCharacterCount = isBackspaceBeforeSeparator
        ? Math.max(rawCharacterIndex, 0)
        : normalizeVehicleRegistration(input.value.slice(0, caretPosition)).length

    if (rawCharacterIndex >= 0 && rawCharacterIndex < rawRegistration.length) {
        const updatedRegistration = rawRegistration.slice(0, rawCharacterIndex)
            + rawRegistration.slice(rawCharacterIndex + 1)
        void updateRegistration(updatedRegistration, caretCharacterCount)
    }
}
</script>

<template>
    <input ref="registrationInput" :value="formatVehicleRegistrationInput(registration)" type="text"
        required minlength="9" maxlength="9" pattern="[A-Z0-9]{2}-[A-Z0-9]{3}-[A-Z0-9]{2}"
        title="Format attendu : 2 caractères, 3 caractères, puis 2 caractères" placeholder="AB-123-CD"
        aria-label="Immatriculation" @input="handleInput" @keydown="handleKeydown" :disabled="disabled"
        class="w-full rounded-xl border border-slate-300 px-4 py-3 uppercase outline-none transition focus:border-blue-500 disabled:cursor-not-allowed disabled:bg-slate-100" />
</template>
