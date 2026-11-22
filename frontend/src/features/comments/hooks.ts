import { useInfiniteQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { queryKeys } from '../../lib/queryKeys'
import { invalidateBoard } from '../boards/hooks'
import * as api from './api'

export const COMMENTS_PAGE_SIZE = 10

/**
 * Comments, newest first, one page at a time ("Show older comments"). After any change the
 * query is invalidated, which refetches every loaded page from the first one: with offset
 * pagination, a new comment shifts all pages by one, so stale pages would repeat or skip items.
 */
export function useComments(cardId: string) {
  return useInfiniteQuery({
    queryKey: queryKeys.cards.comments(cardId),
    queryFn: ({ pageParam }) => api.listComments(cardId, pageParam, COMMENTS_PAGE_SIZE),
    initialPageParam: 0,
    getNextPageParam: (last) => (last.page + 1 < last.totalPages ? last.page + 1 : undefined),
  })
}

/** Posting or deleting changes the comment count shown on the board. */
function useInvalidateComments(cardId: string, boardId: string) {
  const queryClient = useQueryClient()
  return () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: queryKeys.cards.comments(cardId) }),
      invalidateBoard(queryClient, boardId),
    ])
}

export function useCreateComment(cardId: string, boardId: string) {
  const invalidate = useInvalidateComments(cardId, boardId)
  return useMutation({
    mutationFn: (body: string) => api.createComment(cardId, body),
    onSuccess: invalidate,
  })
}

export function useUpdateComment(cardId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ commentId, body }: { commentId: string; body: string }) =>
      api.updateComment(commentId, body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.cards.comments(cardId) }),
  })
}

export function useDeleteComment(cardId: string, boardId: string) {
  const invalidate = useInvalidateComments(cardId, boardId)
  return useMutation({
    mutationFn: (commentId: string) => api.deleteComment(commentId),
    onSuccess: invalidate,
  })
}
