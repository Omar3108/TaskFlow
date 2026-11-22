import { Link, Outlet, useLocation, useMatch, useNavigate } from 'react-router'
import { Menu } from '../components/ui/Menu'
import { ErrorBoundary } from '../app/ErrorBoundary'
import { useAuth } from '../features/auth/AuthContext'

/** Shell shared by every signed-in page: top bar plus the current page below it. */
export function AppLayout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  // A kanban board needs every pixel of width; the other pages read better in a column.
  const wide = useMatch('/boards/:boardId') !== null
  const width = wide ? 'max-w-none' : 'max-w-5xl'

  return (
    <div className="min-h-screen bg-slate-50">
      {/* First focusable element: keyboard users can jump straight past the header. */}
      <a
        href="#main"
        className="sr-only z-50 rounded-md bg-white px-3 py-2 text-sm font-medium text-indigo-700 shadow focus:not-sr-only focus:fixed focus:top-2 focus:left-2"
      >
        Skip to content
      </a>
      <header className="border-b border-slate-200 bg-white">
        <div className={`mx-auto flex ${width} items-center justify-between px-4 py-3`}>
          <Link
            to="/"
            className="rounded-sm text-lg font-bold tracking-tight text-indigo-600 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-600"
          >
            TaskFlow
          </Link>
          {user && (
            <Menu
              label="Account menu"
              trigger={
                <span className="flex items-center gap-2 px-1 py-1">
                  <span className="hidden text-sm text-slate-700 sm:inline">
                    {user.displayName}
                  </span>
                  <span
                    aria-hidden="true"
                    className="flex size-8 items-center justify-center rounded-full bg-indigo-100 text-sm font-semibold text-indigo-700"
                  >
                    {user.displayName.trim().charAt(0).toUpperCase()}
                  </span>
                </span>
              }
              items={[
                { label: 'Account', onSelect: () => navigate('/account') },
                { label: 'Sign out', onSelect: () => void logout() },
              ]}
            />
          )}
        </div>
      </header>

      <main id="main" tabIndex={-1} className={`mx-auto ${width} px-4 py-8 outline-none`}>
        <ErrorBoundary key={location.pathname}>
          <Outlet />
        </ErrorBoundary>
      </main>
    </div>
  )
}
