import { z } from 'zod'

// These rules mirror the backend Bean Validation constraints. The server remains the source
// of truth; validating here only gives faster feedback.

export const loginSchema = z.object({
  email: z.string().trim().min(1, 'Enter your email'),
  password: z.string().min(1, 'Enter your password'),
})

export const registerSchema = z
  .object({
    displayName: z
      .string()
      .trim()
      .min(1, 'Enter your name')
      .max(80, 'Name must be at most 80 characters'),
    email: z.email('Enter a valid email address').max(254, 'Email is too long'),
    password: z
      .string()
      .min(8, 'Password must be at least 8 characters')
      .max(72, 'Password must be at most 72 characters'),
    confirmPassword: z.string(),
  })
  .refine((values) => values.password === values.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
  })

export type LoginValues = z.infer<typeof loginSchema>
export type RegisterValues = z.infer<typeof registerSchema>
