import { Link, useLocation } from 'react-router'
import { LoginForm } from '../features/auth/components/LoginForm'
import { AuthLayout } from './AuthLayout'
import { useDocumentTitle } from '../lib/useDocumentTitle'

export function LoginPage() {
  useDocumentTitle('Sign in')
  const location = useLocation()

  return (
    <AuthLayout
      title="Sign in to your account"
      footer={
        <>
          New to TaskFlow?{' '}
          {/* Pass the router state along so "where I was going" survives switching pages. */}
          <Link
            to="/register"
            state={location.state}
            className="font-medium text-indigo-600 hover:underline"
          >
            Create an account
          </Link>
        </>
      }
    >
      {/* No navigation here: once the session exists, PublicOnlyRoute redirects. */}
      <LoginForm />
    </AuthLayout>
  )
}
