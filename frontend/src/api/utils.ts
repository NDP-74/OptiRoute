import axios from 'axios'

export function getApiErrorMessage(
    error: unknown,
    fallback = 'Une erreur est survenue.'
): string {
    if (!axios.isAxiosError(error)) {
        return fallback
    }

    const status = error.response?.status

    switch (status) {
        case 400:
            return 'Les informations envoyées sont invalides.'

        case 401:
            return 'Votre session a expiré. Veuillez vous reconnecter.'

        case 403:
            return 'Vous n’êtes pas autorisé à effectuer cette action.'

        case 404:
            return 'La ressource demandée est introuvable.'

        case 409:
            return 'Cette opération entre en conflit avec des données existantes.'

        case 422: {
            const message = (error.response?.data as { message?: unknown } | undefined)?.message
            return typeof message === 'string' && message ? message : fallback
        }

        case 500:
            return 'Le serveur a rencontré une erreur.'

        default:
            return fallback
    }
}