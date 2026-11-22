import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
  type QueryClient,
  type QueryKey,
} from '@tanstack/react-query'
import { queryKeys } from '../../lib/queryKeys'
import type { BoardDetail } from '../../types/api'
import * as api from './api'
import { applyColumnMove, type ColumnMove } from './dnd'
import type { BoardFilters } from './filters'

export function useBoards(workspaceId: string) {
  return useQuery({
    queryKey: queryKeys.workspaces.boards(workspaceId),
    queryFn: () => api.listBoards(workspaceId),
  })
}

export function useBoard(boardId: string, filters: BoardFilters = {}) {
  return useQuery({
    // Each filter combination is its own cache entry, under the board's common prefix.
    queryKey: queryKeys.boards.filtered(boardId, filters),
    queryFn: () => api.getBoard(boardId, filters),
    // While a new filter loads, keep showing the previous result instead of a loading screen.
    placeholderData: keepPreviousData,
  })
}

export function useCreateBoard(workspaceId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: api.CreateBoardInput) => api.createBoard(workspaceId, input),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: queryKeys.workspaces.boards(workspaceId) }),
  })
}

export function useUpdateBoard(board: { id: string; workspaceId: string }) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: { name: string; description: string | null }) =>
      api.updateBoard(board.id, input),
    onSuccess: () =>
      Promise.all([
        invalidateBoard(queryClient, board.id),
        queryClient.invalidateQueries({ queryKey: queryKeys.workspaces.boards(board.workspaceId) }),
      ]),
  })
}

export function useDeleteBoard(board: { id: string; workspaceId: string }) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => api.deleteBoard(board.id),
    onSuccess: () => {
      // Remove (not refetch) everything about the deleted board: refetching would only 404.
      queryClient.removeQueries({ queryKey: queryKeys.boards.root(board.id) })
      return queryClient.invalidateQueries({
        queryKey: queryKeys.workspaces.boards(board.workspaceId),
      })
    },
  })
}

/** Every column or card change ends by refetching the board (all its filter variants). */
export function invalidateBoard(queryClient: QueryClient, boardId: string) {
  return queryClient.invalidateQueries({ queryKey: queryKeys.boards.detail(boardId) })
}

/**
 * Optimistic update of the board in the cache: the screen changes at once, and is rolled back
 * if the server rejects the change. Used by the drag-and-drop moves. It updates every cached
 * filter variant of the board, so switching filters right after a move shows no stale order.
 */
export function optimisticBoardUpdate<TVariables>(
  queryClient: QueryClient,
  boardId: string,
  apply: (board: BoardDetail, variables: TVariables) => BoardDetail,
) {
  const queryKey = queryKeys.boards.detail(boardId)
  return {
    onMutate: async (variables: TVariables) => {
      // Cancel refetches in flight first: one that lands later would overwrite the change.
      await queryClient.cancelQueries({ queryKey })
      const previous = queryClient.getQueriesData<BoardDetail>({ queryKey })
      queryClient.setQueriesData<BoardDetail>({ queryKey }, (board) =>
        board ? apply(board, variables) : board,
      )
      return { previous }
    },
    onError: (
      _error: Error,
      _variables: TVariables,
      context?: { previous?: [QueryKey, BoardDetail | undefined][] },
    ) => {
      context?.previous?.forEach(([key, board]) => queryClient.setQueryData(key, board))
    },
    // Success or failure, end with the real state (and real positions) from the server.
    onSettled: () => invalidateBoard(queryClient, boardId),
  }
}

export function useCreateColumn(boardId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (name: string) => api.createColumn(boardId, name),
    onSuccess: () => invalidateBoard(queryClient, boardId),
  })
}

export function useRenameColumn(boardId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ columnId, name }: { columnId: string; name: string }) =>
      api.renameColumn(columnId, name),
    onSuccess: () => invalidateBoard(queryClient, boardId),
  })
}

export function useDeleteColumn(boardId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (columnId: string) => api.deleteColumn(columnId),
    onSuccess: () => invalidateBoard(queryClient, boardId),
  })
}

export function useMoveColumn(boardId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ columnId, ...neighbours }: ColumnMove) => api.moveColumn(columnId, neighbours),
    ...optimisticBoardUpdate(queryClient, boardId, applyColumnMove),
  })
}
