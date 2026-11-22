import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, expect, test, vi } from 'vitest'
import * as apiClient from '../../lib/apiClient'
import type { AuthResponse } from '../../types/api'
import * as authApi from './api'
import { useAuth } from './AuthContext'
import { AuthProvider } from './AuthProvider'

vi.mock('./api')
vi.mock('../../lib/apiClient', () => ({
  refreshSession: vi.fn(),
  setAccessToken: vi.fn(),
  onSessionExpired: vi.fn(),
}))

const SESSION: AuthResponse = {
  accessToken: 'token-1',
  expiresIn: 900,
  user: {
    id: 'u-1',
    email: 'ada@example.com',
    displayName: 'Ada',
    createdAt: '2026-01-01T00:00:00Z',
  },
}

/** Shows the auth state and exposes the actions as buttons. */
function Probe() {
  const auth = useAuth()
  return (
    <div>
      <output aria-label="status">{auth.status}</output>
      <output aria-label="user">{auth.user?.displayName ?? '-'}</output>
      <button onClick={() => void auth.login({ email: 'ada@example.com', password: 'pw' })}>
        login
      </button>
      <button onClick={() => void auth.logout()}>logout</button>
      <button onClick={() => auth.updateUser({ ...SESSION.user, displayName: 'Ada L.' })}>
        rename
      </button>
    </div>
  )
}

let expire: () => void = () => {}

function renderProvider(queryClient = new QueryClient()) {
  render(
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <Probe />
      </AuthProvider>
    </QueryClientProvider>,
  )
  return queryClient
}

const status = () => screen.getByRole('status', { name: 'status' })
const user = () => screen.getByRole('status', { name: 'user' })

beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(apiClient.onSessionExpired).mockImplementation((handler) => {
    expire = handler
    return () => {}
  })
})

test('restores the session from the refresh cookie on load', async () => {
  vi.mocked(apiClient.refreshSession).mockResolvedValue(SESSION)
  renderProvider()

  expect(status()).toHaveTextContent('loading')
  await vi.waitFor(() => expect(status()).toHaveTextContent('authenticated'))
  expect(user()).toHaveTextContent('Ada')
})

test('without a refresh cookie the visitor is signed out', async () => {
  vi.mocked(apiClient.refreshSession).mockResolvedValue(null)
  renderProvider()

  await vi.waitFor(() => expect(status()).toHaveTextContent('unauthenticated'))
})

test('login keeps the access token in memory and logout clears it and the cache', async () => {
  const u = userEvent.setup()
  vi.mocked(apiClient.refreshSession).mockResolvedValue(null)
  vi.mocked(authApi.login).mockResolvedValue(SESSION)
  vi.mocked(authApi.logout).mockRejectedValue(new Error('network down'))
  const queryClient = renderProvider()
  queryClient.setQueryData(['workspaces'], ['private data'])
  await vi.waitFor(() => expect(status()).toHaveTextContent('unauthenticated'))

  await u.click(screen.getByRole('button', { name: 'login' }))
  await vi.waitFor(() => expect(status()).toHaveTextContent('authenticated'))
  expect(apiClient.setAccessToken).toHaveBeenCalledWith('token-1')

  // Even if the server call fails, the user ends up signed out locally.
  await u.click(screen.getByRole('button', { name: 'logout' }))
  await vi.waitFor(() => expect(status()).toHaveTextContent('unauthenticated'))
  expect(apiClient.setAccessToken).toHaveBeenLastCalledWith(null)
  expect(queryClient.getQueryData(['workspaces'])).toBeUndefined()
})

test('updateUser replaces the signed-in user', async () => {
  const u = userEvent.setup()
  vi.mocked(apiClient.refreshSession).mockResolvedValue(SESSION)
  renderProvider()
  await vi.waitFor(() => expect(user()).toHaveTextContent('Ada'))

  await u.click(screen.getByRole('button', { name: 'rename' }))

  expect(user()).toHaveTextContent('Ada L.')
})

test('an expired session that cannot be renewed signs the user out', async () => {
  vi.mocked(apiClient.refreshSession).mockResolvedValue(SESSION)
  renderProvider()
  await vi.waitFor(() => expect(status()).toHaveTextContent('authenticated'))

  act(() => expire())

  expect(status()).toHaveTextContent('unauthenticated')
})
