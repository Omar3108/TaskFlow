import { apiFetch } from '../../lib/apiClient'
import type { Board, BoardDetail, ColumnSummary } from '../../types/api'

// ---------- boards ----------

export function listBoards(workspaceId: string) {
  return apiFetch<Board[]>(`/workspaces/${workspaceId}/boards`)
}

export interface CreateBoardInput {
  name: string
  description: string | null
  /** Start with "To do", "In progress" and "Done". */
  defaultColumns: boolean
}

export function createBoard(workspaceId: string, input: CreateBoardInput) {
  return apiFetch<Board>(`/workspaces/${workspaceId}/boards`, { method: 'POST', body: input })
}

export function getBoard(boardId: string) {
  return apiFetch<BoardDetail>(`/boards/${boardId}`)
}

/** Replaces name and description: a null description clears it. */
export function updateBoard(boardId: string, input: { name: string; description: string | null }) {
  return apiFetch<Board>(`/boards/${boardId}`, { method: 'PATCH', body: input })
}

export function deleteBoard(boardId: string) {
  return apiFetch<void>(`/boards/${boardId}`, { method: 'DELETE' })
}

// ---------- columns ----------

export function createColumn(boardId: string, name: string) {
  return apiFetch<ColumnSummary>(`/boards/${boardId}/columns`, { method: 'POST', body: { name } })
}

export function renameColumn(columnId: string, name: string) {
  return apiFetch<ColumnSummary>(`/columns/${columnId}`, { method: 'PATCH', body: { name } })
}

export function deleteColumn(columnId: string) {
  return apiFetch<void>(`/columns/${columnId}`, { method: 'DELETE' })
}

/** The server computes the new position from the neighbours (null = start or end). */
export function moveColumn(
  columnId: string,
  neighbours: { previousColumnId: string | null; nextColumnId: string | null },
) {
  return apiFetch<ColumnSummary>(`/columns/${columnId}/move`, { method: 'PATCH', body: neighbours })
}
