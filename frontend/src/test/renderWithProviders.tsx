import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import type { ReactElement } from 'react'
import { MemoryRouter } from 'react-router'
import { Toaster } from 'sonner'
import { vi } from 'vitest'
import { AuthContext, type AuthContextValue } from '../features/auth/AuthContext'

interface Options {
  auth?: Partial<AuthContextValue>
  /** Initial URL of the in-memory router. */
  route?: string
}

/**
 * Renders a component with the providers it expects, replacing the real AuthProvider by a
 * controllable value so tests need no network.
 */
export function renderWithProviders(ui: ReactElement, { auth = {}, route = '/' }: Options = {}) {
  const authValue: AuthContextValue = {
    status: 'unauthenticated',
    user: null,
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
    updateUser: vi.fn(),
    ...auth,
  }
  // A fresh client per test: no cache shared between tests, and no retries slowing failures.
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })

  return {
    authValue,
    ...render(
      <QueryClientProvider client={queryClient}>
        <AuthContext value={authValue}>
          <MemoryRouter initialEntries={[route]}>{ui}</MemoryRouter>
          <Toaster />
        </AuthContext>
      </QueryClientProvider>,
    ),
  }
}
