import type {
  BoardDetail,
  Card,
  CardSummary,
  Comment,
  Label,
  Member,
  Page,
  WorkspaceRole,
} from '../types/api'

/** Small builders for the API shapes, so tests only spell out what they care about. */

export function cardSummary(id: string, columnId: string, title = id.toUpperCase()): CardSummary {
  return {
    id,
    columnId,
    title,
    position: 0,
    assignee: null,
    dueDate: null,
    labels: [],
    commentCount: 0,
    version: 0,
  }
}

/** To do: A, B, C. Done: X. Empty: nothing. */
export function sampleBoard(role: WorkspaceRole = 'MEMBER'): BoardDetail {
  return {
    id: 'board-1',
    workspaceId: 'ws-1',
    name: 'Roadmap',
    description: null,
    role,
    columns: [
      {
        id: 'todo',
        name: 'To do',
        position: 1024,
        cards: [cardSummary('a', 'todo'), cardSummary('b', 'todo'), cardSummary('c', 'todo')],
      },
      { id: 'done', name: 'Done', position: 2048, cards: [cardSummary('x', 'done')] },
      { id: 'empty', name: 'Empty', position: 3072, cards: [] },
    ],
  }
}

export function fullCard(overrides: Partial<Card> = {}): Card {
  return {
    id: 'a',
    boardId: 'board-1',
    columnId: 'todo',
    title: 'Write tests',
    description: 'Cover the board',
    position: 1024,
    assignee: { id: 'u-ada', displayName: 'Ada', email: 'ada@example.com' },
    dueDate: '2027-01-31',
    labels: [],
    version: 1,
    createdAt: '2026-11-01T00:00:00Z',
    updatedAt: '2026-11-01T00:00:00Z',
    ...overrides,
  }
}

export function member(
  userId: string,
  displayName: string,
  role: WorkspaceRole = 'MEMBER',
): Member {
  return {
    userId,
    displayName,
    role,
    email: `${userId}@example.com`,
    joinedAt: '2026-01-01T00:00:00Z',
  }
}

export const BUG: Label = { id: 'l-bug', name: 'Bug', color: 'RED' }
export const DOCS: Label = { id: 'l-docs', name: 'Docs', color: 'BLUE' }

export function comment(id: string, body: string, author: Comment['author']): Comment {
  return {
    id,
    cardId: 'a',
    author,
    body,
    edited: false,
    createdAt: '2026-11-20T10:00:00Z',
    updatedAt: '2026-11-20T10:00:00Z',
  }
}

export function page<T>(
  content: T[],
  pageNumber = 0,
  totalPages = 1,
  totalElements = content.length,
): Page<T> {
  return { content, page: pageNumber, size: 10, totalElements, totalPages }
}
