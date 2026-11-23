import { afterEach, beforeEach, expect, test, vi } from 'vitest'
import {
  getSlowRequestCount,
  SLOW_REQUEST_MS,
  subscribeToSlowRequests,
  trackSlow,
} from './slowRequests'

beforeEach(() => {
  vi.useFakeTimers()
})

afterEach(() => {
  vi.useRealTimers()
})

test('a request counts as slow only after the threshold, and stops counting when it ends', async () => {
  let finish: (value: string) => void = () => {}
  const listener = vi.fn()
  const unsubscribe = subscribeToSlowRequests(listener)

  const tracked = trackSlow(new Promise<string>((resolve) => (finish = resolve)))
  await vi.advanceTimersByTimeAsync(SLOW_REQUEST_MS - 1)
  expect(getSlowRequestCount()).toBe(0)

  await vi.advanceTimersByTimeAsync(1)
  expect(getSlowRequestCount()).toBe(1)
  expect(listener).toHaveBeenCalledTimes(1)

  finish('done')
  await expect(tracked).resolves.toBe('done')
  expect(getSlowRequestCount()).toBe(0)
  expect(listener).toHaveBeenCalledTimes(2)
  unsubscribe()
})

test('a fast request never counts, and failures are passed through', async () => {
  await expect(trackSlow(Promise.resolve(1))).resolves.toBe(1)
  await expect(trackSlow(Promise.reject(new Error('boom')))).rejects.toThrow('boom')
  await vi.advanceTimersByTimeAsync(SLOW_REQUEST_MS * 2)
  expect(getSlowRequestCount()).toBe(0)
})
