import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, expect, test, vi } from 'vitest'
import { BUG, fullCard, member, page, sampleBoard } from '../../../test/fixtures'
import { renderWithProviders } from '../../../test/renderWithProviders'
import * as cardsApi from '../../cards/api'
import * as commentsApi from '../../comments/api'
import * as labelsApi from '../../labels/api'
import * as workspacesApi from '../../workspaces/api'
import * as api from '../api'
import { KanbanBoard } from './KanbanBoard'

vi.mock('../api')
vi.mock('../../cards/api')
vi.mock('../../workspaces/api')
vi.mock('../../labels/api')
vi.mock('../../comments/api')

beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(api.getBoard).mockResolvedValue(sampleBoard())
  vi.mocked(cardsApi.createCard).mockResolvedValue(fullCard())
  vi.mocked(cardsApi.getCard).mockResolvedValue(fullCard())
  vi.mocked(workspacesApi.listMembers).mockResolvedValue([member('u-ada', 'Ada')])
  vi.mocked(labelsApi.listLabels).mockResolvedValue([BUG])
  vi.mocked(commentsApi.listComments).mockResolvedValue(page([]))
  vi.mocked(api.createColumn).mockResolvedValue({
    id: 'new',
    boardId: 'board-1',
    name: 'Review',
    position: 4096,
  })
})

const column = (name: string) => screen.getByRole('region', { name })

test('shows the columns in order with their cards in order', () => {
  renderWithProviders(<KanbanBoard board={sampleBoard()} />)

  const columns = screen.getAllByRole('region')
  expect(columns.map((c) => c.getAttribute('aria-label'))).toEqual(['To do', 'Done', 'Empty'])
  const cards = within(column('To do')).getAllByRole('button', { name: /^[ABC]$/ })
  expect(cards.map((card) => card.textContent)).toEqual(['A', 'B', 'C'])
  expect(within(column('Done')).getByRole('button', { name: 'X' })).toBeInTheDocument()
})

test('a plain member can add cards but not manage columns', () => {
  renderWithProviders(<KanbanBoard board={sampleBoard('MEMBER')} />)

  expect(within(column('To do')).getByRole('button', { name: '+ Add card' })).toBeInTheDocument()
  expect(screen.queryByRole('button', { name: '+ Add column' })).not.toBeInTheDocument()
  expect(screen.queryByRole('button', { name: /^Rename column/ })).not.toBeInTheDocument()
  expect(screen.queryByRole('button', { name: /^Delete column/ })).not.toBeInTheDocument()
  expect(screen.queryByRole('button', { name: /^Move column/ })).not.toBeInTheDocument()
})

test('an admin sees the column controls', () => {
  renderWithProviders(<KanbanBoard board={sampleBoard('ADMIN')} />)

  expect(screen.getByRole('button', { name: '+ Add column' })).toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'Rename column To do' })).toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'Delete column Done' })).toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'Move column Empty' })).toBeInTheDocument()
})

test('adding a card sends its title and keeps the form open for the next one', async () => {
  const user = userEvent.setup()
  renderWithProviders(<KanbanBoard board={sampleBoard()} />)

  await user.click(within(column('Done')).getByRole('button', { name: '+ Add card' }))
  const input = within(column('Done')).getByLabelText('Title of the new card in Done')
  await user.type(input, '  Release notes  {Enter}')

  await vi.waitFor(() => expect(cardsApi.createCard).toHaveBeenCalledWith('done', 'Release notes'))
  await vi.waitFor(() => expect(input).toHaveValue(''))
})

test('an empty title is not sent', async () => {
  const user = userEvent.setup()
  renderWithProviders(<KanbanBoard board={sampleBoard()} />)

  await user.click(within(column('To do')).getByRole('button', { name: '+ Add card' }))
  await user.click(within(column('To do')).getByRole('button', { name: 'Add card' }))

  expect(await within(column('To do')).findByText('Enter a title')).toBeInTheDocument()
  expect(cardsApi.createCard).not.toHaveBeenCalled()
})

test('an admin adds a column', async () => {
  const user = userEvent.setup()
  renderWithProviders(<KanbanBoard board={sampleBoard('OWNER')} />)

  await user.click(screen.getByRole('button', { name: '+ Add column' }))
  await user.type(screen.getByLabelText('Name of the new column'), 'Review{Enter}')

  await vi.waitFor(() => expect(api.createColumn).toHaveBeenCalledWith('board-1', 'Review'))
})

test('deleting a column asks for confirmation and mentions its cards', async () => {
  const user = userEvent.setup()
  vi.mocked(api.deleteColumn).mockResolvedValue(undefined)
  renderWithProviders(<KanbanBoard board={sampleBoard('ADMIN')} />)

  await user.click(screen.getByRole('button', { name: 'Delete column To do' }))
  const dialog = screen.getByRole('dialog', { name: 'Delete column' })
  expect(dialog).toHaveTextContent('Delete To do and its 3 cards?')
  await user.click(within(dialog).getByRole('button', { name: 'Delete column' }))

  await vi.waitFor(() => expect(api.deleteColumn).toHaveBeenCalledWith('todo'))
})

test('clicking a card opens it, and so does Enter', async () => {
  const user = userEvent.setup()
  renderWithProviders(<KanbanBoard board={sampleBoard()} />)

  await user.click(screen.getByRole('button', { name: 'A' }))
  const dialog = await screen.findByRole('dialog', { name: 'Card details' })
  expect(await within(dialog).findByLabelText('Title')).toHaveValue('Write tests')
  expect(cardsApi.getCard).toHaveBeenCalledWith('a')

  await user.click(within(dialog).getByRole('button', { name: 'Cancel' }))
  expect(screen.queryByRole('dialog', { name: 'Card details' })).not.toBeInTheDocument()

  screen.getByRole('button', { name: 'B' }).focus()
  await user.keyboard('{Enter}')
  await vi.waitFor(() => expect(cardsApi.getCard).toHaveBeenCalledWith('b'))
})

test('cards show their labels and how many comments they have', () => {
  const board = sampleBoard()
  board.columns[0].cards[0] = { ...board.columns[0].cards[0], labels: [BUG], commentCount: 3 }
  renderWithProviders(<KanbanBoard board={board} />)

  const card = screen.getByRole('button', { name: 'A' })
  expect(within(card).getByRole('list', { name: 'Labels' })).toHaveTextContent('Bug')
  expect(within(card).getByLabelText('3 comments')).toBeInTheDocument()
  expect(
    within(screen.getByRole('button', { name: 'B' })).queryByRole('list'),
  ).not.toBeInTheDocument()
})
