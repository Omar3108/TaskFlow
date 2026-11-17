import { expect, test } from 'vitest'
import { canManageBoards } from './permissions'

test('only admins and the owner manage boards and columns', () => {
  expect(canManageBoards('OWNER')).toBe(true)
  expect(canManageBoards('ADMIN')).toBe(true)
  expect(canManageBoards('MEMBER')).toBe(false)
})
