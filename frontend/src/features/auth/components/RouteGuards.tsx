import { Navigate, Outlet, useLocation } from 'react-router'
import { useAuth } from '../AuthContext'
import { useRedirectTarget, type RedirectState } from '../redirect'

function SessionLoading() {
  return (
    <div role="status" className="flex min-h-screen items-center justify-center text-slate-500">
      Loading…
    </div>
  )
}

/** Wraps routes that need a session. Anonymous visitors are sent to /login. */
export function ProtectedRoute() {
  const { status } = useAuth()
  const location = useLocation()

  // Wait for the session check: redirecting now would flash the login page on every reload.
  if (status === 'loading') return <SessionLoading />
  if (status === 'unauthenticated') {
    // Remember where the user was going (an invitation link, a shared card) to return there.
    // The query string matters too: it holds the open card (?card=) and the board filters.
    const state: RedirectState = { from: location.pathname + location.search }
    return <Navigate to="/login" replace state={state} />
  }
  return <Outlet />
}

/** Wraps /login and /register: a signed-in user has nothing to do there. */
export function PublicOnlyRoute() {
  const { status } = useAuth()
  const target = useRedirectTarget()

  if (status === 'loading') return <SessionLoading />
  // Reached right after a successful login or registration: continue to the requested page.
  if (status === 'authenticated') return <Navigate to={target} replace />
  return <Outlet />
}
