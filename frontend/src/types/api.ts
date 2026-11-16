/** Shapes of the JSON exchanged with the backend. Keep in sync with the API DTOs. */

export interface User {
  id: string
  email: string
  displayName: string
  createdAt: string
}

export interface AuthResponse {
  accessToken: string
  /** Access-token lifetime in seconds. */
  expiresIn: number
  user: User
}

/** RFC 9457 error body returned by the API for every failure. */
export interface ProblemDetail {
  status: number
  title?: string
  detail?: string
  /** Present on validation errors: field name -> message. */
  errors?: Record<string, string>
}
