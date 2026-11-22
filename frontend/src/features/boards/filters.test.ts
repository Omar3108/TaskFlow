import { describe, expect, test } from 'vitest'
import { hasFilters, readFilters, toApiQuery, writeFilters } from './filters'

describe('readFilters', () => {
  test('reads every filter from the URL', () => {
    expect(readFilters(new URLSearchParams('q=login&assignee=u-1&label=l-1&due=week'))).toEqual({
      q: 'login',
      assignee: 'u-1',
      label: 'l-1',
      due: 'week',
    })
  })

  test('ignores blank and unknown values', () => {
    expect(readFilters(new URLSearchParams('q=%20%20&due=tomorrow&label='))).toEqual({})
  })
})

test('writeFilters leaves empty filters out of the URL', () => {
  expect(writeFilters({ q: '  bug ', assignee: undefined, due: 'overdue' }).toString()).toBe(
    'q=bug&due=overdue',
  )
  expect(writeFilters({}).toString()).toBe('')
})

test('hasFilters ignores a search made only of spaces', () => {
  expect(hasFilters({})).toBe(false)
  expect(hasFilters({ q: '   ' })).toBe(false)
  expect(hasFilters({ label: 'l-1' })).toBe(true)
})

describe('toApiQuery', () => {
  test('translates to the parameter names of the API', () => {
    expect(toApiQuery({ q: 'login', assignee: 'u-1', label: 'l-1', due: 'week' })).toBe(
      '?q=login&assigneeId=u-1&labelId=l-1&due=WEEK',
    )
  })

  test('"none" as assignee means unassigned cards', () => {
    expect(toApiQuery({ assignee: 'none' })).toBe('?unassigned=true')
  })

  test('no filters, no query string', () => {
    expect(toApiQuery({})).toBe('')
  })

  test('encodes the search text', () => {
    expect(toApiQuery({ q: '100% & more' })).toBe('?q=100%25+%26+more')
  })
})
