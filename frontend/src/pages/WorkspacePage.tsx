import type { ReactNode } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { Alert } from '../components/ui/Alert'
import { useAuth } from '../features/auth/AuthContext'
import { InvitationPanel } from '../features/workspaces/components/InvitationPanel'
import { MemberList } from '../features/workspaces/components/MemberList'
import { RoleBadge } from '../features/workspaces/components/RoleBadge'
import { WorkspaceSettings } from '../features/workspaces/components/WorkspaceSettings'
import { useWorkspace } from '../features/workspaces/hooks'
import { canManageInvitations } from '../features/workspaces/permissions'
import { ApiError } from '../lib/apiClient'
import { errorMessage } from '../lib/errors'

function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="mt-8">
      <h2 className="mb-3 text-lg font-semibold text-slate-900">{title}</h2>
      {children}
    </section>
  )
}

export function WorkspacePage() {
  const { workspaceId = '' } = useParams()
  const { user } = useAuth()
  const navigate = useNavigate()
  const workspace = useWorkspace(workspaceId)

  if (workspace.isPending) {
    return (
      <p role="status" className="text-sm text-slate-500">
        Loading workspace…
      </p>
    )
  }

  if (workspace.isError) {
    // The API answers 404 both for unknown workspaces and for ones the user is not part of.
    const notFound = workspace.error instanceof ApiError && workspace.error.status === 404
    return (
      <div className="flex flex-col items-start gap-3">
        <Alert>
          {notFound
            ? 'This workspace does not exist or you do not have access to it.'
            : errorMessage(workspace.error)}
        </Alert>
        <Link to="/" className="text-sm font-medium text-indigo-600 hover:underline">
          Back to your workspaces
        </Link>
      </div>
    )
  }

  const { data } = workspace
  if (!user) return null

  return (
    <>
      <Link to="/" className="text-sm font-medium text-indigo-600 hover:underline">
        ← All workspaces
      </Link>
      <div className="mt-2 flex flex-wrap items-center gap-3">
        <h1 className="text-2xl font-semibold text-slate-900">{data.name}</h1>
        <RoleBadge role={data.role} />
      </div>

      <Section title="Boards">
        <p className="rounded-xl border border-dashed border-slate-300 bg-white p-6 text-sm text-slate-500">
          Boards are coming next.
        </p>
      </Section>

      <Section title="Members">
        <MemberList workspaceId={data.id} currentUserId={user.id} currentRole={data.role} />
      </Section>

      {canManageInvitations(data.role) && (
        <Section title="Invitations">
          <InvitationPanel workspaceId={data.id} currentRole={data.role} />
        </Section>
      )}

      <Section title="Settings">
        {/* key: remount the form when the name changes elsewhere, so it shows the new value */}
        <WorkspaceSettings
          key={data.name}
          workspace={data}
          currentUserId={user.id}
          onGone={() => navigate('/', { replace: true })}
        />
      </Section>
    </>
  )
}
