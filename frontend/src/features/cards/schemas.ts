import { z } from 'zod'

// Mirrors the backend Bean Validation constraints; the server remains the source of truth.

const title = z
  .string()
  .trim()
  .min(1, 'Enter a title')
  .max(200, 'Title must be at most 200 characters')

export const newCardSchema = z.object({ title })

/** Form values: empty strings stand for "none" and become null when sent. */
export const cardSchema = z.object({
  title,
  description: z.string().max(5000, 'Description must be at most 5000 characters'),
  assigneeId: z.string(),
  dueDate: z.string(),
  labelIds: z.array(z.string()),
})

export type NewCardValues = z.infer<typeof newCardSchema>
export type CardValues = z.infer<typeof cardSchema>
