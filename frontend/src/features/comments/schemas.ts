import { z } from 'zod'

export const commentSchema = z.object({
  body: z
    .string()
    .trim()
    .min(1, 'Write something first')
    .max(2000, 'A comment can be at most 2000 characters'),
})

export type CommentValues = z.infer<typeof commentSchema>
