import { lazy, Suspense } from 'react'
import { Navigate, Route, Routes } from 'react-router'
import { ProtectedRoute, PublicOnlyRoute } from '../features/auth/components/RouteGuards'
import { AppLayout } from '../pages/AppLayout'
import { HomePage } from '../pages/HomePage'
import { InvitePage } from '../pages/InvitePage'
import { LoginPage } from '../pages/LoginPage'
import { RegisterPage } from '../pages/RegisterPage'
import { WorkspacePage } from '../pages/WorkspacePage'

// The board screen is the only one that needs dnd-kit: loading it on demand keeps the
// drag-and-drop code out of the bundle that every other page downloads first.
const BoardPage = lazy(() => import('../pages/BoardPage').then((m) => ({ default: m.BoardPage })))

export function AppRoutes() {
  return (
    <Routes>
      <Route element={<PublicOnlyRoute />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
      </Route>

      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route path="/" element={<HomePage />} />
          <Route path="/workspaces/:workspaceId" element={<WorkspacePage />} />
          <Route
            path="/boards/:boardId"
            element={
              <Suspense
                fallback={
                  <p role="status" className="text-sm text-slate-500">
                    Loading board…
                  </p>
                }
              >
                <BoardPage />
              </Suspense>
            }
          />
          <Route path="/invite/:token" element={<InvitePage />} />
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
