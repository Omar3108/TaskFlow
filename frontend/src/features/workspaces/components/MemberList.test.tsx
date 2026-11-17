import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, expect, test, vi } from 'vitest'
import { renderWithProviders } from '../../../test/renderWithProviders'
import type { Member, WorkspaceRole } from '../../../types/api'
import * as api from '../api'
import { MemberList } from './MemberList'

// Replace the HTTP layer: these tests are about what the component shows and calls.
vi.mock('../api')

const member = (userId: string, displayName: string, role: WorkspaceRole): Member => ({
  userId,
  displayName,
  role,
  email: `${userId}@example.com`,
  joinedAt: '2026-01-01T00:00:00Z',
})

const TEAM = [
  member('olivia', 'Olivia Owner', 'OWNER'),
  member('adam', 'Adam Admin', 'ADMIN'),
  member('anna', 'Anna Admin', 'ADMIN'),
  member('max', 'Max Member', 'MEMBER'),
]

function renderAs(currentUserId: string, currentRole: WorkspaceRole) {
  return renderWithProviders(
    <MemberList workspaceId="ws-1" currentUserId={currentUserId} currentRole={currentRole} />,
  )
}

beforeEach(() => {
  vi.mocked(api.listMembers).mockReset().mockResolvedValue(TEAM)
  vi.mocked(api.changeMemberRole).mockReset().mockResolvedValue(TEAM[3])
  vi.mocked(api.removeMember).mockReset().mockResolvedValue(undefined)
})

test('the owner can change roles of and remove everyone else', async () => {
  renderAs('olivia', 'OWNER')

  expect(await screen.findByText('Olivia Owner')).toBeInTheDocument()
  expect(screen.getByText('(you)')).toBeInTheDocument()

  for (const name of ['Adam Admin', 'Anna Admin', 'Max Member']) {
    expect(screen.getByRole('combobox', { name: `Role of ${name}` })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: `Remove ${name}` })).toBeInTheDocument()
  }
  // Nothing can be done to the owner's own row.
  expect(screen.queryByRole('combobox', { name: 'Role of Olivia Owner' })).not.toBeInTheDocument()
  expect(screen.queryByRole('button', { name: 'Remove Olivia Owner' })).not.toBeInTheDocument()
})

test('an admin can only remove plain members and cannot change roles', async () => {
  renderAs('adam', 'ADMIN')

  expect(await screen.findByText('Max Member')).toBeInTheDocument()

  expect(screen.queryAllByRole('combobox')).toHaveLength(0)
  expect(screen.getByRole('button', { name: 'Remove Max Member' })).toBeInTheDocument()
  expect(screen.queryByRole('button', { name: 'Remove Anna Admin' })).not.toBeInTheDocument()
  expect(screen.queryByRole('button', { name: 'Remove Olivia Owner' })).not.toBeInTheDocument()
})

test('a plain member sees the list without any action', async () => {
  renderAs('max', 'MEMBER')

  expect(await screen.findByText('Adam Admin')).toBeInTheDocument()

  expect(screen.queryAllByRole('combobox')).toHaveLength(0)
  expect(screen.queryAllByRole('button', { name: /^Remove/ })).toHaveLength(0)
})

test('changing a role calls the API with the new role', async () => {
  const user = userEvent.setup()
  renderAs('olivia', 'OWNER')

  await user.selectOptions(
    await screen.findByRole('combobox', { name: 'Role of Max Member' }),
    'ADMIN',
  )

  await vi.waitFor(() => expect(api.changeMemberRole).toHaveBeenCalledWith('ws-1', 'max', 'ADMIN'))
})

test('removing a member asks for confirmation first', async () => {
  const user = userEvent.setup()
  renderAs('olivia', 'OWNER')

  await user.click(await screen.findByRole('button', { name: 'Remove Max Member' }))

  const dialog = screen.getByRole('dialog', { name: 'Remove member' })
  expect(within(dialog).getByText('Max Member')).toBeInTheDocument()
  expect(api.removeMember).not.toHaveBeenCalled()

  await user.click(within(dialog).getByRole('button', { name: 'Remove' }))

  await vi.waitFor(() => expect(api.removeMember).toHaveBeenCalledWith('ws-1', 'max'))
})
