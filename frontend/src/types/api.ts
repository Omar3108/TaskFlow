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

/** RFC 9457 error body returned by the API for every failure. */
export interface ProblemDetail {
  status: number
  title?: string
  detail?: string
  /** Present on validation errors: field name -> message. */
  errors?: Record<string, string>
}
