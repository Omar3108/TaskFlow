import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, expect, test, vi } from 'vitest'
import * as workspacesApi from '../features/workspaces/api'
import { renderWithProviders } from '../test/renderWithProviders'
import type { User } from '../types/api'
import { AppRoutes } from './AppRoutes'

vi.mock('../features/workspaces/api')

const ADA: User = {
  id: 'u-ada',
  email: 'ada@example.com',
  displayName: 'Ada Lovelace',
  createdAt: '2026-01-01T00:00:00Z',
}

beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(workspacesApi.listWorkspaces).mockResolvedValue([])
  vi.mocked(workspacesApi.listReceivedInvitations).mockResolvedValue([])
})

const signedIn = { status: 'authenticated' as const, user: ADA }

test('an unknown address shows a not-found page instead of a silent redirect', async () => {
  renderWithProviders(<AppRoutes />, { route: '/no/such/page', auth: signedIn })

  expect(await screen.findByText('Page not found', { selector: 'p' })).toBeInTheDocument()
  expect(screen.getByRole('link', { name: 'Go to your workspaces' })).toHaveAttribute('href', '/')
  expect(document.title).toBe('Page not found · TaskFlow')
})

test('signed-out visitors of an unknown address are asked to sign in first', async () => {
  renderWithProviders(<AppRoutes />, { route: '/no/such/page' })

  expect(await screen.findByRole('button', { name: 'Sign in' })).toBeInTheDocument()
})

test('the account menu leads to the account page and signs out', async () => {
  const user = userEvent.setup()
  const { authValue } = renderWithProviders(<AppRoutes />, { route: '/', auth: signedIn })

  await user.click(await screen.findByRole('button', { name: 'Account menu' }))
  await user.click(screen.getByRole('menuitem', { name: 'Account' }))
  expect(await screen.findByRole('heading', { name: 'Account' })).toBeInTheDocument()

  await user.click(screen.getByRole('button', { name: 'Account menu' }))
  await user.click(screen.getByRole('menuitem', { name: 'Sign out' }))
  expect(authValue.logout).toHaveBeenCalledTimes(1)
})

test('a skip link leads past the header to the main content', async () => {
  renderWithProviders(<AppRoutes />, { route: '/', auth: signedIn })

  expect(await screen.findByRole('link', { name: 'Skip to content' })).toHaveAttribute(
    'href',
    '#main',
  )
  expect(screen.getByRole('main')).toHaveAttribute('id', 'main')
})
