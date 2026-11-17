import { Link, useLocation } from 'react-router'
import { RegisterForm } from '../features/auth/components/RegisterForm'
import { AuthLayout } from './AuthLayout'

export function RegisterPage() {
  const location = useLocation()

  return (
    <AuthLayout
      title="Create your account"
      footer={
        <>
          Already have an account?{' '}
          <Link
            to="/login"
            state={location.state}
            className="font-medium text-indigo-600 hover:underline"
          >
            Sign in
          </Link>
        </>
      }
    >
      <RegisterForm />
    </AuthLayout>
  )
}
