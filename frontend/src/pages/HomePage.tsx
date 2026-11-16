import { useState } from 'react'
import { useNavigate } from 'react-router'
import { Button } from '../components/ui/Button'
import { CreateWorkspaceDialog } from '../features/workspaces/components/CreateWorkspaceDialog'
import { ReceivedInvitations } from '../features/workspaces/components/ReceivedInvitations'
import { WorkspaceList } from '../features/workspaces/components/WorkspaceList'

/** Landing page for signed-in users: pending invitations and the list of workspaces. */
export function HomePage() {
  const navigate = useNavigate()
  const [creating, setCreating] = useState(false)

  return (
    <>
      <ReceivedInvitations />

      <div className="mb-4 flex items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold text-slate-900">Your workspaces</h1>
        <Button onClick={() => setCreating(true)}>New workspace</Button>
      </div>

      <WorkspaceList onCreateClick={() => setCreating(true)} />

      <CreateWorkspaceDialog
        open={creating}
        onClose={() => setCreating(false)}
        onCreated={(workspace) => navigate(`/workspaces/${workspace.id}`)}
      />
    </>
  )
}
