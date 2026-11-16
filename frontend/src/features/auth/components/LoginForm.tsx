import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Alert } from '../../../components/ui/Alert'
import { Button } from '../../../components/ui/Button'
import { TextField } from '../../../components/ui/TextField'
import { useAuth } from '../AuthContext'
import { applyApiError } from '../formErrors'
import { loginSchema, type LoginValues } from '../schemas'

export function LoginForm({ onSuccess }: { onSuccess: () => void }) {
  const { login } = useAuth()
  const [formError, setFormError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<LoginValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', password: '' },
  })

  const mutation = useMutation({
    mutationFn: login,
    onSuccess,
    onError: (error) => setFormError(applyApiError(error, setError, ['email', 'password'])),
  })

  const onSubmit = handleSubmit((values) => {
    setFormError(null)
    mutation.mutate(values)
  })

  return (
    <form onSubmit={onSubmit} noValidate className="flex flex-col gap-4">
      {formError && <Alert>{formError}</Alert>}

      <TextField
        label="Email"
        type="email"
        autoComplete="email"
        error={errors.email?.message}
        {...register('email')}
      />
      <TextField
        label="Password"
        type="password"
        autoComplete="current-password"
        error={errors.password?.message}
        {...register('password')}
      />

      <Button type="submit" loading={mutation.isPending}>
        {mutation.isPending ? 'Signing in…' : 'Sign in'}
      </Button>
    </form>
  )
}
