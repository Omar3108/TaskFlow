import { useQueryClient } from '@tanstack/react-query'
import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { onSessionExpired, refreshSession, setAccessToken } from '../../lib/apiClient'
import type { AuthResponse, User } from '../../types/api'
import * as authApi from './api'
import { AuthContext, type AuthContextValue, type AuthStatus } from './AuthContext'

interface AuthState {
  status: AuthStatus
  user: User | null
}

const SIGNED_OUT: AuthState = { status: 'unauthenticated', user: null }

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [state, setState] = useState<AuthState>({ status: 'loading', user: null })

  useEffect(() => {
    let cancelled = false

    // The access token is kept in memory, so a page reload loses it. If the browser still has
    // the refresh cookie, this call silently restores the session.
    refreshSession().then((session) => {
      if (cancelled) return
      setState(session ? { status: 'authenticated', user: session.user } : SIGNED_OUT)
    })

    // The API client calls this when a request got 401 and the session could not be renewed.
    const unsubscribe = onSessionExpired(() => setState(SIGNED_OUT))

    return () => {
      cancelled = true
      unsubscribe()
    }
  }, [])

  const startSession = useCallback((session: AuthResponse) => {
    setAccessToken(session.accessToken)
    setState({ status: 'authenticated', user: session.user })
  }, [])

  const login = useCallback(
    async (payload: authApi.LoginPayload) => startSession(await authApi.login(payload)),
    [startSession],
  )

  const register = useCallback(
    async (payload: authApi.RegisterPayload) => startSession(await authApi.register(payload)),
    [startSession],
  )

  const startDemo = useCallback(async () => startSession(await authApi.startDemo()), [startSession])

  const logout = useCallback(async () => {
    try {
      await authApi.logout()
    } catch {
      // Even if the server call fails, the user must end up signed out locally.
    }
    setAccessToken(null)
    // Drop every cached server response so the next user never sees this user's data.
    queryClient.clear()
    setState(SIGNED_OUT)
  }, [queryClient])

  const updateUser = useCallback((user: User) => {
    setState((current) => (current.status === 'authenticated' ? { ...current, user } : current))
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({ ...state, login, register, startDemo, logout, updateUser }),
    [state, login, register, startDemo, logout, updateUser],
  )

  return <AuthContext value={value}>{children}</AuthContext>
}
