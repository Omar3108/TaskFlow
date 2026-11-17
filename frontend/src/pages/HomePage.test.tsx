import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route, Routes } from 'react-router'
import { beforeEach, expect, test, vi } from 'vitest'
import * as api from '../features/workspaces/api'
import { ApiError } from '../lib/apiClient'
import { renderWithProviders } from '../test/renderWithProviders'
import type { ReceivedInvitation, Workspace } from '../types/api'
import { HomePage } from './HomePage'

vi.mock('../features/workspaces/api')

const WORKSPACES: Workspace[] = [
  { id: 'ws-1', name: 'Engineering', role: 'OWNER', createdAt: '2026-01-01T00:00:00Z' },
  { id: 'ws-2', name: 'Marketing', role: 'MEMBER', createdAt: '2026-01-02T00:00:00Z' },
]

const INVITATION: ReceivedInvitation = {
  id: 'inv-1',
  workspaceId: 'ws-3',
  workspaceName: 'Design',
  role: 'ADMIN',
  invitedByName: 'Grace',
  expiresAt: '2026-12-01T00:00:00Z',
}

function renderHome() {
  return renderWithProviders(
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/workspaces/:workspaceId" element={<p>Workspace page</p>} />
    </Routes>,
  )
}

beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(api.listWorkspaces).mockResolvedValue(WORKSPACES)
  vi.mocked(api.listReceivedInvitations).mockResolvedValue([])
})

test('lists the workspaces with the role the user has in each', async () => {
  renderHome()

  const engineering = await screen.findByRole('link', { name: /Engineering/ })
  expect(engineering).toHaveAttribute('href', '/workspaces/ws-1')
  expect(within(engineering).getByText('Owner')).toBeInTheDocument()
  expect(
    within(screen.getByRole('link', { name: /Marketing/ })).getByText('Member'),
  ).toBeInTheDocument()
  expect(screen.queryByText('Pending invitations')).not.toBeInTheDocument()
})

test('shows an empty state when the user has no workspace', async () => {
  vi.mocked(api.listWorkspaces).mockResolvedValue([])
  renderHome()

  expect(await screen.findByText('You are not in any workspace yet')).toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'Create your first workspace' })).toBeInTheDocument()
})

test('shows an error when the workspaces cannot be loaded', async () => {
  vi.mocked(api.listWorkspaces).mockRejectedValue(
    new ApiError({ status: 500, detail: 'An unexpected error occurred' }),
  )
  renderHome()

  expect(await screen.findByRole('alert')).toHaveTextContent('An unexpected error occurred')
})

test('creating a workspace validates the name and opens the new workspace', async () => {
  const user = userEvent.setup()
  vi.mocked(api.createWorkspace).mockResolvedValue({
    id: 'ws-9',
    name: 'Research',
    role: 'OWNER',
    createdAt: '2026-01-03T00:00:00Z',
  })
  renderHome()

  await user.click(await screen.findByRole('button', { name: 'New workspace' }))
  const dialog = screen.getByRole('dialog', { name: 'Create workspace' })

  await user.click(within(dialog).getByRole('button', { name: 'Create' }))
  expect(await within(dialog).findByText('Enter a name')).toBeInTheDocument()
  expect(api.createWorkspace).not.toHaveBeenCalled()

  await user.type(within(dialog).getByLabelText('Workspace name'), 'Research')
  await user.click(within(dialog).getByRole('button', { name: 'Create' }))

  expect(await screen.findByText('Workspace page')).toBeInTheDocument()
  expect(api.createWorkspace).toHaveBeenCalledWith('Research')
})

test('a pending invitation can be accepted from the home page', async () => {
  const user = userEvent.setup()
  vi.mocked(api.listReceivedInvitations).mockResolvedValue([INVITATION])
  vi.mocked(api.acceptInvitation).mockResolvedValue({
    id: 'ws-3',
    name: 'Design',
    role: 'ADMIN',
    createdAt: '2026-01-01T00:00:00Z',
  })
  renderHome()

  expect(await screen.findByText('Pending invitations')).toBeInTheDocument()
  expect(screen.getByText(/invited you to join/)).toHaveTextContent(
    'Grace invited you to join Design as Admin.',
  )

  await user.click(screen.getByRole('button', { name: 'Accept invitation to Design' }))

  await vi.waitFor(() => expect(api.acceptInvitation).toHaveBeenCalledWith('inv-1'))
  // Both lists are refreshed after joining.
  await vi.waitFor(() => expect(api.listWorkspaces).toHaveBeenCalledTimes(2))
  expect(api.listReceivedInvitations).toHaveBeenCalledTimes(2)
})

test('a pending invitation can be declined', async () => {
  const user = userEvent.setup()
  vi.mocked(api.listReceivedInvitations).mockResolvedValue([INVITATION])
  vi.mocked(api.declineInvitation).mockResolvedValue(undefined)
  renderHome()

  await user.click(await screen.findByRole('button', { name: 'Decline invitation to Design' }))

  await vi.waitFor(() => expect(api.declineInvitation).toHaveBeenCalledWith('inv-1'))
})
