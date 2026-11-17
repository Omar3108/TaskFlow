import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { Button } from '../../../components/ui/Button'

interface Props {
  label: string
  schema: z.ZodType<{ name: string }, { name: string }>
  initialValue?: string
  submitLabel: string
  pending: boolean
  /** Return true to keep the form open and empty (to add several items in a row). */
  onSubmit: (name: string) => Promise<boolean> | boolean
  onCancel: () => void
}

/** A one-field form used to add cards and columns and to rename columns. Escape cancels. */
export function InlineNameForm({
  label,
  schema,
  initialValue = '',
  submitLabel,
  pending,
  onSubmit,
  onCancel,
}: Props) {
  const {
    register,
    handleSubmit,
    reset,
    setFocus,
    formState: { errors },
  } = useForm<{ name: string }>({
    resolver: zodResolver(schema),
    defaultValues: { name: initialValue },
  })

  const submit = handleSubmit(async ({ name }) => {
    if (await onSubmit(name)) {
      reset({ name: '' })
      setFocus('name')
    }
  })

  return (
    <form
      onSubmit={submit}
      noValidate
      className="flex flex-col gap-2"
      onKeyDown={(event) => {
        if (event.key === 'Escape') {
          event.stopPropagation()
          onCancel()
        }
      }}
    >
      <label className="sr-only" htmlFor={`inline-${label}`}>
        {label}
      </label>
      <input
        id={`inline-${label}`}
        autoFocus
        autoComplete="off"
        {...register('name')}
        aria-invalid={errors.name ? true : undefined}
        className={`rounded-md border bg-white px-2.5 py-1.5 text-sm text-slate-900 outline-none focus:ring-2 focus:ring-indigo-500 ${
          errors.name ? 'border-red-500' : 'border-slate-300'
        }`}
      />
      {errors.name && <p className="text-xs text-red-600">{errors.name.message}</p>}
      <div className="flex gap-2">
        <Button type="submit" size="sm" loading={pending}>
          {submitLabel}
        </Button>
        <Button size="sm" variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
      </div>
    </form>
  )
}
