import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { queryKeys } from '../../lib/queryKeys'
import type { WorkspaceRole } from '../../types/api'
import * as api from './api'

// Queries read server state; mutations change it and then invalidate the affected queries so
// TanStack Query refetches them. Components never copy server data into their own state.

export function useWorkspaces() {
  return useQuery({ queryKey: queryKeys.workspaces.all, queryFn: api.listWorkspaces })
}

export function useWorkspace(workspaceId: string) {
  return useQuery({
    queryKey: queryKeys.workspaces.detail(workspaceId),
    queryFn: () => api.getWorkspace(workspaceId),
  })
}

export function useMembers(workspaceId: string) {
  return useQuery({
    queryKey: queryKeys.workspaces.members(workspaceId),
    queryFn: () => api.listMembers(workspaceId),
  })
}

export function useInvitations(workspaceId: string, enabled: boolean) {
  return useQuery({
    queryKey: queryKeys.workspaces.invitations(workspaceId),
    queryFn: () => api.listInvitations(workspaceId),
    // Members without the admin role would only get a 403.
    enabled,
  })
}

export function useReceivedInvitations() {
  return useQuery({
    queryKey: queryKeys.invitations.received,
    queryFn: api.listReceivedInvitations,
  })
}

export function useInvitationPreview(token: string) {
  return useQuery({
    queryKey: queryKeys.invitations.preview(token),
    queryFn: () => api.previewInvitation(token),
    // An invalid or foreign link will not become valid by asking again.
    retry: false,
  })
}

export function useCreateWorkspace() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (name: string) => api.createWorkspace(name),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.workspaces.all }),
  })
}

export function useRenameWorkspace(workspaceId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (name: string) => api.renameWorkspace(workspaceId, name),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.workspaces.all }),
  })
}

export function useDeleteWorkspace(workspaceId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => api.deleteWorkspace(workspaceId),
    onSuccess: () => {
      // Remove (not refetch) this workspace's queries: refetching would only produce 404s.
      queryClient.removeQueries({ queryKey: queryKeys.workspaces.detail(workspaceId) })
      return queryClient.invalidateQueries({ queryKey: queryKeys.workspaces.all, exact: true })
    },
  })
}

export function useChangeMemberRole(workspaceId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ userId, role }: { userId: string; role: WorkspaceRole }) =>
      api.changeMemberRole(workspaceId, userId, role),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: queryKeys.workspaces.members(workspaceId) }),
  })
}

export function useRemoveMember(workspaceId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (userId: string) => api.removeMember(workspaceId, userId),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: queryKeys.workspaces.members(workspaceId) }),
  })
}

/** Leaving is "remove myself": afterwards this workspace is no longer accessible. */
export function useLeaveWorkspace(workspaceId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (ownUserId: string) => api.removeMember(workspaceId, ownUserId),
    onSuccess: () => {
      queryClient.removeQueries({ queryKey: queryKeys.workspaces.detail(workspaceId) })
      return queryClient.invalidateQueries({ queryKey: queryKeys.workspaces.all, exact: true })
    },
  })
}

export function useCreateInvitation(workspaceId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ email, role }: { email: string; role: WorkspaceRole }) =>
      api.createInvitation(workspaceId, email, role),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: queryKeys.workspaces.invitations(workspaceId) }),
  })
}

export function useRevokeInvitation(workspaceId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (invitationId: string) => api.revokeInvitation(workspaceId, invitationId),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: queryKeys.workspaces.invitations(workspaceId) }),
  })
}

/** Accepting adds a workspace to the user's list and removes the invitation from theirs. */
function useInvalidateAfterJoining() {
  const queryClient = useQueryClient()
  return () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: queryKeys.workspaces.all }),
      queryClient.invalidateQueries({ queryKey: queryKeys.invitations.received }),
    ])
}

export function useAcceptInvitation() {
  const invalidate = useInvalidateAfterJoining()
  return useMutation({
    mutationFn: (invitationId: string) => api.acceptInvitation(invitationId),
    onSuccess: invalidate,
  })
}

export function useAcceptInvitationByToken() {
  const invalidate = useInvalidateAfterJoining()
  return useMutation({
    mutationFn: (token: string) => api.acceptInvitationByToken(token),
    onSuccess: invalidate,
  })
}

export function useDeclineInvitation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (invitationId: string) => api.declineInvitation(invitationId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.invitations.received }),
  })
}
