import type { AuthResponse, ProblemDetail } from '../types/api'
import { trackSlow } from './slowRequests'

/**
 * Relative on purpose: in development Vite proxies /api to the backend, and in production the
 * host rewrites it. The browser always sees a same-origin API, so the refresh-token cookie is
 * first-party and no CORS preflight is needed.
 */
const API_BASE = '/api/v1'

/**
 * The access token lives only in this module's memory: not in localStorage, where any injected
 * script could read it. It is lost on a page reload and recovered through the refresh cookie.
 */
let accessToken: string | null = null
let sessionExpiredHandler: (() => void) | null = null
let refreshInFlight: Promise<AuthResponse | null> | null = null

export function setAccessToken(token: string | null) {
  accessToken = token
}

/** Registers a callback for "the session could not be renewed". Returns an unsubscribe function. */
export function onSessionExpired(handler: () => void) {
  sessionExpiredHandler = handler
  return () => {
    if (sessionExpiredHandler === handler) sessionExpiredHandler = null
  }
}

export class ApiError extends Error {
  readonly status: number
  readonly problem: ProblemDetail

  constructor(problem: ProblemDetail) {
    super(problem.detail ?? problem.title ?? `Request failed with status ${problem.status}`)
    this.name = 'ApiError'
    this.status = problem.status
    this.problem = problem
  }
}

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  body?: unknown
  /** Set to false for public endpoints (login, register): no token, no refresh-and-retry. */
  auth?: boolean
}

export async function apiFetch<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { auth = true } = options

  let response = await send(path, options, auth ? accessToken : null)

  if (response.status === 401 && auth) {
    // The access token probably expired: renew the session once and replay the request.
    const session = await refreshSession()
    if (!session) {
      sessionExpiredHandler?.()
      throw await toApiError(response)
    }
    response = await send(path, options, session.accessToken)
  }

  if (!response.ok) throw await toApiError(response)
  if (response.status === 204) return undefined as T
  return (await response.json()) as T
}

/**
 * Exchanges the refresh cookie for a new access token.
 *
 * Single-flight: refresh tokens are single-use, so if several requests fail with 401 at the
 * same time (or React StrictMode runs an effect twice) they must all share one refresh call.
 * A second parallel call would present an already-rotated token and get the session revoked.
 */
export function refreshSession(): Promise<AuthResponse | null> {
  refreshInFlight ??= doRefresh().finally(() => {
    refreshInFlight = null
  })
  return refreshInFlight
}

async function doRefresh(): Promise<AuthResponse | null> {
  try {
    // The first request of a visit: the one most likely to meet a sleeping server.
    const response = await trackSlow(fetch(`${API_BASE}/auth/refresh`, { method: 'POST' }))
    if (!response.ok) {
      accessToken = null
      return null
    }
    const session = (await response.json()) as AuthResponse
    accessToken = session.accessToken
    return session
  } catch {
    // Network failure: treat as "no session" rather than crashing the app on startup.
    return null
  }
}

function send(path: string, options: RequestOptions, token: string | null) {
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (options.body !== undefined) headers['Content-Type'] = 'application/json'
  if (token) headers.Authorization = `Bearer ${token}`

  return trackSlow(
    fetch(`${API_BASE}${path}`, {
      method: options.method ?? 'GET',
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
    }),
  )
}

async function toApiError(response: Response) {
  try {
    return new ApiError((await response.json()) as ProblemDetail)
  } catch {
    // The body was not JSON (for example a proxy error page).
    return new ApiError({
      status: response.status,
      detail: 'Something went wrong. Please try again.',
    })
  }
}
