import { apiFetch } from '../../lib/apiClient'
import type { Label, LabelColor } from '../../types/api'

export function listLabels(boardId: string) {
  return apiFetch<Label[]>(`/boards/${boardId}/labels`)
}

export function createLabel(boardId: string, input: { name: string; color: LabelColor }) {
  return apiFetch<Label>(`/boards/${boardId}/labels`, { method: 'POST', body: input })
}

export function updateLabel(labelId: string, input: { name: string; color: LabelColor }) {
  return apiFetch<Label>(`/labels/${labelId}`, { method: 'PATCH', body: input })
}

export function deleteLabel(labelId: string) {
  return apiFetch<void>(`/labels/${labelId}`, { method: 'DELETE' })
}
