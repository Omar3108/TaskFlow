import { describe, expect, test } from 'vitest'
import type { WorkspaceRole } from '../../types/api'
import {
  canChangeRoleOf,
  canEditWorkspace,
  canLeave,
  canManageInvitations,
  canRemove,
  invitableRoles,
} from './permissions'

const ROLES: WorkspaceRole[] = ['OWNER', 'ADMIN', 'MEMBER']

describe('workspace permissions', () => {
  test('only admins and the owner manage invitations', () => {
    expect(ROLES.filter(canManageInvitations)).toEqual(['OWNER', 'ADMIN'])
  })

  test('only the owner can grant the admin role through an invitation', () => {
    expect(invitableRoles('OWNER')).toEqual(['MEMBER', 'ADMIN'])
    expect(invitableRoles('ADMIN')).toEqual(['MEMBER'])
    expect(invitableRoles('MEMBER')).toEqual([])
  })

  test('only the owner changes roles, and never the role of an owner', () => {
    expect(canChangeRoleOf('OWNER', 'ADMIN')).toBe(true)
    expect(canChangeRoleOf('OWNER', 'MEMBER')).toBe(true)
    expect(canChangeRoleOf('OWNER', 'OWNER')).toBe(false)
    expect(canChangeRoleOf('ADMIN', 'MEMBER')).toBe(false)
    expect(canChangeRoleOf('MEMBER', 'MEMBER')).toBe(false)
  })

  test.each<[WorkspaceRole, WorkspaceRole, boolean]>([
    ['OWNER', 'ADMIN', true],
    ['OWNER', 'MEMBER', true],
    ['ADMIN', 'MEMBER', true],
    ['ADMIN', 'ADMIN', false],
    ['ADMIN', 'OWNER', false],
    ['MEMBER', 'MEMBER', false],
    ['MEMBER', 'ADMIN', false],
  ])('%s removing %s -> %s', (actor, target, expected) => {
    expect(canRemove(actor, target)).toBe(expected)
  })

  test('everyone but the owner can leave; only the owner edits the workspace', () => {
    expect(ROLES.filter(canLeave)).toEqual(['ADMIN', 'MEMBER'])
    expect(ROLES.filter(canEditWorkspace)).toEqual(['OWNER'])
  })
})
