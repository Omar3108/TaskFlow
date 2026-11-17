import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, expect, test, vi } from 'vitest'
import { ApiError } from '../../../lib/apiClient'
import { fullCard, member } from '../../../test/fixtures'
import { renderWithProviders } from '../../../test/renderWithProviders'
import * as workspacesApi from '../../workspaces/api'
import * as api from '../api'
import { CardDialog } from './CardDialog'

vi.mock('../api')
vi.mock('../../workspaces/api')

function renderDialog(onClose = vi.fn()) {
  renderWithProviders(
    <CardDialog cardId="a" boardId="board-1" workspaceId="ws-1" onClose={onClose} />,
  )
  return onClose
}

beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(api.getCard).mockResolvedValue(fullCard())
  vi.mocked(workspacesApi.listMembers).mockResolvedValue([
    member('u-ada', 'Ada'),
    member('u-grace', 'Grace'),
  ])
})

test('shows the card and saves every field with the version it was read with', async () => {
  const user = userEvent.setup()
  vi.mocked(api.updateCard).mockResolvedValue(fullCard({ title: 'Ship it', version: 2 }))
  const onClose = renderDialog()

  const title = await screen.findByLabelText('Title')
  expect(title).toHaveValue('Write tests')
  expect(screen.getByLabelText('Description')).toHaveValue('Cover the board')
  expect(screen.getByLabelText('Due date')).toHaveValue('2027-01-31')
  await vi.waitFor(() => expect(screen.getByLabelText('Assignee')).toHaveValue('u-ada'))

  await user.clear(title)
  await user.type(title, 'Ship it')
  await user.clear(screen.getByLabelText('Description'))
  await user.selectOptions(screen.getByLabelText('Assignee'), 'u-grace')
  await user.clear(screen.getByLabelText('Due date'))
  await user.click(screen.getByRole('button', { name: 'Save' }))

  await vi.waitFor(() =>
    expect(api.updateCard).toHaveBeenCalledWith('a', {
      title: 'Ship it',
      // Cleared fields are sent as null.
      description: null,
      assigneeId: 'u-grace',
      dueDate: null,
      version: 1,
    }),
  )
  await vi.waitFor(() => expect(onClose).toHaveBeenCalled())
})

test('a conflict explains what happened and loads the latest version on request', async () => {
  const user = userEvent.setup()
  vi.mocked(api.updateCard).mockRejectedValue(
    new ApiError({ status: 409, detail: 'The card was modified by someone else.' }),
  )
  const onClose = renderDialog()

  const title = await screen.findByLabelText('Title')
  await user.type(title, ' (mine)')
  await user.click(screen.getByRole('button', { name: 'Save' }))

  expect(await screen.findByRole('alert')).toHaveTextContent(
    'Someone else changed this card while you were editing it.',
  )
  expect(screen.getByRole('button', { name: 'Save' })).toBeDisabled()
  expect(onClose).not.toHaveBeenCalled()

  vi.mocked(api.getCard).mockResolvedValue(fullCard({ title: 'Their title', version: 2 }))
  await user.click(screen.getByRole('button', { name: 'Load latest version' }))

  await vi.waitFor(() => expect(screen.getByLabelText('Title')).toHaveValue('Their title'))
  expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'Save' })).toBeEnabled()
})

test('other errors are shown as they come from the server', async () => {
  const user = userEvent.setup()
  vi.mocked(api.updateCard).mockRejectedValue(
    new ApiError({ status: 400, detail: 'The assignee must be a member of the workspace' }),
  )
  renderDialog()

  await screen.findByLabelText('Title')
  await user.click(screen.getByRole('button', { name: 'Save' }))

  expect(await screen.findByRole('alert')).toHaveTextContent(
    'The assignee must be a member of the workspace',
  )
})

test('deleting asks for confirmation first', async () => {
  const user = userEvent.setup()
  vi.mocked(api.deleteCard).mockResolvedValue(undefined)
  const onClose = renderDialog()

  await screen.findByLabelText('Title')
  await user.click(screen.getByRole('button', { name: 'Delete' }))
  expect(screen.getByText('Delete this card? This cannot be undone.')).toBeInTheDocument()
  expect(api.deleteCard).not.toHaveBeenCalled()

  await user.click(screen.getByRole('button', { name: 'Delete card' }))

  await vi.waitFor(() => expect(api.deleteCard).toHaveBeenCalledWith('a'))
  await vi.waitFor(() => expect(onClose).toHaveBeenCalled())
})

test('a card that no longer exists says so', async () => {
  vi.mocked(api.getCard).mockRejectedValue(new ApiError({ status: 404, detail: 'Card not found' }))
  renderDialog()

  expect(await screen.findByRole('alert')).toHaveTextContent('This card no longer exists.')
})
