/** Optional text fields travel as null when empty, so "no value" has a single representation. */
export function emptyToNull(value: string) {
  const trimmed = value.trim()
  return trimmed === '' ? null : trimmed
}
