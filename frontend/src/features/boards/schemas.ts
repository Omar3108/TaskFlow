import { z } from 'zod'

// Mirrors the backend Bean Validation constraints; the server remains the source of truth.

export const boardSchema = z.object({
  name: z.string().trim().min(1, 'Enter a name').max(80, 'Name must be at most 80 characters'),
  description: z.string().trim().max(500, 'Description must be at most 500 characters'),
})

export const columnNameSchema = z.object({
  name: z.string().trim().min(1, 'Enter a name').max(60, 'Name must be at most 60 characters'),
})

export type BoardValues = z.infer<typeof boardSchema>
export type ColumnNameValues = z.infer<typeof columnNameSchema>
