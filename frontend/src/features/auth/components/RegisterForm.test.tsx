import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { expect, test, vi } from 'vitest'
import { ApiError } from '../../../lib/apiClient'
import { renderWithProviders } from '../../../test/renderWithProviders'
import { RegisterForm } from './RegisterForm'

async function fillForm(
  user: ReturnType<typeof userEvent.setup>,
  values: { name: string; email: string; password: string; confirm: string },
) {
  await user.type(screen.getByLabelText('Name'), values.name)
  await user.type(screen.getByLabelText('Email'), values.email)
  await user.type(screen.getByLabelText('Password'), values.password)
  await user.type(screen.getByLabelText('Confirm password'), values.confirm)
  await user.click(screen.getByRole('button', { name: 'Create account' }))
}

test('rejects a short password and mismatching confirmation without calling the API', async () => {
  const user = userEvent.setup()
  const register = vi.fn()
  renderWithProviders(<RegisterForm onSuccess={vi.fn()} />, { auth: { register } })

  await fillForm(user, {
    name: 'Ada',
    email: 'ada@example.com',
    password: 'short',
    confirm: 'different',
  })

  expect(await screen.findByText('Password must be at least 8 characters')).toBeInTheDocument()
  expect(screen.getByText('Passwords do not match')).toBeInTheDocument()
  expect(register).not.toHaveBeenCalled()
})

test('sends the account data without the confirmation field', async () => {
  const user = userEvent.setup()
  const register = vi.fn().mockResolvedValue(undefined)
  const onSuccess = vi.fn()
  renderWithProviders(<RegisterForm onSuccess={onSuccess} />, { auth: { register } })

  await fillForm(user, {
    name: 'Ada Lovelace',
    email: 'ada@example.com',
    password: 's3cret-password',
    confirm: 's3cret-password',
  })

  await vi.waitFor(() => expect(onSuccess).toHaveBeenCalledTimes(1))
  expect(register).toHaveBeenCalledWith({
    displayName: 'Ada Lovelace',
    email: 'ada@example.com',
    password: 's3cret-password',
  })
})

test('shows a conflict from the server as a form error', async () => {
  const user = userEvent.setup()
  const register = vi
    .fn()
    .mockRejectedValue(
      new ApiError({ status: 409, detail: 'An account with this e-mail already exists' }),
    )
  renderWithProviders(<RegisterForm onSuccess={vi.fn()} />, { auth: { register } })

  await fillForm(user, {
    name: 'Ada',
    email: 'ada@example.com',
    password: 's3cret-password',
    confirm: 's3cret-password',
  })

  expect(await screen.findByRole('alert')).toHaveTextContent(
    'An account with this e-mail already exists',
  )
})

test('places server-side field errors next to their inputs', async () => {
  const user = userEvent.setup()
  const register = vi.fn().mockRejectedValue(
    new ApiError({
      status: 400,
      detail: 'Validation failed',
      errors: { email: 'must be a well-formed email address' },
    }),
  )
  renderWithProviders(<RegisterForm onSuccess={vi.fn()} />, { auth: { register } })

  await fillForm(user, {
    name: 'Ada',
    email: 'ada@example.com',
    password: 's3cret-password',
    confirm: 's3cret-password',
  })

  expect(await screen.findByText('must be a well-formed email address')).toBeInTheDocument()
  expect(screen.getByLabelText('Email')).toBeInvalid()
  expect(screen.queryByRole('alert')).not.toBeInTheDocument()
})
