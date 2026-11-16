import { Navigate, Outlet, useLocation } from 'react-router'
import { useAuth } from '../AuthContext'

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
    // Remember where the user was going, to return there after signing in.
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }
  return <Outlet />
}

/** Wraps /login and /register: a signed-in user has nothing to do there. */
export function PublicOnlyRoute() {
  const { status } = useAuth()

  if (status === 'loading') return <SessionLoading />
  if (status === 'authenticated') return <Navigate to="/" replace />
  return <Outlet />
}
