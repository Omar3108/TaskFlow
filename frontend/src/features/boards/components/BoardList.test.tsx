import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, expect, test, vi } from 'vitest'
import { renderWithProviders } from '../../../test/renderWithProviders'
import type { Board } from '../../../types/api'
import * as api from '../api'
import { BoardList } from './BoardList'

vi.mock('../api')

const board = (id: string, name: string, description: string | null = null): Board => ({
  id,
  workspaceId: 'ws-1',
  name,
  description,
  createdAt: '2026-11-01T00:00:00Z',
  updatedAt: '2026-11-01T00:00:00Z',
})

beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(api.listBoards).mockResolvedValue([
    board('b-1', 'Roadmap', 'What we ship next'),
    board('b-2', 'Bugs'),
  ])
  vi.mocked(api.createBoard).mockResolvedValue(board('b-3', 'Sprint'))
})

test('lists the boards as links, without a create button for plain members', async () => {
  renderWithProviders(<BoardList workspaceId="ws-1" role="MEMBER" />)

  const roadmap = await screen.findByRole('link', { name: /Roadmap/ })
  expect(roadmap).toHaveAttribute('href', '/boards/b-1')
  expect(roadmap).toHaveTextContent('What we ship next')
  expect(screen.getByRole('link', { name: /Bugs/ })).toHaveAttribute('href', '/boards/b-2')
  expect(screen.queryByRole('button', { name: 'New board' })).not.toBeInTheDocument()
  expect(api.listBoards).toHaveBeenCalledWith('ws-1')
})

test('explains an empty workspace according to the role', async () => {
  vi.mocked(api.listBoards).mockResolvedValue([])
  renderWithProviders(<BoardList workspaceId="ws-1" role="MEMBER" />)

  expect(await screen.findByText(/An admin of this workspace can create one/)).toBeInTheDocument()
})

test('an admin creates a board with the default columns', async () => {
  const user = userEvent.setup()
  renderWithProviders(<BoardList workspaceId="ws-1" role="ADMIN" />)

  await user.click(await screen.findByRole('button', { name: 'New board' }))
  const dialog = screen.getByRole('dialog', { name: 'New board' })
  await user.type(within(dialog).getByLabelText('Board name'), '  Sprint  ')
  expect(
    within(dialog).getByRole('checkbox', { name: /To do, In progress and Done/ }),
  ).toBeChecked()
  await user.click(within(dialog).getByRole('button', { name: 'Create board' }))

  await vi.waitFor(() =>
    expect(api.createBoard).toHaveBeenCalledWith('ws-1', {
      name: 'Sprint',
      description: null,
      defaultColumns: true,
    }),
  )
})

test('the default columns can be turned off and the name is required', async () => {
  const user = userEvent.setup()
  renderWithProviders(<BoardList workspaceId="ws-1" role="OWNER" />)

  await user.click(await screen.findByRole('button', { name: 'New board' }))
  const dialog = screen.getByRole('dialog', { name: 'New board' })
  await user.click(within(dialog).getByRole('button', { name: 'Create board' }))
  expect(await within(dialog).findByText('Enter a name')).toBeInTheDocument()
  expect(api.createBoard).not.toHaveBeenCalled()

  await user.type(within(dialog).getByLabelText('Board name'), 'Empty board')
  await user.type(within(dialog).getByLabelText('Description (optional)'), 'Starts blank')
  await user.click(within(dialog).getByRole('checkbox'))
  await user.click(within(dialog).getByRole('button', { name: 'Create board' }))

  await vi.waitFor(() =>
    expect(api.createBoard).toHaveBeenCalledWith('ws-1', {
      name: 'Empty board',
      description: 'Starts blank',
      defaultColumns: false,
    }),
  )
})
