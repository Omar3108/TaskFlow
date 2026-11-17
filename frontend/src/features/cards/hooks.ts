import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { queryKeys } from '../../lib/queryKeys'
import { applyCardMove, type CardMove } from '../boards/dnd'
import { invalidateBoard, optimisticBoardUpdate } from '../boards/hooks'
import * as api from './api'

export function useCard(cardId: string) {
  return useQuery({
    queryKey: queryKeys.cards.detail(cardId),
    queryFn: () => api.getCard(cardId),
    // The card is shown in an edit form: a background refetch with a newer version would reset
    // the form and throw away what the user is typing. Conflicts are handled on save (409).
    refetchOnWindowFocus: false,
  })
}

export function useCreateCard(boardId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ columnId, title }: { columnId: string; title: string }) =>
      api.createCard(columnId, title),
    onSuccess: () => invalidateBoard(queryClient, boardId),
  })
}

export function useUpdateCard(boardId: string, cardId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: api.UpdateCardInput) => api.updateCard(cardId, input),
    onSuccess: (card) => {
      // The response is the new state of the card: store it instead of refetching it.
      queryClient.setQueryData(queryKeys.cards.detail(cardId), card)
      return invalidateBoard(queryClient, boardId)
    },
  })
}

export function useDeleteCard(boardId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (cardId: string) => api.deleteCard(cardId),
    onSuccess: (_result, cardId) => {
      queryClient.removeQueries({ queryKey: queryKeys.cards.detail(cardId) })
      return invalidateBoard(queryClient, boardId)
    },
  })
}

export function useMoveCard(boardId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ cardId, ...target }: CardMove) => api.moveCard(cardId, target),
    ...optimisticBoardUpdate(queryClient, boardId, applyCardMove),
  })
}
