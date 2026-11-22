import { createContext, useContext } from 'react'
import type { User } from '../../types/api'
import type { LoginPayload, RegisterPayload } from './api'

/** 'loading' = still finding out whether a session exists (the refresh call on page load). */
export type AuthStatus = 'loading' | 'authenticated' | 'unauthenticated'

export interface AuthContextValue {
  status: AuthStatus
  user: User | null
  login: (payload: LoginPayload) => Promise<void>
  register: (payload: RegisterPayload) => Promise<void>
  logout: () => Promise<void>
  /** Replaces the signed-in user's data after a profile change. */
  updateUser: (user: User) => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside <AuthProvider>')
  return context
}
