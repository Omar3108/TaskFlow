import type { WorkspaceRole } from '../../types/api'
import { isAtLeast } from '../workspaces/permissions'

/**
 * Create, edit and delete boards and columns, and reorder columns. Cards are open to every
 * member. Like the workspace permissions, this only decides which controls to show: the API
 * enforces the same rule.
 */
export function canManageBoards(role: WorkspaceRole) {
  return isAtLeast(role, 'ADMIN')
}
