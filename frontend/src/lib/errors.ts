import { ApiError } from './apiClient'

/** A message that is safe to show to the user for any failed request. */
export function errorMessage(error: unknown) {
  if (error instanceof ApiError) return error.message
  return 'Could not reach the server. Check your connection and try again.'
}
