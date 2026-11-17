/**
 * Central registry of TanStack Query cache keys, so invalidations never rely on typos.
 *
 * Keys are hierarchical: invalidating ['workspaces'] also invalidates every key that starts
 * with it (one workspace, its members, its invitations).
 */
export const queryKeys = {
  currentUser: ['users', 'me'] as const,
  workspaces: {
    all: ['workspaces'] as const,
    detail: (workspaceId: string) => ['workspaces', workspaceId] as const,
    members: (workspaceId: string) => ['workspaces', workspaceId, 'members'] as const,
    invitations: (workspaceId: string) => ['workspaces', workspaceId, 'invitations'] as const,
    // Under the workspace, so deleting or leaving a workspace also drops its board list.
    boards: (workspaceId: string) => ['workspaces', workspaceId, 'boards'] as const,
  },
  boards: {
    detail: (boardId: string) => ['boards', boardId] as const,
  },
  cards: {
    detail: (cardId: string) => ['cards', cardId] as const,
  },
  invitations: {
    received: ['invitations', 'received'] as const,
    preview: (token: string) => ['invitations', 'preview', token] as const,
  },
}
