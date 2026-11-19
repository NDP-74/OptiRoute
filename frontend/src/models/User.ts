export interface User {
    id: number
    email: string
    firstName: string
    lastName: string
}

export interface UpdateUserRequest {
    email: string
    firstName: string
    lastName: string
    password?: string
}