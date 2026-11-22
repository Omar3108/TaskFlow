import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { toast } from 'sonner'
import { z } from 'zod'
import { Alert } from '../components/ui/Alert'
import { Button } from '../components/ui/Button'
import { TextField } from '../components/ui/TextField'
import { updateProfile } from '../features/account/api'
import { useAuth } from '../features/auth/AuthContext'
import { errorMessage } from '../lib/errors'
import { useDocumentTitle } from '../lib/useDocumentTitle'

const profileSchema = z.object({
  displayName: z
    .string()
    .trim()
    .min(1, 'Enter your name')
    .max(80, 'Name must be at most 80 characters'),
})

type ProfileValues = z.infer<typeof profileSchema>

export function AccountPage() {
  useDocumentTitle('Account')
  const { user, updateUser } = useAuth()
  const save = useMutation({ mutationFn: (displayName: string) => updateProfile(displayName) })
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isDirty },
  } = useForm<ProfileValues>({
    resolver: zodResolver(profileSchema),
    defaultValues: { displayName: user?.displayName ?? '' },
  })

  if (!user) return null

  const onSubmit = handleSubmit(({ displayName }) =>
    save.mutate(displayName, {
      onSuccess: (updated) => {
        // The header and the "(you)" markers read the user from the auth context.
        updateUser(updated)
        reset({ displayName: updated.displayName })
        toast.success('Profile updated')
      },
    }),
  )

  return (
    <div className="mx-auto max-w-lg">
      <h1 className="mb-6 text-2xl font-semibold text-slate-900">Account</h1>
      <form
        onSubmit={onSubmit}
        noValidate
        className="flex flex-col gap-4 rounded-xl border border-slate-200 bg-white p-6"
      >
        {save.isError && <Alert>{errorMessage(save.error)}</Alert>}
        <div className="flex flex-col gap-1">
          <span className="text-sm font-medium text-slate-700">Email</span>
          <span className="text-sm text-slate-900">{user.email}</span>
        </div>
        <TextField
          label="Display name"
          autoComplete="name"
          error={errors.displayName?.message}
          {...register('displayName')}
        />
        <div>
          <Button type="submit" disabled={!isDirty} loading={save.isPending}>
            Save changes
          </Button>
        </div>
      </form>
    </div>
  )
}
