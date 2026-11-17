import { Link, useNavigate, useParams } from 'react-router'
import { Alert } from '../components/ui/Alert'
import { Button } from '../components/ui/Button'
import { useAuth } from '../features/auth/AuthContext'
import { useAcceptInvitationByToken, useInvitationPreview } from '../features/workspaces/hooks'
import { ROLE_LABELS } from '../features/workspaces/permissions'
import { ApiError } from '../lib/apiClient'
import { errorMessage } from '../lib/errors'

/** Target of an invitation link. Reached only when signed in (the route is protected). */
export function InvitePage() {
  const { token = '' } = useParams()
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const preview = useInvitationPreview(token)
  const accept = useAcceptInvitationByToken()

  let content
  if (preview.isPending) {
    content = (
      <p role="status" className="text-sm text-slate-500">
        Loading invitation…
      </p>
    )
  } else if (preview.isError) {
    const status = preview.error instanceof ApiError ? preview.error.status : null
    if (status === 403) {
      content = (
        <>
          <Alert>
            This invitation was sent to a different email address. You are signed in as{' '}
            {user?.email}.
          </Alert>
          <Button variant="secondary" onClick={() => void logout()}>
            Sign in with another account
          </Button>
        </>
      )
    } else if (status === 404) {
      content = <Alert>This invitation is not valid any more. Ask for a new one.</Alert>
    } else {
      content = <Alert>{errorMessage(preview.error)}</Alert>
    }
  } else {
    const invitation = preview.data
    content = (
      <>
        <p className="text-slate-700">
          <span className="font-medium">{invitation.invitedByName}</span> invited you to join{' '}
          <span className="font-medium">{invitation.workspaceName}</span> as{' '}
          {ROLE_LABELS[invitation.role]}.
        </p>
        {accept.isError && <Alert>{errorMessage(accept.error)}</Alert>}
        <Button
          loading={accept.isPending}
          onClick={() =>
            accept.mutate(token, {
              onSuccess: (workspace) => navigate(`/workspaces/${workspace.id}`, { replace: true }),
            })
          }
        >
          Accept invitation
        </Button>
      </>
    )
  }

  return (
    <div className="mx-auto max-w-md rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
      <h1 className="mb-4 text-lg font-semibold text-slate-900">Workspace invitation</h1>
      <div className="flex flex-col items-start gap-4">
        {content}
        <Link to="/" className="text-sm font-medium text-indigo-600 hover:underline">
          Go to your workspaces
        </Link>
      </div>
    </div>
  )
}
