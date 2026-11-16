import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import { ApiError, apiFetch, onSessionExpired, refreshSession, setAccessToken } from './apiClient'

const fetchMock = vi.fn<typeof fetch>()

function jsonResponse(status: number, body: unknown) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

const session = (accessToken: string) => ({
  accessToken,
  expiresIn: 900,
  user: { id: '1', email: 'ada@example.com', displayName: 'Ada', createdAt: '2026-01-01' },
})

function authorizationOf(callIndex: number) {
  const init = fetchMock.mock.calls[callIndex][1]
  return (init?.headers as Record<string, string>).Authorization
}

beforeEach(() => {
  fetchMock.mockReset()
  vi.stubGlobal('fetch', fetchMock)
  setAccessToken(null)
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('apiFetch', () => {
  test('sends the access token as a Bearer header', async () => {
    setAccessToken('token-1')
    fetchMock.mockResolvedValueOnce(jsonResponse(200, { ok: true }))

    const result = await apiFetch<{ ok: boolean }>('/users/me')

    expect(result).toEqual({ ok: true })
    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(fetchMock.mock.calls[0][0]).toBe('/api/v1/users/me')
    expect(authorizationOf(0)).toBe('Bearer token-1')
  })

  test('on 401 it refreshes the session once and replays the request with the new token', async () => {
    setAccessToken('expired')
    fetchMock
      .mockResolvedValueOnce(jsonResponse(401, { status: 401 }))
      .mockResolvedValueOnce(jsonResponse(200, session('fresh')))
      .mockResolvedValueOnce(jsonResponse(200, { ok: true }))

    const result = await apiFetch<{ ok: boolean }>('/users/me')

    expect(result).toEqual({ ok: true })
    expect(fetchMock.mock.calls.map((call) => call[0])).toEqual([
      '/api/v1/users/me',
      '/api/v1/auth/refresh',
      '/api/v1/users/me',
    ])
    expect(authorizationOf(2)).toBe('Bearer fresh')
  })

  test('when the refresh fails it reports the expired session and throws', async () => {
    const expired = vi.fn()
    const unsubscribe = onSessionExpired(expired)
    setAccessToken('expired')
    fetchMock
      .mockResolvedValueOnce(
        jsonResponse(401, { status: 401, detail: 'Authentication is required' }),
      )
      .mockResolvedValueOnce(jsonResponse(401, { status: 401 }))

    await expect(apiFetch('/users/me')).rejects.toMatchObject({ status: 401 })

    expect(expired).toHaveBeenCalledTimes(1)
    // No third call: the request is not replayed without a session.
    expect(fetchMock).toHaveBeenCalledTimes(2)
    unsubscribe()
  })

  test('public requests never trigger a refresh', async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse(401, { status: 401, detail: 'Invalid e-mail or password' }),
    )

    const error = await apiFetch('/auth/login', { method: 'POST', body: {}, auth: false }).catch(
      (e: unknown) => e,
    )

    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).message).toBe('Invalid e-mail or password')
    expect(fetchMock).toHaveBeenCalledTimes(1)
  })

  test('exposes field errors from a validation failure', async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse(400, { status: 400, detail: 'Validation failed', errors: { email: 'bad' } }),
    )

    const error = await apiFetch('/auth/register', { method: 'POST', body: {}, auth: false }).catch(
      (e: unknown) => e,
    )

    expect((error as ApiError).problem.errors).toEqual({ email: 'bad' })
  })

  test('returns undefined for 204 No Content', async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 204 }))

    await expect(apiFetch('/auth/logout', { method: 'POST', auth: false })).resolves.toBeUndefined()
  })
})

describe('refreshSession', () => {
  test('concurrent callers share a single refresh request', async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse(200, session('fresh')))

    const [first, second] = await Promise.all([refreshSession(), refreshSession()])

    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(first?.accessToken).toBe('fresh')
    expect(second).toBe(first)
  })

  test('resolves to null when there is no session or the network is down', async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse(401, { status: 401 }))
    await expect(refreshSession()).resolves.toBeNull()

    fetchMock.mockRejectedValueOnce(new TypeError('Failed to fetch'))
    await expect(refreshSession()).resolves.toBeNull()
  })
})
