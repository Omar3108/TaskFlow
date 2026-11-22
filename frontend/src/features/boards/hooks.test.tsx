import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, renderHook } from '@testing-library/react'
import type { ReactNode } from 'react'
import { beforeEach, expect, test, vi } from 'vitest'
import { queryKeys } from '../../lib/queryKeys'
import { sampleBoard } from '../../test/fixtures'
import type { BoardDetail } from '../../types/api'
import * as cardsApi from '../cards/api'
import { useMoveCard } from '../cards/hooks'

vi.mock('../cards/api')
vi.mock('./api')

/**
 * The optimistic update used by drag and drop, tested through useMoveCard with a real
 * QueryClient: the cache must change before the server answers, and go back if it refuses.
 */

const order = (board: BoardDetail | undefined, columnId: string) =>
  board?.columns.find((column) => column.id === columnId)?.cards.map((card) => card.id)

function setup() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  // Two cached variants of the same board: unfiltered and filtered.
  const plainKey = queryKeys.boards.filtered('board-1', {})
  const filteredKey = queryKeys.boards.filtered('board-1', { q: 'a' })
  queryClient.setQueryData(plainKey, sampleBoard())
  queryClient.setQueryData(filteredKey, sampleBoard())
  // Keep invalidation from refetching (there is no server in this test).
  vi.spyOn(queryClient, 'invalidateQueries').mockResolvedValue()

  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  )
  const { result } = renderHook(() => useMoveCard('board-1'), { wrapper })
  return {
    queryClient,
    result,
    plain: () => queryClient.getQueryData<BoardDetail>(plainKey),
    filtered: () => queryClient.getQueryData<BoardDetail>(filteredKey),
  }
}

const MOVE = { cardId: 'c', columnId: 'todo', previousCardId: null, nextCardId: 'a' }

beforeEach(() => {
  vi.resetAllMocks()
})

test('moves the card in every cached variant before the server answers', async () => {
  let answer: (value: unknown) => void = () => {}
  vi.mocked(cardsApi.moveCard).mockReturnValue(
    new Promise((resolve) => (answer = resolve)) as never,
  )
  const { result, plain, filtered } = setup()

  act(() => result.current.mutate(MOVE))

  await vi.waitFor(() => expect(order(plain(), 'todo')).toEqual(['c', 'a', 'b']))
  expect(order(filtered(), 'todo')).toEqual(['c', 'a', 'b'])
  expect(cardsApi.moveCard).toHaveBeenCalledWith('c', {
    columnId: 'todo',
    previousCardId: null,
    nextCardId: 'a',
  })

  await act(async () => answer({}))
})

test('puts everything back when the server rejects the move', async () => {
  vi.mocked(cardsApi.moveCard).mockRejectedValue(new Error('Conflict'))
  const { result, plain, filtered, queryClient } = setup()

  act(() => result.current.mutate(MOVE))

  await vi.waitFor(() => expect(result.current.isError).toBe(true))
  expect(order(plain(), 'todo')).toEqual(['a', 'b', 'c'])
  expect(order(filtered(), 'todo')).toEqual(['a', 'b', 'c'])
  // Either way it ends by asking for the real state from the server.
  expect(queryClient.invalidateQueries).toHaveBeenCalledWith({
    queryKey: queryKeys.boards.detail('board-1'),
  })
})
