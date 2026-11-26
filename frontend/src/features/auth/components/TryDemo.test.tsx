import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { expect, test, vi } from 'vitest'
import { ApiError } from '../../../lib/apiClient'
import { LoginPage } from '../../../pages/LoginPage'
import { renderWithProviders } from '../../../test/renderWithProviders'

test('the login page offers the demo, which starts a sandbox session', async () => {
  const user = userEvent.setup()
  const startDemo = vi.fn().mockResolvedValue(undefined)
  renderWithProviders(<LoginPage />, { auth: { startDemo } })

  await user.click(screen.getByRole('button', { name: 'Try the demo' }))

  expect(startDemo).toHaveBeenCalledTimes(1)
})

test('explains why the demo could not start', async () => {
  const user = userEvent.setup()
  const startDemo = vi.fn().mockRejectedValue(
    new ApiError({
      status: 429,
      detail: 'Too many demos from this address. Please try again in an hour.',
    }),
  )
  renderWithProviders(<LoginPage />, { auth: { startDemo } })

  await user.click(screen.getByRole('button', { name: 'Try the demo' }))

  expect(await screen.findByRole('alert')).toHaveTextContent('Too many demos from this address')
})
