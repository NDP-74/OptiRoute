import api from './axios'
import type { CreateUserRequest, UpdateUserRequest, User } from '@/models/User'

export const getCurrentUser = async (): Promise<User> => {
  const response = await api.get<User>('/users/me')
  return response.data
}

export const getUsers = async () => {
  const response = await api.get('/users')
  return response.data
}

export const createUser = async (user: CreateUserRequest): Promise<User> => {
  const response = await api.post<User>('/users', user)
  return response.data
}

export const deleteUser = async (id: number) => {
  await api.delete(`/users/${id}`)
}

export const updateUser = async (id: number, user: UpdateUserRequest) => {
  const response = await api.put(`/users/${id}`, user)
  return response.data
}