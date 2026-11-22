import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, expect, test, vi } from 'vitest'
import { ApiError } from '../../../lib/apiClient'
import { BUG, DOCS } from '../../../test/fixtures'
import { renderWithProviders } from '../../../test/renderWithProviders'
import * as api from '../api'
import { LabelManagerDialog } from './LabelManagerDialog'

vi.mock('../api')

function renderManager() {
  renderWithProviders(<LabelManagerDialog boardId="board-1" open onClose={vi.fn()} />)
  return screen.getByRole('dialog', { name: 'Labels' })
}

beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(api.listLabels).mockResolvedValue([BUG, DOCS])
  vi.mocked(api.createLabel).mockResolvedValue({ id: 'l-new', name: 'Urgent', color: 'PINK' })
  vi.mocked(api.updateLabel).mockResolvedValue({ ...BUG, name: 'Defect' })
  vi.mocked(api.deleteLabel).mockResolvedValue(undefined)
})

test('lists the labels of the board', async () => {
  const dialog = renderManager()

  expect(await within(dialog).findByText('Bug')).toBeInTheDocument()
  expect(within(dialog).getByText('Docs')).toBeInTheDocument()
  expect(api.listLabels).toHaveBeenCalledWith('board-1')
})

test('creates a label with the chosen colour and clears the form', async () => {
  const user = userEvent.setup()
  const dialog = renderManager()
  await within(dialog).findByText('Bug')

  const name = within(dialog).getByLabelText('Label name')
  await user.type(name, '  Urgent ')
  await user.click(within(dialog).getByRole('radio', { name: 'Pink' }))
  await user.click(within(dialog).getByRole('button', { name: 'Create label' }))

  await vi.waitFor(() =>
    expect(api.createLabel).toHaveBeenCalledWith('board-1', { name: 'Urgent', color: 'PINK' }),
  )
  await vi.waitFor(() => expect(name).toHaveValue(''))
})

test('shows the server message for a duplicate name', async () => {
  const user = userEvent.setup()
  vi.mocked(api.createLabel).mockRejectedValue(
    new ApiError({ status: 409, detail: 'This board already has a label with that name' }),
  )
  const dialog = renderManager()
  await within(dialog).findByText('Bug')

  await user.type(within(dialog).getByLabelText('Label name'), 'bug')
  await user.click(within(dialog).getByRole('button', { name: 'Create label' }))

  expect(await within(dialog).findByRole('alert')).toHaveTextContent(
    'This board already has a label with that name',
  )
})

test('edits a label', async () => {
  const user = userEvent.setup()
  const dialog = renderManager()

  await user.click(await within(dialog).findByRole('button', { name: 'Edit label Bug' }))
  const names = within(dialog).getAllByLabelText('Label name')
  // The first form is the edit form (the "new label" form comes after the list).
  expect(names[0]).toHaveValue('Bug')
  expect(within(dialog).getAllByRole('radio', { name: 'Red' })[0]).toBeChecked()
  await user.clear(names[0])
  await user.type(names[0], 'Defect')
  await user.click(within(dialog).getByRole('button', { name: 'Save' }))

  await vi.waitFor(() =>
    expect(api.updateLabel).toHaveBeenCalledWith('l-bug', { name: 'Defect', color: 'RED' }),
  )
})

test('deleting a label asks for confirmation', async () => {
  const user = userEvent.setup()
  const dialog = renderManager()

  await user.click(await within(dialog).findByRole('button', { name: 'Delete label Docs' }))
  expect(within(dialog).getByText(/It will be removed from every card/)).toBeInTheDocument()
  expect(api.deleteLabel).not.toHaveBeenCalled()
  await user.click(within(dialog).getByRole('button', { name: 'Delete label' }))

  await vi.waitFor(() => expect(api.deleteLabel).toHaveBeenCalledWith('l-docs'))
})

test('the name is required', async () => {
  const user = userEvent.setup()
  const dialog = renderManager()
  await within(dialog).findByText('Bug')

  await user.click(within(dialog).getByRole('button', { name: 'Create label' }))

  expect(await within(dialog).findByText('Enter a name')).toBeInTheDocument()
  expect(api.createLabel).not.toHaveBeenCalled()
})
