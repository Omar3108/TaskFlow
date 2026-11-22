import { apiFetch } from '../../lib/apiClient'
import type { Comment, Page } from '../../types/api'

/** Newest first. */
export function listComments(cardId: string, page: number, size: number) {
  return apiFetch<Page<Comment>>(`/cards/${cardId}/comments?page=${page}&size=${size}`)
}

export function createComment(cardId: string, body: string) {
  return apiFetch<Comment>(`/cards/${cardId}/comments`, { method: 'POST', body: { body } })
}

export function updateComment(commentId: string, body: string) {
  return apiFetch<Comment>(`/comments/${commentId}`, { method: 'PATCH', body: { body } })
}

export function deleteComment(commentId: string) {
  return apiFetch<void>(`/comments/${commentId}`, { method: 'DELETE' })
}
