import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route, Routes } from 'react-router'
import { beforeEach, expect, test, vi } from 'vitest'
import { sampleBoard } from '../../../test/fixtures'
import { renderWithProviders } from '../../../test/renderWithProviders'
import * as labelsApi from '../../labels/api'
import * as api from '../api'
import { BoardHeader } from './BoardHeader'

vi.mock('../api')
vi.mock('../../labels/api')

function renderHeader() {
  return renderWithProviders(
    <Routes>
      <Route path="/boards/:boardId" element={<BoardHeader board={sampleBoard('ADMIN')} />} />
      <Route path="/workspaces/:workspaceId" element={<p>Workspace page</p>} />
    </Routes>,
    { route: '/boards/board-1' },
  )
}

beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(labelsApi.listLabels).mockResolvedValue([])
})

test('edits the name and description of the board', async () => {
  const user = userEvent.setup()
  vi.mocked(api.updateBoard).mockResolvedValue({
    id: 'board-1',
    workspaceId: 'ws-1',
    name: 'Roadmap 2027',
    description: 'Next year',
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-02T00:00:00Z',
  })
  renderHeader()

  await user.click(screen.getByRole('button', { name: 'Edit board' }))
  const dialog = screen.getByRole('dialog', { name: 'Edit board' })
  const name = within(dialog).getByLabelText('Board name')
  expect(name).toHaveValue('Roadmap')
  await user.clear(name)
  await user.type(name, 'Roadmap 2027')
  await user.type(within(dialog).getByLabelText('Description (optional)'), 'Next year')
  await user.click(within(dialog).getByRole('button', { name: 'Save' }))

  await vi.waitFor(() =>
    expect(api.updateBoard).toHaveBeenCalledWith('board-1', {
      name: 'Roadmap 2027',
      description: 'Next year',
    }),
  )
})

test('deleting the board asks first, then goes back to the workspace with a confirmation', async () => {
  const user = userEvent.setup()
  vi.mocked(api.deleteBoard).mockResolvedValue(undefined)
  renderHeader()

  await user.click(screen.getByRole('button', { name: 'Delete board' }))
  const dialog = screen.getByRole('dialog', { name: 'Delete board' })
  expect(dialog).toHaveTextContent('Delete Roadmap with all its columns and cards?')
  await user.click(within(dialog).getByRole('button', { name: 'Delete board' }))

  expect(await screen.findByText('Workspace page')).toBeInTheDocument()
  expect(api.deleteBoard).toHaveBeenCalledWith('board-1')
  expect(await screen.findByText('Board Roadmap deleted')).toBeInTheDocument()
})

test('opens the label manager', async () => {
  const user = userEvent.setup()
  renderHeader()

  await user.click(screen.getByRole('button', { name: 'Labels' }))

  expect(screen.getByRole('dialog', { name: 'Labels' })).toBeInTheDocument()
  await vi.waitFor(() => expect(labelsApi.listLabels).toHaveBeenCalledWith('board-1'))
})
