import { beforeEach, describe, expect, test, vi } from 'vitest'
import * as accountApi from '../features/account/api'
import * as authApi from '../features/auth/api'
import * as boardsApi from '../features/boards/api'
import * as cardsApi from '../features/cards/api'
import * as commentsApi from '../features/comments/api'
import * as labelsApi from '../features/labels/api'
import * as workspacesApi from '../features/workspaces/api'
import { apiFetch } from '../lib/apiClient'

/**
 * Component tests replace these modules with mocks, so nothing else checks that each function
 * calls the right URL with the right method and body. This table is that check: one row per
 * endpoint, compared against the backend controllers.
 */
vi.mock('../lib/apiClient', () => ({ apiFetch: vi.fn() }))

type Row = [name: string, call: () => unknown, path: string, options?: object]

const B = 'board-1'
const C = 'card-1'

const rows: Row[] = [
  // auth and account
  [
    'login',
    () => authApi.login({ email: 'a@b.c', password: 'pw' }),
    '/auth/login',
    { method: 'POST', body: { email: 'a@b.c', password: 'pw' }, auth: false },
  ],
  [
    'register',
    () => authApi.register({ email: 'a@b.c', password: 'pw', displayName: 'A' }),
    '/auth/register',
    { method: 'POST', body: { email: 'a@b.c', password: 'pw', displayName: 'A' }, auth: false },
  ],
  ['logout', () => authApi.logout(), '/auth/logout', { method: 'POST', auth: false }],
  ['start demo', () => authApi.startDemo(), '/auth/demo', { method: 'POST', auth: false }],
  ['current user', () => authApi.fetchCurrentUser(), '/users/me'],
  [
    'update profile',
    () => accountApi.updateProfile('Ada'),
    '/users/me',
    { method: 'PATCH', body: { displayName: 'Ada' } },
  ],
  // workspaces, members, invitations
  ['list workspaces', () => workspacesApi.listWorkspaces(), '/workspaces'],
  ['get workspace', () => workspacesApi.getWorkspace('ws'), '/workspaces/ws'],
  [
    'create workspace',
    () => workspacesApi.createWorkspace('Team'),
    '/workspaces',
    { method: 'POST', body: { name: 'Team' } },
  ],
  [
    'rename workspace',
    () => workspacesApi.renameWorkspace('ws', 'New'),
    '/workspaces/ws',
    { method: 'PATCH', body: { name: 'New' } },
  ],
  [
    'delete workspace',
    () => workspacesApi.deleteWorkspace('ws'),
    '/workspaces/ws',
    { method: 'DELETE' },
  ],
  ['list members', () => workspacesApi.listMembers('ws'), '/workspaces/ws/members'],
  [
    'change role',
    () => workspacesApi.changeMemberRole('ws', 'u', 'ADMIN'),
    '/workspaces/ws/members/u',
    { method: 'PATCH', body: { role: 'ADMIN' } },
  ],
  [
    'remove member',
    () => workspacesApi.removeMember('ws', 'u'),
    '/workspaces/ws/members/u',
    { method: 'DELETE' },
  ],
  ['list invitations', () => workspacesApi.listInvitations('ws'), '/workspaces/ws/invitations'],
  [
    'create invitation',
    () => workspacesApi.createInvitation('ws', 'x@y.z', 'MEMBER'),
    '/workspaces/ws/invitations',
    { method: 'POST', body: { email: 'x@y.z', role: 'MEMBER' } },
  ],
  [
    'revoke invitation',
    () => workspacesApi.revokeInvitation('ws', 'i'),
    '/workspaces/ws/invitations/i',
    { method: 'DELETE' },
  ],
  ['received invitations', () => workspacesApi.listReceivedInvitations(), '/users/me/invitations'],
  [
    'accept invitation',
    () => workspacesApi.acceptInvitation('i'),
    '/users/me/invitations/i/accept',
    { method: 'POST' },
  ],
  [
    'decline invitation',
    () => workspacesApi.declineInvitation('i'),
    '/users/me/invitations/i/decline',
    { method: 'POST' },
  ],
  ['preview invitation', () => workspacesApi.previewInvitation('a/b'), '/invitations/a%2Fb'],
  [
    'accept by token',
    () => workspacesApi.acceptInvitationByToken('t'),
    '/invitations/t/accept',
    { method: 'POST' },
  ],
  // boards and columns
  ['list boards', () => boardsApi.listBoards('ws'), '/workspaces/ws/boards'],
  [
    'create board',
    () => boardsApi.createBoard('ws', { name: 'R', description: null, defaultColumns: true }),
    '/workspaces/ws/boards',
    { method: 'POST', body: { name: 'R', description: null, defaultColumns: true } },
  ],
  ['get board', () => boardsApi.getBoard(B), `/boards/${B}`],
  [
    'get board filtered',
    () => boardsApi.getBoard(B, { q: 'x', assignee: 'none' }),
    `/boards/${B}?q=x&unassigned=true`,
  ],
  [
    'update board',
    () => boardsApi.updateBoard(B, { name: 'R', description: 'd' }),
    `/boards/${B}`,
    { method: 'PATCH', body: { name: 'R', description: 'd' } },
  ],
  ['delete board', () => boardsApi.deleteBoard(B), `/boards/${B}`, { method: 'DELETE' }],
  [
    'create column',
    () => boardsApi.createColumn(B, 'To do'),
    `/boards/${B}/columns`,
    { method: 'POST', body: { name: 'To do' } },
  ],
  [
    'rename column',
    () => boardsApi.renameColumn('col', 'Done'),
    '/columns/col',
    { method: 'PATCH', body: { name: 'Done' } },
  ],
  ['delete column', () => boardsApi.deleteColumn('col'), '/columns/col', { method: 'DELETE' }],
  [
    'move column',
    () => boardsApi.moveColumn('col', { previousColumnId: 'p', nextColumnId: null }),
    '/columns/col/move',
    { method: 'PATCH', body: { previousColumnId: 'p', nextColumnId: null } },
  ],
  // cards
  [
    'create card',
    () => cardsApi.createCard('col', 'T'),
    '/columns/col/cards',
    { method: 'POST', body: { title: 'T' } },
  ],
  ['get card', () => cardsApi.getCard(C), `/cards/${C}`],
  [
    'update card',
    () =>
      cardsApi.updateCard(C, {
        title: 'T',
        description: null,
        assigneeId: null,
        dueDate: null,
        labelIds: [],
        version: 3,
      }),
    `/cards/${C}`,
    {
      method: 'PATCH',
      body: {
        title: 'T',
        description: null,
        assigneeId: null,
        dueDate: null,
        labelIds: [],
        version: 3,
      },
    },
  ],
  ['delete card', () => cardsApi.deleteCard(C), `/cards/${C}`, { method: 'DELETE' }],
  [
    'move card',
    () => cardsApi.moveCard(C, { columnId: 'col', previousCardId: null, nextCardId: 'n' }),
    `/cards/${C}/move`,
    { method: 'PATCH', body: { columnId: 'col', previousCardId: null, nextCardId: 'n' } },
  ],
  // labels and comments
  ['list labels', () => labelsApi.listLabels(B), `/boards/${B}/labels`],
  [
    'create label',
    () => labelsApi.createLabel(B, { name: 'Bug', color: 'RED' }),
    `/boards/${B}/labels`,
    { method: 'POST', body: { name: 'Bug', color: 'RED' } },
  ],
  [
    'update label',
    () => labelsApi.updateLabel('l', { name: 'Bug', color: 'BLUE' }),
    '/labels/l',
    { method: 'PATCH', body: { name: 'Bug', color: 'BLUE' } },
  ],
  ['delete label', () => labelsApi.deleteLabel('l'), '/labels/l', { method: 'DELETE' }],
  [
    'list comments',
    () => commentsApi.listComments(C, 2, 10),
    `/cards/${C}/comments?page=2&size=10`,
  ],
  [
    'create comment',
    () => commentsApi.createComment(C, 'Hi'),
    `/cards/${C}/comments`,
    { method: 'POST', body: { body: 'Hi' } },
  ],
  [
    'update comment',
    () => commentsApi.updateComment('cm', 'Edited'),
    '/comments/cm',
    { method: 'PATCH', body: { body: 'Edited' } },
  ],
  ['delete comment', () => commentsApi.deleteComment('cm'), '/comments/cm', { method: 'DELETE' }],
]

beforeEach(() => {
  vi.mocked(apiFetch).mockReset().mockResolvedValue(undefined)
})

describe('API functions call the endpoints of the backend', () => {
  test.each(rows)('%s', async (_name, call, path, options) => {
    await call()

    expect(apiFetch).toHaveBeenCalledTimes(1)
    if (options) {
      expect(apiFetch).toHaveBeenCalledWith(path, options)
    } else {
      expect(vi.mocked(apiFetch).mock.calls[0]).toEqual([path])
    }
  })
})
