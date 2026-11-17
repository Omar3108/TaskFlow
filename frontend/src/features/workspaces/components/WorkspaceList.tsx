import { Link } from 'react-router'
import { Alert } from '../../../components/ui/Alert'
import { Button } from '../../../components/ui/Button'
import { errorMessage } from '../../../lib/errors'
import { useWorkspaces } from '../hooks'
import { RoleBadge } from './RoleBadge'

export function WorkspaceList({ onCreateClick }: { onCreateClick: () => void }) {
  const workspaces = useWorkspaces()

  if (workspaces.isPending) {
    return (
      <p role="status" className="text-sm text-slate-500">
        Loading workspaces…
      </p>
    )
  }
  if (workspaces.isError) {
    return <Alert>{errorMessage(workspaces.error)}</Alert>
  }
  if (workspaces.data.length === 0) {
    return (
      <div className="rounded-xl border border-dashed border-slate-300 bg-white p-8 text-center">
        <p className="font-medium text-slate-900">You are not in any workspace yet</p>
        <p className="mt-1 text-sm text-slate-600">
          Create one to start organizing boards with your team.
        </p>
        <Button className="mt-4" onClick={onCreateClick}>
          Create your first workspace
        </Button>
      </div>
    )
  }

  return (
    <ul className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
      {workspaces.data.map((workspace) => (
        <li key={workspace.id}>
          <Link
            to={`/workspaces/${workspace.id}`}
            className="flex h-full flex-col gap-3 rounded-xl border border-slate-200 bg-white p-5 shadow-sm transition-shadow hover:shadow-md focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-600"
          >
            <span className="font-semibold text-slate-900">{workspace.name}</span>
            <span>
              <RoleBadge role={workspace.role} />
            </span>
          </Link>
        </li>
      ))}
    </ul>
  )
}
