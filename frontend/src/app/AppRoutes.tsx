import { Navigate, Route, Routes } from 'react-router'
import { ProtectedRoute, PublicOnlyRoute } from '../features/auth/components/RouteGuards'
import { AppLayout } from '../pages/AppLayout'
import { HomePage } from '../pages/HomePage'
import { InvitePage } from '../pages/InvitePage'
import { LoginPage } from '../pages/LoginPage'
import { RegisterPage } from '../pages/RegisterPage'
import { WorkspacePage } from '../pages/WorkspacePage'

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
          <Route path="/invite/:token" element={<InvitePage />} />
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
