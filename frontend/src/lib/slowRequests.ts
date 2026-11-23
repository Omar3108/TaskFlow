/**
 * Counts API requests that are taking unusually long. On free hosting the API sleeps when idle
 * and the first request after a while can take up to a minute; the UI uses this count to say
 * "waking up the server" instead of looking frozen.
 */

/** A request slower than this counts as slow. */
export const SLOW_REQUEST_MS = 4000

let slowCount = 0
const listeners = new Set<() => void>()

function notify() {
  listeners.forEach((listener) => listener())
}

export function subscribeToSlowRequests(listener: () => void) {
  listeners.add(listener)
  return () => {
    listeners.delete(listener)
  }
}

export function getSlowRequestCount() {
  return slowCount
}

/** Wraps a request: if it is still pending after SLOW_REQUEST_MS, it counts as slow until it ends. */
export async function trackSlow<T>(request: Promise<T>): Promise<T> {
  let counted = false
  const timer = setTimeout(() => {
    counted = true
    slowCount += 1
    notify()
  }, SLOW_REQUEST_MS)
  try {
    return await request
  } finally {
    clearTimeout(timer)
    if (counted) {
      slowCount -= 1
      notify()
    }
  }
}
