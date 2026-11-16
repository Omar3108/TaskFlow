import { Link, useNavigate } from 'react-router'
import { RegisterForm } from '../features/auth/components/RegisterForm'
import { AuthLayout } from './AuthLayout'

export function RegisterPage() {
  const navigate = useNavigate()

  return (
    <AuthLayout
      title="Create your account"
      footer={
        <>
          Already have an account?{' '}
          <Link to="/login" className="font-medium text-indigo-600 hover:underline">
            Sign in
          </Link>
        </>
      }
    >
      <RegisterForm onSuccess={() => navigate('/', { replace: true })} />
    </AuthLayout>
  )
}
