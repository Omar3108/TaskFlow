import { render, screen } from '@testing-library/react'
import { afterEach, beforeEach, expect, test, vi } from 'vitest'
import { ErrorBoundary } from './ErrorBoundary'

function Broken(): never {
  throw new Error('boom')
}

beforeEach(() => {
  // React logs caught errors to the console; keep the test output clean.
  vi.spyOn(console, 'error').mockImplementation(() => {})
})

afterEach(() => {
  vi.restoreAllMocks()
})

test('shows a recovery screen instead of a blank page', () => {
  render(
    <ErrorBoundary>
      <Broken />
    </ErrorBoundary>,
  )

  expect(screen.getByRole('alert')).toHaveTextContent('Something went wrong')
  expect(screen.getByRole('button', { name: 'Reload page' })).toBeInTheDocument()
  expect(screen.getByRole('link', { name: 'Go to your workspaces' })).toHaveAttribute('href', '/')
})

test('renders its children when nothing fails', () => {
  render(
    <ErrorBoundary>
      <p>All good</p>
    </ErrorBoundary>,
  )

  expect(screen.getByText('All good')).toBeInTheDocument()
})
