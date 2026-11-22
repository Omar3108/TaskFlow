import { apiFetch } from '../../lib/apiClient'
import type { User } from '../../types/api'

export function updateProfile(displayName: string) {
  return apiFetch<User>('/users/me', { method: 'PATCH', body: { displayName } })
}
