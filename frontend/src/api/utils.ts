import axios, { type AxiosError } from 'axios'

// Message explicatif renvoyé par le backend (ApiErrorResponse.message)
function serverMessage(error: AxiosError): string | undefined {
    const message = (error.response?.data as { message?: unknown } | undefined)?.message
    return typeof message === 'string' && message ? message : undefined
}

export function getApiErrorMessage(
    error: unknown,
    fallback = 'Une erreur est survenue.'
): string {
    if (!axios.isAxiosError(error)) {
        return fallback
    }

    if (error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT') {
        return 'Le délai de réponse est dépassé. Vérifiez la connexion au serveur et réessayez.'
    }

    const status = error.response?.status;

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
            return serverMessage(error) ?? 'Cette opération entre en conflit avec des données existantes.'

        case 422:
            return serverMessage(error) ?? fallback

        case 500:
            return 'Le serveur a rencontré une erreur.'

        case 502:
            return serverMessage(error) ?? 'Un service externe est indisponible. Réessayez dans quelques instants.'

        case 503:
            return serverMessage(error) ?? 'Le service demandé n’est pas configuré.'

        case 504:
            return serverMessage(error) ?? 'Dashdoc met trop de temps à répondre. Réessayez dans quelques instants.'

        default:
            return fallback
    }
}