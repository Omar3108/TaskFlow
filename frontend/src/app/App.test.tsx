import { render, screen } from '@testing-library/react'
import { afterEach, expect, test, vi } from 'vitest'
import App from './App'

afterEach(() => {
  vi.unstubAllGlobals()
})

test('a visitor without a session lands on the login page', async () => {
  // The app checks for an existing session on startup; the server answers "no session".
  vi.stubGlobal(
    'fetch',
    vi.fn<typeof fetch>().mockResolvedValue(new Response('{"status":401}', { status: 401 })),
  )

  render(<App />)

  expect(
    await screen.findByRole('heading', { name: 'Sign in to your account' }),
  ).toBeInTheDocument()
})
