import { Alert } from '../../../components/ui/Alert'
import { Button } from '../../../components/ui/Button'
import { errorMessage } from '../../../lib/errors'
import { useAcceptInvitation, useDeclineInvitation, useReceivedInvitations } from '../hooks'
import { ROLE_LABELS } from '../permissions'

/** Invitations waiting for the signed-in user. Renders nothing when there are none. */
export function ReceivedInvitations() {
  const invitations = useReceivedInvitations()
  const accept = useAcceptInvitation()
  const decline = useDeclineInvitation()

  // A failure to load invitations must not break the page: the workspace list still works.
  if (!invitations.data || invitations.data.length === 0) return null

  const failure = accept.error ?? decline.error
  const busy = accept.isPending || decline.isPending

  return (
    <section aria-labelledby="received-invitations-title" className="mb-8">
      <h2 id="received-invitations-title" className="mb-3 text-lg font-semibold text-slate-900">
        Pending invitations
      </h2>
      {failure && (
        <div className="mb-3">
          <Alert>{errorMessage(failure)}</Alert>
        </div>
      )}
      <ul className="flex flex-col gap-3">
        {invitations.data.map((invitation) => (
          <li
            key={invitation.id}
            className="flex flex-col gap-3 rounded-xl border border-indigo-200 bg-indigo-50 p-4 sm:flex-row sm:items-center sm:justify-between"
          >
            <p className="text-sm text-slate-700">
              <span className="font-medium">{invitation.invitedByName}</span> invited you to join{' '}
              <span className="font-medium">{invitation.workspaceName}</span> as{' '}
              {ROLE_LABELS[invitation.role]}.
            </p>
            <div className="flex shrink-0 gap-2">
              <Button
                variant="secondary"
                size="sm"
                disabled={busy}
                onClick={() => decline.mutate(invitation.id)}
                aria-label={`Decline invitation to ${invitation.workspaceName}`}
              >
                Decline
              </Button>
              <Button
                size="sm"
                disabled={busy}
                onClick={() => accept.mutate(invitation.id)}
                aria-label={`Accept invitation to ${invitation.workspaceName}`}
              >
                Accept
              </Button>
            </div>
          </li>
        ))}
      </ul>
    </section>
  )
}
