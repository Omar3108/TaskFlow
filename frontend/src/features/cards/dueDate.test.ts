import { expect, test } from 'vitest'
import { dueStatus } from './dueDate'

// A fixed "today" (local time) so the test does not depend on the day it runs.
const today = new Date(2026, 10, 22) // 22 Nov 2026

test.each([
  ['2026-11-21', 'overdue'],
  ['2025-12-31', 'overdue'],
  ['2026-11-22', 'soon'],
  ['2026-11-24', 'soon'],
  ['2026-11-25', 'later'],
  ['2027-01-01', 'later'],
])('%s is %s', (date, expected) => {
  expect(dueStatus(date, today)).toBe(expected)
})

test('crosses month and year boundaries', () => {
  expect(dueStatus('2027-01-01', new Date(2026, 11, 30))).toBe('soon')
  expect(dueStatus('2026-12-31', new Date(2027, 0, 1))).toBe('overdue')
})
