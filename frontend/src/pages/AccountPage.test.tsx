import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, expect, test, vi } from 'vitest'
import * as api from '../features/account/api'
import { ApiError } from '../lib/apiClient'
import { renderWithProviders } from '../test/renderWithProviders'
import type { User } from '../types/api'
import { AccountPage } from './AccountPage'

vi.mock('../features/account/api')

const ADA: User = {
  id: 'u-ada',
  email: 'ada@example.com',
  displayName: 'Ada',
  createdAt: '2026-01-01T00:00:00Z',
}

beforeEach(() => {
  vi.resetAllMocks()
})

function renderAccount() {
  return renderWithProviders(<AccountPage />, { auth: { status: 'authenticated', user: ADA } })
}

test('changes the display name and updates the signed-in user', async () => {
  const user = userEvent.setup()
  vi.mocked(api.updateProfile).mockResolvedValue({ ...ADA, displayName: 'Ada Lovelace' })
  const { authValue } = renderAccount()

  expect(screen.getByText('ada@example.com')).toBeInTheDocument()
  const save = screen.getByRole('button', { name: 'Save changes' })
  // Nothing to save until the name changes.
  expect(save).toBeDisabled()

  const name = screen.getByLabelText('Display name')
  await user.clear(name)
  await user.type(name, '  Ada Lovelace ')
  await user.click(save)

  await vi.waitFor(() => expect(api.updateProfile).toHaveBeenCalledWith('Ada Lovelace'))
  await vi.waitFor(() =>
    expect(authValue.updateUser).toHaveBeenCalledWith({ ...ADA, displayName: 'Ada Lovelace' }),
  )
  expect(await screen.findByText('Profile updated')).toBeInTheDocument()
  expect(document.title).toBe('Account · TaskFlow')
})

test('validates the name and shows server errors', async () => {
  const user = userEvent.setup()
  vi.mocked(api.updateProfile).mockRejectedValue(
    new ApiError({ status: 500, detail: 'Something went wrong. Please try again.' }),
  )
  renderAccount()

  const name = screen.getByLabelText('Display name')
  await user.clear(name)
  await user.click(screen.getByRole('button', { name: 'Save changes' }))
  expect(await screen.findByText('Enter your name')).toBeInTheDocument()
  expect(api.updateProfile).not.toHaveBeenCalled()

  await user.type(name, 'Countess')
  await user.click(screen.getByRole('button', { name: 'Save changes' }))
  expect(await screen.findByRole('alert')).toHaveTextContent('Something went wrong')
})
