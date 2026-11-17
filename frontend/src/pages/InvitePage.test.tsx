import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route, Routes } from 'react-router'
import { beforeEach, expect, test, vi } from 'vitest'
import * as api from '../features/workspaces/api'
import { ApiError } from '../lib/apiClient'
import { renderWithProviders } from '../test/renderWithProviders'
import type { User } from '../types/api'
import { InvitePage } from './InvitePage'

vi.mock('../features/workspaces/api')

const USER: User = {
  id: 'u-1',
  email: 'ada@example.com',
  displayName: 'Ada',
  createdAt: '2026-01-01T00:00:00Z',
}

function renderInvite(logout = vi.fn()) {
  return renderWithProviders(
    <Routes>
      <Route path="/invite/:token" element={<InvitePage />} />
      <Route path="/workspaces/:workspaceId" element={<p>Workspace page</p>} />
    </Routes>,
    { route: '/invite/secret-token', auth: { status: 'authenticated', user: USER, logout } },
  )
}

beforeEach(() => {
  vi.resetAllMocks()
})

test('shows the invitation and joins the workspace on accept', async () => {
  const user = userEvent.setup()
  vi.mocked(api.previewInvitation).mockResolvedValue({
    id: 'inv-1',
    workspaceId: 'ws-1',
    workspaceName: 'Engineering',
    role: 'MEMBER',
    invitedByName: 'Grace',
    expiresAt: '2026-12-01T00:00:00Z',
  })
  vi.mocked(api.acceptInvitationByToken).mockResolvedValue({
    id: 'ws-1',
    name: 'Engineering',
    role: 'MEMBER',
    createdAt: '2026-01-01T00:00:00Z',
  })
  renderInvite()

  expect(await screen.findByText(/invited you to join/)).toHaveTextContent(
    'Grace invited you to join Engineering as Member.',
  )
  expect(api.previewInvitation).toHaveBeenCalledWith('secret-token')

  await user.click(screen.getByRole('button', { name: 'Accept invitation' }))

  expect(await screen.findByText('Workspace page')).toBeInTheDocument()
  expect(api.acceptInvitationByToken).toHaveBeenCalledWith('secret-token')
})

test('explains when the invitation belongs to another account and offers to switch', async () => {
  const user = userEvent.setup()
  const logout = vi.fn()
  vi.mocked(api.previewInvitation).mockRejectedValue(
    new ApiError({ status: 403, detail: 'This invitation was sent to a different e-mail address' }),
  )
  renderInvite(logout)

  expect(await screen.findByRole('alert')).toHaveTextContent(
    'This invitation was sent to a different email address. You are signed in as ada@example.com.',
  )
  expect(screen.queryByRole('button', { name: 'Accept invitation' })).not.toBeInTheDocument()

  await user.click(screen.getByRole('button', { name: 'Sign in with another account' }))
  expect(logout).toHaveBeenCalledTimes(1)
})

test('explains when the invitation is no longer valid', async () => {
  vi.mocked(api.previewInvitation).mockRejectedValue(
    new ApiError({ status: 404, detail: 'Invitation not found or no longer valid' }),
  )
  renderInvite()

  expect(await screen.findByRole('alert')).toHaveTextContent(
    'This invitation is not valid any more. Ask for a new one.',
  )
})
