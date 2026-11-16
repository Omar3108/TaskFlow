import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { expect, test, vi } from 'vitest'
import { ApiError } from '../../../lib/apiClient'
import { renderWithProviders } from '../../../test/renderWithProviders'
import { LoginForm } from './LoginForm'

test('shows validation messages and does not call the API when the form is empty', async () => {
  const user = userEvent.setup()
  const login = vi.fn()
  renderWithProviders(<LoginForm onSuccess={vi.fn()} />, { auth: { login } })

  await user.click(screen.getByRole('button', { name: 'Sign in' }))

  expect(await screen.findByText('Enter your email')).toBeInTheDocument()
  expect(screen.getByText('Enter your password')).toBeInTheDocument()
  expect(screen.getByLabelText('Email')).toBeInvalid()
  expect(login).not.toHaveBeenCalled()
})

test('submits the credentials and reports success', async () => {
  const user = userEvent.setup()
  const login = vi.fn().mockResolvedValue(undefined)
  const onSuccess = vi.fn()
  renderWithProviders(<LoginForm onSuccess={onSuccess} />, { auth: { login } })

  await user.type(screen.getByLabelText('Email'), 'ada@example.com')
  await user.type(screen.getByLabelText('Password'), 's3cret-password')
  await user.click(screen.getByRole('button', { name: 'Sign in' }))

  await vi.waitFor(() => expect(onSuccess).toHaveBeenCalledTimes(1))
  expect(login.mock.calls[0][0]).toEqual({ email: 'ada@example.com', password: 's3cret-password' })
})

test('shows the error returned by the server', async () => {
  const user = userEvent.setup()
  const login = vi
    .fn()
    .mockRejectedValue(new ApiError({ status: 401, detail: 'Invalid e-mail or password' }))
  const onSuccess = vi.fn()
  renderWithProviders(<LoginForm onSuccess={onSuccess} />, { auth: { login } })

  await user.type(screen.getByLabelText('Email'), 'ada@example.com')
  await user.type(screen.getByLabelText('Password'), 'wrong-password')
  await user.click(screen.getByRole('button', { name: 'Sign in' }))

  expect(await screen.findByRole('alert')).toHaveTextContent('Invalid e-mail or password')
  expect(onSuccess).not.toHaveBeenCalled()
})
