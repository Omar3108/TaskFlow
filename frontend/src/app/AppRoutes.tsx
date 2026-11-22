import { lazy, Suspense } from 'react'
import { Route, Routes } from 'react-router'
import { ProtectedRoute, PublicOnlyRoute } from '../features/auth/components/RouteGuards'
import { BoardSkeleton } from '../features/boards/components/BoardSkeleton'
import { AccountPage } from '../pages/AccountPage'
import { AppLayout } from '../pages/AppLayout'
import { HomePage } from '../pages/HomePage'
import { InvitePage } from '../pages/InvitePage'
import { LoginPage } from '../pages/LoginPage'
import { NotFoundPage } from '../pages/NotFoundPage'
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
              <Suspense fallback={<BoardSkeleton />}>
                <BoardPage />
              </Suspense>
            }
          />
          <Route path="/invite/:token" element={<InvitePage />} />
          <Route path="/account" element={<AccountPage />} />
          {/* Inside the protected layout: signed-out visitors are sent to log in first. */}
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Route>
    </Routes>
  )
}
