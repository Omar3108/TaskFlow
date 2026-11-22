import { apiFetch } from '../../lib/apiClient'
import type { Card } from '../../types/api'

export function createCard(columnId: string, title: string) {
  return apiFetch<Card>(`/columns/${columnId}/cards`, { method: 'POST', body: { title } })
}

export function getCard(cardId: string) {
  return apiFetch<Card>(`/cards/${cardId}`)
}

export interface UpdateCardInput {
  title: string
  description: string | null
  assigneeId: string | null
  dueDate: string | null
  /** Labels of the card's board; the ones left out are removed. */
  labelIds: string[]
  /** The version that was read; the server answers 409 if somebody saved in between. */
  version: number
}

/** Replaces every editable field: null clears it. */
export function updateCard(cardId: string, input: UpdateCardInput) {
  return apiFetch<Card>(`/cards/${cardId}`, { method: 'PATCH', body: input })
}

export function deleteCard(cardId: string) {
  return apiFetch<void>(`/cards/${cardId}`, { method: 'DELETE' })
}

/** The server computes the new position from the neighbours (null = top or bottom). */
export function moveCard(
  cardId: string,
  target: { columnId: string; previousCardId: string | null; nextCardId: string | null },
) {
  return apiFetch<Card>(`/cards/${cardId}/move`, { method: 'PATCH', body: target })
}
