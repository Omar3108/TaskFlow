import { z } from 'zod'

// Mirrors the backend Bean Validation constraints; the server remains the source of truth.

export const workspaceNameSchema = z.object({
  name: z.string().trim().min(1, 'Enter a name').max(80, 'Name must be at most 80 characters'),
})

export const invitationSchema = z.object({
  email: z.email('Enter a valid email address').max(254, 'Email is too long'),
  role: z.enum(['MEMBER', 'ADMIN']),
})

export type WorkspaceNameValues = z.infer<typeof workspaceNameSchema>
export type InvitationValues = z.infer<typeof invitationSchema>
