import { describe, expect, test } from 'vitest'
import { sampleBoard } from '../../test/fixtures'
import type { BoardDetail } from '../../types/api'
import {
  applyCardMove,
  applyColumnMove,
  cardMoveFor,
  columnMoveFor,
  locateCard,
  moveCardTo,
  moveColumnTo,
} from './dnd'

const titles = (board: BoardDetail, columnId: string) =>
  board.columns.find((column) => column.id === columnId)!.cards.map((card) => card.id)

describe('moveCardTo', () => {
  test('reorders inside a column', () => {
    const board = moveCardTo(sampleBoard(), 'a', 'todo', 2)

    expect(titles(board, 'todo')).toEqual(['b', 'c', 'a'])
  })

  test('moves to another column and updates the card column', () => {
    const board = moveCardTo(sampleBoard(), 'b', 'done', 0)

    expect(titles(board, 'todo')).toEqual(['a', 'c'])
    expect(titles(board, 'done')).toEqual(['b', 'x'])
    expect(board.columns[1].cards[0].columnId).toBe('done')
  })

  test('moves into an empty column', () => {
    const board = moveCardTo(sampleBoard(), 'c', 'empty', 0)

    expect(titles(board, 'empty')).toEqual(['c'])
  })

  test('clamps an index past the end to the bottom', () => {
    expect(titles(moveCardTo(sampleBoard(), 'a', 'done', 99), 'done')).toEqual(['x', 'a'])
    expect(titles(moveCardTo(sampleBoard(), 'c', 'todo', -5), 'todo')).toEqual(['c', 'a', 'b'])
  })

  test('never mutates the board it receives', () => {
    const original = sampleBoard()
    const snapshot = structuredClone(original)

    moveCardTo(original, 'a', 'done', 0)

    expect(original).toEqual(snapshot)
  })

  test('ignores unknown cards and columns', () => {
    const board = sampleBoard()

    expect(moveCardTo(board, 'nope', 'todo', 0)).toBe(board)
    expect(moveCardTo(board, 'a', 'nope', 0)).toBe(board)
  })
})

describe('cardMoveFor', () => {
  test('describes a card by its neighbours', () => {
    const board = sampleBoard()

    expect(cardMoveFor(board, 'b')).toEqual({
      cardId: 'b',
      columnId: 'todo',
      previousCardId: 'a',
      nextCardId: 'c',
    })
    expect(cardMoveFor(board, 'a')).toMatchObject({ previousCardId: null, nextCardId: 'b' })
    expect(cardMoveFor(board, 'c')).toMatchObject({ previousCardId: 'b', nextCardId: null })
    expect(cardMoveFor(board, 'x')).toMatchObject({ previousCardId: null, nextCardId: null })
    expect(cardMoveFor(board, 'nope')).toBeNull()
  })
})

describe('applyCardMove', () => {
  // Round trip: arrange the board by index, describe the result by neighbours (what is sent to
  // the server), and apply that description to the original board. Both must agree for every
  // card and every possible target.
  test('rebuilds every arrangement from its neighbours', () => {
    const board = sampleBoard()
    for (const cardId of ['a', 'b', 'c', 'x']) {
      for (const column of board.columns) {
        for (let index = 0; index <= column.cards.length; index++) {
          const expected = moveCardTo(board, cardId, column.id, index)
          const move = cardMoveFor(expected, cardId)!

          expect(applyCardMove(board, move), `${cardId} -> ${column.id}[${index}]`).toEqual(
            expected,
          )
        }
      }
    }
  })

  test('leaves the board alone when the target column does not exist', () => {
    const board = sampleBoard()

    expect(
      applyCardMove(board, {
        cardId: 'a',
        columnId: 'nope',
        previousCardId: null,
        nextCardId: null,
      }),
    ).toBe(board)
  })
})

describe('columns', () => {
  const order = (board: BoardDetail) => board.columns.map((column) => column.id)

  test('moveColumnTo reorders columns', () => {
    expect(order(moveColumnTo(sampleBoard(), 'empty', 0))).toEqual(['empty', 'todo', 'done'])
    expect(order(moveColumnTo(sampleBoard(), 'todo', 2))).toEqual(['done', 'empty', 'todo'])
  })

  test('columnMoveFor describes a column by its neighbours', () => {
    expect(columnMoveFor(sampleBoard(), 'done')).toEqual({
      columnId: 'done',
      previousColumnId: 'todo',
      nextColumnId: 'empty',
    })
    expect(columnMoveFor(sampleBoard(), 'nope')).toBeNull()
  })

  test('applyColumnMove rebuilds every arrangement from its neighbours', () => {
    const board = sampleBoard()
    for (const column of board.columns) {
      for (let index = 0; index < board.columns.length; index++) {
        const expected = moveColumnTo(board, column.id, index)

        expect(applyColumnMove(board, columnMoveFor(expected, column.id)!)).toEqual(expected)
      }
    }
  })

  test('moving a column keeps its cards', () => {
    const board = moveColumnTo(sampleBoard(), 'todo', 2)

    expect(locateCard(board, 'a')).toEqual({ columnId: 'todo', index: 0 })
  })
})
