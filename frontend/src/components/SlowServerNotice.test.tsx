import { act, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, expect, test, vi } from 'vitest'
import { SLOW_REQUEST_MS, trackSlow } from '../lib/slowRequests'
import { SlowServerNotice } from './SlowServerNotice'

beforeEach(() => {
  vi.useFakeTimers()
})

afterEach(() => {
  vi.useRealTimers()
})

test('explains a slow response while it lasts', async () => {
  let finish: () => void = () => {}
  render(<SlowServerNotice />)
  const request = trackSlow(new Promise<void>((resolve) => (finish = resolve)))

  expect(screen.queryByRole('status')).not.toBeInTheDocument()
  await act(() => vi.advanceTimersByTimeAsync(SLOW_REQUEST_MS))
  expect(screen.getByRole('status')).toHaveTextContent('Waking up the server')

  await act(async () => {
    finish()
    await request
  })
  expect(screen.queryByRole('status')).not.toBeInTheDocument()
})
