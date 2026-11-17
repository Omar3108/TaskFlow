import type { WorkspaceRole } from '../../../types/api'
import { ROLE_LABELS } from '../permissions'

const STYLES: Record<WorkspaceRole, string> = {
  OWNER: 'bg-amber-100 text-amber-800',
  ADMIN: 'bg-indigo-100 text-indigo-800',
  MEMBER: 'bg-slate-100 text-slate-700',
}

export function RoleBadge({ role }: { role: WorkspaceRole }) {
  return (
    <span className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${STYLES[role]}`}>
      {ROLE_LABELS[role]}
    </span>
  )
}
