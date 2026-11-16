import { useQuery } from '@tanstack/react-query'
import { Alert } from '../components/ui/Alert'
import { Button } from '../components/ui/Button'
import { fetchCurrentUser } from '../features/auth/api'
import { useAuth } from '../features/auth/AuthContext'
import { queryKeys } from '../lib/queryKeys'

/** Temporary landing page for signed-in users. Workspaces replace it in the next phase. */
export function HomePage() {
  const { user, logout } = useAuth()

  // Read the profile through the protected API rather than from the login response: this
  // exercises the Bearer token and the automatic refresh on every visit.
  const profile = useQuery({ queryKey: queryKeys.currentUser, queryFn: fetchCurrentUser })

  return (
    <div className="min-h-screen bg-slate-50">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-4xl items-center justify-between px-4 py-3">
          <span className="text-lg font-bold tracking-tight text-indigo-600">TaskFlow</span>
          <div className="flex items-center gap-3">
            <span className="hidden text-sm text-slate-600 sm:inline">{user?.email}</span>
            <Button variant="secondary" onClick={() => void logout()}>
              Sign out
            </Button>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-4xl px-4 py-8">
        <h1 className="text-2xl font-semibold text-slate-900">Welcome, {user?.displayName}</h1>
        <p className="mt-1 text-slate-600">Your workspaces will appear here.</p>

        <section className="mt-6 rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
          <h2 className="text-sm font-semibold tracking-wide text-slate-500 uppercase">Profile</h2>
          {profile.isPending && <p className="mt-3 text-sm text-slate-500">Loading profile…</p>}
          {profile.isError && (
            <div className="mt-3">
              <Alert>Could not load your profile.</Alert>
            </div>
          )}
          {profile.data && (
            <dl className="mt-3 grid grid-cols-1 gap-3 text-sm sm:grid-cols-3">
              <div>
                <dt className="text-slate-500">Name</dt>
                <dd className="font-medium text-slate-900">{profile.data.displayName}</dd>
              </div>
              <div>
                <dt className="text-slate-500">Email</dt>
                <dd className="font-medium text-slate-900">{profile.data.email}</dd>
              </div>
              <div>
                <dt className="text-slate-500">Member since</dt>
                <dd className="font-medium text-slate-900">
                  {new Date(profile.data.createdAt).toLocaleDateString()}
                </dd>
              </div>
            </dl>
          )}
        </section>
      </main>
    </div>
  )
}
