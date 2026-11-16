import type { FieldValues, Path, UseFormSetError } from 'react-hook-form'
import { ApiError } from '../../lib/apiClient'

/**
 * Shows a failed request in a form: field-level messages from the API go next to their
 * inputs, and anything else is returned as a general message for the form banner.
 */
export function applyApiError<T extends FieldValues>(
  error: unknown,
  setError: UseFormSetError<T>,
  fields: readonly Path<T>[],
): string | null {
  if (!(error instanceof ApiError)) {
    return 'Could not reach the server. Check your connection and try again.'
  }

  const fieldErrors = error.problem.errors
  if (fieldErrors) {
    let matched = false
    for (const field of fields) {
      const message = fieldErrors[field]
      if (message) {
        setError(field, { type: 'server', message })
        matched = true
      }
    }
    if (matched) return null
  }

  return error.message
}
