import { apiFetch } from '../../lib/apiClient'
import type {
  Invitation,
  InvitationCreated,
  Member,
  ReceivedInvitation,
  Workspace,
  WorkspaceRole,
} from '../../types/api'

// ---------- workspaces ----------

export function listWorkspaces() {
  return apiFetch<Workspace[]>('/workspaces')
}

export function getWorkspace(workspaceId: string) {
  return apiFetch<Workspace>(`/workspaces/${workspaceId}`)
}

export function createWorkspace(name: string) {
  return apiFetch<Workspace>('/workspaces', { method: 'POST', body: { name } })
}

export function renameWorkspace(workspaceId: string, name: string) {
  return apiFetch<Workspace>(`/workspaces/${workspaceId}`, { method: 'PATCH', body: { name } })
}

export function deleteWorkspace(workspaceId: string) {
  return apiFetch<void>(`/workspaces/${workspaceId}`, { method: 'DELETE' })
}

// ---------- members ----------

export function listMembers(workspaceId: string) {
  return apiFetch<Member[]>(`/workspaces/${workspaceId}/members`)
}

export function changeMemberRole(workspaceId: string, userId: string, role: WorkspaceRole) {
  return apiFetch<Member>(`/workspaces/${workspaceId}/members/${userId}`, {
    method: 'PATCH',
    body: { role },
  })
}

/** Removes a member. With the caller's own id it means "leave the workspace". */
export function removeMember(workspaceId: string, userId: string) {
  return apiFetch<void>(`/workspaces/${workspaceId}/members/${userId}`, { method: 'DELETE' })
}

// ---------- invitations sent by a workspace ----------

export function listInvitations(workspaceId: string) {
  return apiFetch<Invitation[]>(`/workspaces/${workspaceId}/invitations`)
}

export function createInvitation(workspaceId: string, email: string, role: WorkspaceRole) {
  return apiFetch<InvitationCreated>(`/workspaces/${workspaceId}/invitations`, {
    method: 'POST',
    body: { email, role },
  })
}

export function revokeInvitation(workspaceId: string, invitationId: string) {
  return apiFetch<void>(`/workspaces/${workspaceId}/invitations/${invitationId}`, {
    method: 'DELETE',
  })
}

// ---------- invitations received by the signed-in user ----------

export function listReceivedInvitations() {
  return apiFetch<ReceivedInvitation[]>('/users/me/invitations')
}

export function acceptInvitation(invitationId: string) {
  return apiFetch<Workspace>(`/users/me/invitations/${invitationId}/accept`, { method: 'POST' })
}

export function declineInvitation(invitationId: string) {
  return apiFetch<void>(`/users/me/invitations/${invitationId}/decline`, { method: 'POST' })
}

export function previewInvitation(token: string) {
  return apiFetch<ReceivedInvitation>(`/invitations/${encodeURIComponent(token)}`)
}

export function acceptInvitationByToken(token: string) {
  return apiFetch<Workspace>(`/invitations/${encodeURIComponent(token)}/accept`, {
    method: 'POST',
  })
}
