import { useLocation } from 'react-router'

/** Router state carried through the login and register pages. */
export interface RedirectState {
  /** Where the visitor was going before being asked to sign in. */
  from?: string
}

/** The page to open after signing in: the one originally requested, or the home page. */
export function useRedirectTarget() {
  const location = useLocation()
  return (location.state as RedirectState | null)?.from ?? '/'
}
