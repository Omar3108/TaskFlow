import type { BoardDetail } from '../../types/api'

/**
 * Pure functions behind drag and drop. Components only translate dnd-kit events into calls to
 * these, so the part that can go wrong (where does the item end up, who are its neighbours) is
 * tested without simulating a drag.
 *
 * Every function returns a new board and never mutates its input: the result is written to the
 * TanStack Query cache, which relies on new references to detect changes.
 */

export interface CardMove {
  cardId: string
  /** Target column (the current one when reordering inside a column). */
  columnId: string
  previousCardId: string | null
  nextCardId: string | null
}

export interface ColumnMove {
  columnId: string
  previousColumnId: string | null
  nextColumnId: string | null
}

/** Where a card currently is, or null if it is not on this board. */
export function locateCard(board: BoardDetail, cardId: string) {
  for (const column of board.columns) {
    const index = column.cards.findIndex((card) => card.id === cardId)
    if (index !== -1) return { columnId: column.id, index }
  }
  return null
}

/**
 * Moves a card so that it ends up at `toIndex` in the target column (the same meaning as the
 * target index of an array move). Out-of-range indexes are clamped to the ends.
 */
export function moveCardTo(
  board: BoardDetail,
  cardId: string,
  toColumnId: string,
  toIndex: number,
): BoardDetail {
  const from = locateCard(board, cardId)
  if (!from || !board.columns.some((column) => column.id === toColumnId)) return board
  const card = board.columns.find((column) => column.id === from.columnId)!.cards[from.index]

  return {
    ...board,
    columns: board.columns.map((column) => {
      const cards = column.cards.filter((c) => c.id !== cardId)
      if (column.id !== toColumnId) {
        return column.id === from.columnId ? { ...column, cards } : column
      }
      const index = Math.max(0, Math.min(toIndex, cards.length))
      cards.splice(index, 0, { ...card, columnId: toColumnId })
      return { ...column, cards }
    }),
  }
}

/** The request the server needs to reproduce the card's current place in `board`. */
export function cardMoveFor(board: BoardDetail, cardId: string): CardMove | null {
  const location = locateCard(board, cardId)
  if (!location) return null
  const cards = board.columns.find((column) => column.id === location.columnId)!.cards
  return {
    cardId,
    columnId: location.columnId,
    previousCardId: cards[location.index - 1]?.id ?? null,
    nextCardId: cards[location.index + 1]?.id ?? null,
  }
}

/** Applies a move expressed as neighbours, as the server will (used for optimistic updates). */
export function applyCardMove(board: BoardDetail, move: CardMove): BoardDetail {
  const target = board.columns.find((column) => column.id === move.columnId)
  if (!target) return board
  const others = target.cards.filter((card) => card.id !== move.cardId)
  let index = others.length
  if (move.previousCardId) {
    index = others.findIndex((card) => card.id === move.previousCardId) + 1
  } else if (move.nextCardId) {
    index = Math.max(
      0,
      others.findIndex((card) => card.id === move.nextCardId),
    )
  }
  return moveCardTo(board, move.cardId, move.columnId, index)
}

export function moveColumnTo(board: BoardDetail, columnId: string, toIndex: number): BoardDetail {
  const from = board.columns.findIndex((column) => column.id === columnId)
  if (from === -1) return board
  const columns = board.columns.filter((column) => column.id !== columnId)
  columns.splice(Math.max(0, Math.min(toIndex, columns.length)), 0, board.columns[from])
  return { ...board, columns }
}

export function columnMoveFor(board: BoardDetail, columnId: string): ColumnMove | null {
  const index = board.columns.findIndex((column) => column.id === columnId)
  if (index === -1) return null
  return {
    columnId,
    previousColumnId: board.columns[index - 1]?.id ?? null,
    nextColumnId: board.columns[index + 1]?.id ?? null,
  }
}

export function applyColumnMove(board: BoardDetail, move: ColumnMove): BoardDetail {
  const others = board.columns.filter((column) => column.id !== move.columnId)
  let index = others.length
  if (move.previousColumnId) {
    index = others.findIndex((column) => column.id === move.previousColumnId) + 1
  } else if (move.nextColumnId) {
    index = Math.max(
      0,
      others.findIndex((column) => column.id === move.nextColumnId),
    )
  }
  return moveColumnTo(board, move.columnId, index)
}
