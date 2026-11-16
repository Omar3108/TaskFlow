/** Central registry of TanStack Query cache keys, so invalidations never rely on typos. */
export const queryKeys = {
  currentUser: ['users', 'me'] as const,
}
