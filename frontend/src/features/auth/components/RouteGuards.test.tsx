import { screen } from '@testing-library/react'
import { Route, Routes } from 'react-router'
import { expect, test } from 'vitest'
import { renderWithProviders } from '../../../test/renderWithProviders'
import type { AuthStatus } from '../AuthContext'
import { useRedirectTarget } from '../redirect'
import { ProtectedRoute, PublicOnlyRoute } from './RouteGuards'

function renderRoutes(status: AuthStatus, route: string) {
  return renderWithProviders(
    <Routes>
      <Route element={<PublicOnlyRoute />}>
        <Route path="/login" element={<p>Login page</p>} />
      </Route>
      <Route element={<ProtectedRoute />}>
        <Route path="/" element={<p>Private page</p>} />
      </Route>
    </Routes>,
    { auth: { status }, route },
  )
}

test('an anonymous visitor is redirected from a protected route to the login page', () => {
  renderRoutes('unauthenticated', '/')

  expect(screen.getByText('Login page')).toBeInTheDocument()
  expect(screen.queryByText('Private page')).not.toBeInTheDocument()
})

test('a signed-in user sees the protected route', () => {
  renderRoutes('authenticated', '/')

  expect(screen.getByText('Private page')).toBeInTheDocument()
})

test('nothing is decided while the session is still being checked', () => {
  renderRoutes('loading', '/')

  expect(screen.getByRole('status')).toHaveTextContent('Loading…')
  expect(screen.queryByText('Private page')).not.toBeInTheDocument()
  expect(screen.queryByText('Login page')).not.toBeInTheDocument()
})

test('a signed-in user is redirected away from the login page', () => {
  renderRoutes('authenticated', '/login')

  expect(screen.getByText('Private page')).toBeInTheDocument()
})

test('the page to return to keeps its query string (a shared card link, filters)', async () => {
  renderWithProviders(
    <Routes>
      <Route element={<ProtectedRoute />}>
        <Route path="/boards/:boardId" element={<p>Board</p>} />
      </Route>
      <Route path="/login" element={<ReturnTarget />} />
    </Routes>,
    { route: '/boards/b-1?card=c-1&label=l-1', auth: { status: 'unauthenticated' } },
  )

  expect(await screen.findByText('/boards/b-1?card=c-1&label=l-1')).toBeInTheDocument()
})

function ReturnTarget() {
  return <p>{useRedirectTarget()}</p>
}
