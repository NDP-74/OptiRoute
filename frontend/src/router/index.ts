import { createRouter, createWebHistory } from 'vue-router'

import OperationCostsView from '@/views/OperationCostsView.vue'
import AdminView from '@/views/AdminView.vue'
import LoginView from '@/views/users/LoginView.vue'
import AccountView from '@/views/users/AccountView.vue'
import ChangePasswordView from '@/views/users/ChangePasswordView.vue'
import DriversView from '@/views/DriversView.vue'
import MapsView from '@/views/MapsView.vue'
import PointOfInterestView from '@/views/PointOfInterestView.vue'
import PlanningView from '@/views/PlanningView.vue'
import CustomersView from '@/views/CustomersView.vue'
import TractorsView from '@/views/vehicle/TractorsView.vue'
import SemiTrailersView from '@/views/vehicle/SemiTrailersView.vue'

import MainLayout from '@/layouts/MainLayout.vue'
import MapLayout from '@/layouts/MapLayout.vue'
import PlanningLayout from '@/layouts/PlanningLayout.vue'

import { useAuthStore } from '@/stores/authStore'


const routes = [
    {
        path: '/login',
        name: 'login',
        component: LoginView,
        meta: { public: true }
    },

    // REDIRECTION

    {
        path: '/',
        redirect: '/homepage/planning'
    },

    {
        path: '/homepage',
        redirect: '/homepage/planning'
    },

    {
        path: '/gestion',
        redirect: '/gestion/operationCosts'
    },

    {
        path: '/admin',
        redirect: '/admin/homepage'
    },

    // MAIN APP LAYOUT (padding / standard pages)

    {
        path: '/',
        component: MainLayout,
        meta: { requiresAuth: true },

        children: [
            {
                path: 'gestion',
                children: [
                    {
                        path: 'operationCosts',
                        name: 'operationCosts',
                        component: OperationCostsView
                    },
                    {
                        path: 'drivers',
                        name: 'drivers',
                        component: DriversView
                    },
                    {
                        path: 'tractors',
                        name: 'tractors',
                        component: TractorsView
                    },
                    {
                        path: 'semiTrailers',
                        name: 'semiTrailers',
                        component: SemiTrailersView
                    },
                    {
                        path: 'customers',
                        name: 'customers',
                        component: CustomersView
                    }
                ]
            },

            {
                path: 'admin',
                children: [
                    {
                        path: 'homepage',
                        name: 'homepage',
                        component: AdminView
                    }
                ]
            },

            {
                path: 'user',
                children: [
                    {
                        path: '',
                        name: 'account',
                        component: AccountView
                    },
                    {
                        path: 'password',
                        name: 'changePassword',
                        component: ChangePasswordView
                    }
                ]
            }
        ]
    },

    // MAP LAYOUT (full screen / no padding)

    {

        path: '/',
        component: MapLayout,
        meta: { requiresAuth: true },

        children: [
            {
                path: 'routes',
                children: [
                    {
                        path: '',
                        name: 'routes',
                        component: MapsView
                    },
                    {
                        path: 'pointsOfInterest',
                        name: 'pointsOfInterest',
                        component: PointOfInterestView
                    }
                ]
            }
        ]
    },

    // PLANNING LAYOUT (full screen / no padding)

    {

        path: '/homepage',
        component: PlanningLayout,
        meta: { requiresAuth: true },

        children: [
            {
                path: 'planning',
                children: [
                    {
                        path: '',
                        name: 'planning',
                        component: PlanningView
                    }
                ]
            }
        ]
    }
]

// Router configurations

const router = createRouter({
    history: createWebHistory(),
    routes
})

router.beforeEach((to, from, next) => {
    const authStore = useAuthStore()

    const requiresAuth = to.matched.some(
        record => record.meta.requiresAuth
    )

    const isPublic = to.matched.some(
        record => record.meta.public
    )

    if (requiresAuth && !authStore.isAuthenticated) {
        return next('/login')
    }

    if (isPublic && authStore.isAuthenticated) {
        return next('/homepage')
    }

    next()
})

export default router