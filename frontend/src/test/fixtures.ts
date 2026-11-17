import type { BoardDetail, Card, CardSummary, Member, WorkspaceRole } from '../types/api'

/** Small builders for the API shapes, so tests only spell out what they care about. */

export function cardSummary(id: string, columnId: string, title = id.toUpperCase()): CardSummary {
  return { id, columnId, title, position: 0, assignee: null, dueDate: null, version: 0 }
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
