import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route, Routes, useLocation } from 'react-router'
import { beforeEach, expect, test, vi } from 'vitest'
import * as api from '../features/boards/api'
import * as labelsApi from '../features/labels/api'
import * as workspacesApi from '../features/workspaces/api'
import { ApiError } from '../lib/apiClient'
import { BUG, member, sampleBoard } from '../test/fixtures'
import { renderWithProviders } from '../test/renderWithProviders'
import { BoardPage } from './BoardPage'

vi.mock('../features/boards/api')
vi.mock('../features/labels/api')
vi.mock('../features/workspaces/api')

/** Shows the current query string, to check what the filters write to the URL. */
function CurrentSearch() {
  return <output aria-label="Current search">{useLocation().search}</output>
}

function renderBoardPage(route = '/boards/board-1') {
  return renderWithProviders(
    <Routes>
      <Route
        path="/boards/:boardId"
        element={
          <>
            <BoardPage />
            <CurrentSearch />
          </>
        }
      />
    </Routes>,
    { route },
  )
}

beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(api.getBoard).mockResolvedValue(sampleBoard('ADMIN'))
  vi.mocked(labelsApi.listLabels).mockResolvedValue([BUG])
  vi.mocked(workspacesApi.listMembers).mockResolvedValue([member('u-ada', 'Ada')])
})

test('shows the board with its columns', async () => {
  vi.mocked(api.getBoard).mockResolvedValue({
    ...sampleBoard('ADMIN'),
    description: 'Next quarter',
  })
  renderBoardPage()

  expect(await screen.findByRole('heading', { name: 'Roadmap' })).toBeInTheDocument()
  expect(screen.getByText('Next quarter')).toBeInTheDocument()
  expect(screen.getByRole('region', { name: 'To do' })).toBeInTheDocument()
  expect(screen.getByRole('link', { name: '← Back to workspace' })).toHaveAttribute(
    'href',
    '/workspaces/ws-1',
  )
  expect(screen.getByRole('button', { name: 'Edit board' })).toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'Labels' })).toBeInTheDocument()
  expect(api.getBoard).toHaveBeenCalledWith('board-1', {})
})

test('members do not get the board settings', async () => {
  vi.mocked(api.getBoard).mockResolvedValue(sampleBoard('MEMBER'))
  renderBoardPage()

  await screen.findByRole('heading', { name: 'Roadmap' })
  expect(screen.queryByRole('button', { name: 'Edit board' })).not.toBeInTheDocument()
  expect(screen.queryByRole('button', { name: 'Delete board' })).not.toBeInTheDocument()
  expect(screen.queryByRole('button', { name: 'Labels' })).not.toBeInTheDocument()
})

test('an unknown or foreign board gets one message for both cases', async () => {
  vi.mocked(api.getBoard).mockRejectedValue(
    new ApiError({ status: 404, detail: 'Board not found' }),
  )
  renderBoardPage()

  expect(await screen.findByRole('alert')).toHaveTextContent(
    'This board does not exist or you do not have access to it.',
  )
})

test('filters in the URL are applied on load and shown in the filter bar', async () => {
  renderBoardPage('/boards/board-1?q=login&label=l-bug&due=overdue')

  await screen.findByRole('heading', { name: 'Roadmap' })
  expect(api.getBoard).toHaveBeenCalledWith('board-1', {
    q: 'login',
    label: 'l-bug',
    due: 'overdue',
  })
  expect(screen.getByRole('searchbox', { name: 'Search' })).toHaveValue('login')
  expect(screen.getByRole('combobox', { name: 'Due' })).toHaveValue('overdue')
  await vi.waitFor(() =>
    expect(screen.getByRole('combobox', { name: 'Label' })).toHaveValue('l-bug'),
  )
})

test('typing a search updates the URL after a pause and reloads the board', async () => {
  const user = userEvent.setup()
  renderBoardPage()

  await user.type(await screen.findByRole('searchbox', { name: 'Search' }), 'login')

  await vi.waitFor(() =>
    expect(screen.getByRole('status', { name: 'Current search' })).toHaveTextContent('?q=login'),
  )
  await vi.waitFor(() => expect(api.getBoard).toHaveBeenCalledWith('board-1', { q: 'login' }))
  // One request for the whole word, not one per keystroke.
  expect(api.getBoard).not.toHaveBeenCalledWith('board-1', { q: 'l' })
})

test('choosing filters and clearing them', async () => {
  const user = userEvent.setup()
  renderBoardPage()

  await screen.findByRole('heading', { name: 'Roadmap' })
  await user.selectOptions(screen.getByRole('combobox', { name: 'Assignee' }), 'Unassigned')
  await vi.waitFor(() => expect(api.getBoard).toHaveBeenCalledWith('board-1', { assignee: 'none' }))

  await user.click(screen.getByRole('button', { name: 'Clear filters' }))
  await vi.waitFor(() =>
    expect(screen.getByRole('status', { name: 'Current search' })).toHaveTextContent(''),
  )
  expect(screen.queryByRole('button', { name: 'Clear filters' })).not.toBeInTheDocument()
})

test('says so when no card matches the filters', async () => {
  const empty = sampleBoard()
  empty.columns.forEach((column) => (column.cards = []))
  vi.mocked(api.getBoard).mockResolvedValue(empty)
  renderBoardPage('/boards/board-1?q=zzz')

  expect(await screen.findByText('No cards match these filters.')).toBeInTheDocument()
})
