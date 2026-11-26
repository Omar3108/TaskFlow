import { useSyncExternalStore } from 'react'
import { getSlowRequestCount, subscribeToSlowRequests } from '../lib/slowRequests'

/** Explains a slow first response instead of letting the app look frozen. */
export function SlowServerNotice() {
  // useSyncExternalStore: the React way to read a value that lives outside React.
  const slow = useSyncExternalStore(subscribeToSlowRequests, getSlowRequestCount) > 0
  if (!slow) return null
  return (
    <div role="status" className="fixed inset-x-0 top-0 z-50 flex justify-center px-4 pt-3">
      <p className="flex items-center gap-2 rounded-full bg-slate-900 px-4 py-2 text-sm text-white shadow-lg">
        <span aria-hidden="true" className="size-2 animate-pulse rounded-full bg-amber-400" />
        Waking up the server… The free hosting sleeps when idle; this can take up to a minute.
      </p>
    </div>
  )
}
