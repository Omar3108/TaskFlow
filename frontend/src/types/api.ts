/** Shapes of the JSON exchanged with the backend. Keep in sync with the API DTOs. */

export interface User {
  id: string
  email: string
  displayName: string
  createdAt: string
}

export interface AuthResponse {
  accessToken: string
  /** Access-token lifetime in seconds. */
  expiresIn: number
  user: User
}

export type WorkspaceRole = 'OWNER' | 'ADMIN' | 'MEMBER'

export interface Workspace {
  id: string
  name: string
  /** Role of the signed-in user in this workspace. */
  role: WorkspaceRole
  createdAt: string
}

export interface Member {
  userId: string
  email: string
  displayName: string
  role: WorkspaceRole
  joinedAt: string
}

/** An invitation as seen by the admins of the workspace that sent it. */
export interface Invitation {
  id: string
  email: string
  role: WorkspaceRole
  status: 'PENDING' | 'ACCEPTED' | 'DECLINED' | 'REVOKED' | 'EXPIRED'
  invitedByName: string
  expiresAt: string
  createdAt: string
}

export interface InvitationCreated {
  invitation: Invitation
  /** Raw token for the invitation link. Only returned at creation time. */
  token: string
}

/** An invitation as seen by the person who was invited. */
export interface ReceivedInvitation {
  id: string
  workspaceId: string
  workspaceName: string
  role: WorkspaceRole
  invitedByName: string
  expiresAt: string
}

export interface Board {
  id: string
  workspaceId: string
  name: string
  description: string | null
  createdAt: string
  updatedAt: string
}

export interface Assignee {
  id: string
  displayName: string
  email: string
}

export type LabelColor = 'GRAY' | 'RED' | 'ORANGE' | 'YELLOW' | 'GREEN' | 'BLUE' | 'PURPLE' | 'PINK'

export interface Label {
  id: string
  name: string
  color: LabelColor
}

/** A card as the board shows it: no description, to keep the board response small. */
export interface CardSummary {
  id: string
  columnId: string
  title: string
  position: number
  assignee: Assignee | null
  /** ISO date (yyyy-mm-dd). */
  dueDate: string | null
  /** Alphabetical. */
  labels: Label[]
  commentCount: number
  version: number
}

export interface BoardColumn {
  id: string
  name: string
  position: number
  /** In display order. */
  cards: CardSummary[]
}

/** The whole board, as returned by GET /boards/{id}. */
export interface BoardDetail {
  id: string
  workspaceId: string
  name: string
  description: string | null
  /** Role of the signed-in user in the workspace of this board. */
  role: WorkspaceRole
  /** In display order. */
  columns: BoardColumn[]
}

/** A column as returned by the column endpoints (without its cards). */
export interface ColumnSummary {
  id: string
  boardId: string
  name: string
  position: number
}

export interface Card {
  id: string
  boardId: string
  columnId: string
  title: string
  description: string | null
  position: number
  assignee: Assignee | null
  dueDate: string | null
  /** Alphabetical. */
  labels: Label[]
  /** Optimistic-locking counter: sent back on edit, a stale value is rejected with 409. */
  version: number
  createdAt: string
  updatedAt: string
}

export interface Comment {
  id: string
  cardId: string
  /** null when the author's account no longer exists. */
  author: { id: string; displayName: string } | null
  body: string
  edited: boolean
  createdAt: string
  updatedAt: string
}

/** One page of a paginated list. `page` is zero-based. */
export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

/** RFC 9457 error body returned by the API for every failure. */
export interface ProblemDetail {
  status: number
  title?: string
  detail?: string
  /** Present on validation errors: field name -> message. */
  errors?: Record<string, string>
}
