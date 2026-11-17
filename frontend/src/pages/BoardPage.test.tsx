import { screen } from '@testing-library/react'
import { Route, Routes } from 'react-router'
import { beforeEach, expect, test, vi } from 'vitest'
import * as api from '../features/boards/api'
import { ApiError } from '../lib/apiClient'
import { sampleBoard } from '../test/fixtures'
import { renderWithProviders } from '../test/renderWithProviders'
import { BoardPage } from './BoardPage'

vi.mock('../features/boards/api')

function renderBoardPage() {
  return renderWithProviders(
    <Routes>
      <Route path="/boards/:boardId" element={<BoardPage />} />
    </Routes>,
    { route: '/boards/board-1' },
  )
}

beforeEach(() => {
  vi.resetAllMocks()
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
  expect(api.getBoard).toHaveBeenCalledWith('board-1')
})

test('members do not get the board settings', async () => {
  vi.mocked(api.getBoard).mockResolvedValue(sampleBoard('MEMBER'))
  renderBoardPage()

  await screen.findByRole('heading', { name: 'Roadmap' })
  expect(screen.queryByRole('button', { name: 'Edit board' })).not.toBeInTheDocument()
  expect(screen.queryByRole('button', { name: 'Delete board' })).not.toBeInTheDocument()
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
