import { Link, useLocation, useNavigate } from 'react-router'
import { LoginForm } from '../features/auth/components/LoginForm'
import { AuthLayout } from './AuthLayout'

export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  // Set by ProtectedRoute when it redirected an anonymous visitor here.
  const from = (location.state as { from?: string } | null)?.from ?? '/'

  return (
    <AuthLayout
      title="Sign in to your account"
      footer={
        <>
          New to TaskFlow?{' '}
          <Link to="/register" className="font-medium text-indigo-600 hover:underline">
            Create an account
          </Link>
        </>
      }
    >
      <LoginForm onSuccess={() => navigate(from, { replace: true })} />
    </AuthLayout>
  )
}
