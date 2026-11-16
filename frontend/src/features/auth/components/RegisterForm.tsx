import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Alert } from '../../../components/ui/Alert'
import { Button } from '../../../components/ui/Button'
import { TextField } from '../../../components/ui/TextField'
import { useAuth } from '../AuthContext'
import { applyApiError } from '../formErrors'
import { registerSchema, type RegisterValues } from '../schemas'

export function RegisterForm({ onSuccess }: { onSuccess: () => void }) {
  const { register: registerAccount } = useAuth()
  const [formError, setFormError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<RegisterValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: { displayName: '', email: '', password: '', confirmPassword: '' },
  })

  const mutation = useMutation({
    // confirmPassword only exists in the form; it is never sent to the API.
    mutationFn: ({ displayName, email, password }: RegisterValues) =>
      registerAccount({ displayName, email, password }),
    onSuccess,
    onError: (error) =>
      setFormError(applyApiError(error, setError, ['displayName', 'email', 'password'])),
  })

  const onSubmit = handleSubmit((values) => {
    setFormError(null)
    mutation.mutate(values)
  })

  return (
    <form onSubmit={onSubmit} noValidate className="flex flex-col gap-4">
      {formError && <Alert>{formError}</Alert>}

      <TextField
        label="Name"
        autoComplete="name"
        error={errors.displayName?.message}
        {...register('displayName')}
      />
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
        autoComplete="new-password"
        error={errors.password?.message}
        {...register('password')}
      />
      <TextField
        label="Confirm password"
        type="password"
        autoComplete="new-password"
        error={errors.confirmPassword?.message}
        {...register('confirmPassword')}
      />

      <Button type="submit" loading={mutation.isPending}>
        {mutation.isPending ? 'Creating account…' : 'Create account'}
      </Button>
    </form>
  )
}
