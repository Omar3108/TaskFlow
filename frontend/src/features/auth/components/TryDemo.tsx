import { useMutation } from '@tanstack/react-query'
import { Alert } from '../../../components/ui/Alert'
import { Button } from '../../../components/ui/Button'
import { errorMessage } from '../../../lib/errors'
import { useAuth } from '../AuthContext'

/**
 * One click into a private, pre-filled sandbox, for visitors who just want to look around
 * (recruiters, mostly). Once signed in, PublicOnlyRoute takes them to their workspaces.
 */
export function TryDemo() {
  const { startDemo } = useAuth()
  const demo = useMutation({ mutationFn: startDemo })

  return (
    <div className="flex flex-col gap-3 rounded-xl border border-indigo-100 bg-indigo-50 p-4 text-center">
      <p className="text-sm text-slate-700">
        Just looking? Explore a board full of sample data. No sign-up needed.
      </p>
      {demo.isError && <Alert>{errorMessage(demo.error)}</Alert>}
      <Button variant="secondary" loading={demo.isPending} onClick={() => demo.mutate()}>
        {demo.isPending ? 'Preparing your demo…' : 'Try the demo'}
      </Button>
    </div>
  )
}
