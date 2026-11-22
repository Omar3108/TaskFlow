import { z } from 'zod'
import { LABEL_COLOR_VALUES } from './colors'

// Mirrors the backend Bean Validation constraints; the server remains the source of truth.
export const labelSchema = z.object({
  name: z.string().trim().min(1, 'Enter a name').max(30, 'Name must be at most 30 characters'),
  color: z.enum(LABEL_COLOR_VALUES as [string, ...string[]]),
})

export type LabelValues = z.infer<typeof labelSchema>
