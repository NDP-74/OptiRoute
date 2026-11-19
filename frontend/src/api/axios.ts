import axios from 'axios'
import { useAuthStore } from '@/stores/authStore'

const api = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL,
    headers: {
        'Content-Type': 'application/json'
    }
})

api.interceptors.request.use((config) => {
    const authStore = useAuthStore()

    if (authStore.token) {
        config.headers.Authorization = `Bearer ${authStore.token}`
    }

    return config
})

api.interceptors.response.use(
    (response) => response,

    (error) => {
        const authStore = useAuthStore()

        // Un 401 sur la connexion signale des identifiants invalides, pas une session expirée
        const isLoginRequest = error.config?.url?.endsWith('/auth/login')

        if (error.response?.status === 401 && !isLoginRequest) {
            authStore.logout()
            window.location.href = '/login'
        }

        return Promise.reject(error)
    }
)

export default api