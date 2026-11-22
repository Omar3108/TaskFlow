import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { queryKeys } from '../../lib/queryKeys'
import type { LabelColor } from '../../types/api'
import * as api from './api'

export function useLabels(boardId: string) {
  return useQuery({
    queryKey: queryKeys.boards.labels(boardId),
    queryFn: () => api.listLabels(boardId),
  })
}

/** Labels are shown on the cards too, so every change refreshes the board as well. */
function useInvalidateLabels(boardId: string) {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: queryKeys.boards.root(boardId) })
}

export function useCreateLabel(boardId: string) {
  const invalidate = useInvalidateLabels(boardId)
  return useMutation({
    mutationFn: (input: { name: string; color: LabelColor }) => api.createLabel(boardId, input),
    onSuccess: invalidate,
  })
}

export function useUpdateLabel(boardId: string) {
  const invalidate = useInvalidateLabels(boardId)
  return useMutation({
    mutationFn: ({ labelId, ...input }: { labelId: string; name: string; color: LabelColor }) =>
      api.updateLabel(labelId, input),
    onSuccess: invalidate,
  })
}

export function useDeleteLabel(boardId: string) {
  const invalidate = useInvalidateLabels(boardId)
  return useMutation({
    mutationFn: (labelId: string) => api.deleteLabel(labelId),
    onSuccess: invalidate,
  })
}
