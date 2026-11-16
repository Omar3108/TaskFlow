import { apiFetch } from '../../lib/apiClient'
import type { AuthResponse, User } from '../../types/api'

export interface LoginPayload {
  email: string
  password: string
}

export interface RegisterPayload extends LoginPayload {
  displayName: string
}

export function login(payload: LoginPayload) {
  return apiFetch<AuthResponse>('/auth/login', { method: 'POST', body: payload, auth: false })
}

export function register(payload: RegisterPayload) {
  return apiFetch<AuthResponse>('/auth/register', { method: 'POST', body: payload, auth: false })
}

export function logout() {
  return apiFetch<void>('/auth/logout', { method: 'POST', auth: false })
}

export function fetchCurrentUser() {
  return apiFetch<User>('/users/me')
}
