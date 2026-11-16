import type { WorkspaceRole } from '../../types/api'

/**
 * Client-side mirror of the server's authorization rules. It only decides which buttons to
 * show: the API enforces the same rules again on every request, so a tampered client gains
 * nothing.
 */

const RANK: Record<WorkspaceRole, number> = { MEMBER: 1, ADMIN: 2, OWNER: 3 }

export const ROLE_LABELS: Record<WorkspaceRole, string> = {
  OWNER: 'Owner',
  ADMIN: 'Admin',
  MEMBER: 'Member',
}

export function isAtLeast(role: WorkspaceRole, minimum: WorkspaceRole) {
  return RANK[role] >= RANK[minimum]
}

/** Invite people, see and revoke pending invitations. */
export function canManageInvitations(role: WorkspaceRole) {
  return isAtLeast(role, 'ADMIN')
}

/** Roles the actor may grant through an invitation. Only the owner hands out ADMIN. */
export function invitableRoles(role: WorkspaceRole): WorkspaceRole[] {
  if (role === 'OWNER') return ['MEMBER', 'ADMIN']
  if (role === 'ADMIN') return ['MEMBER']
  return []
}

/** Only the owner changes roles, and never the owner's own. */
export function canChangeRoleOf(actorRole: WorkspaceRole, targetRole: WorkspaceRole) {
  return actorRole === 'OWNER' && targetRole !== 'OWNER'
}

/** Removing someone else requires strictly outranking them. */
export function canRemove(actorRole: WorkspaceRole, targetRole: WorkspaceRole) {
  return isAtLeast(actorRole, 'ADMIN') && RANK[actorRole] > RANK[targetRole]
}

/** Everyone can leave except the owner, who has to delete the workspace instead. */
export function canLeave(role: WorkspaceRole) {
  return role !== 'OWNER'
}

/** Rename or delete the workspace. */
export function canEditWorkspace(role: WorkspaceRole) {
  return role === 'OWNER'
}
