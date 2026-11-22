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
  /**
   * Adding several items in a row: the field is emptied as soon as the value is sent, so the
   * next title can be typed while the previous one is still saving.
   */
  clearOnSubmit?: boolean
  /** Return false when saving failed; with clearOnSubmit the value is then put back. */
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
  clearOnSubmit = false,
  onSubmit,
  onCancel,
}: Props) {
  const {
    register,
    handleSubmit,
    reset,
    setFocus,
    getValues,
    setValue,
    formState: { errors },
  } = useForm<{ name: string }>({
    resolver: zodResolver(schema),
    defaultValues: { name: initialValue },
  })

  const submit = handleSubmit(async ({ name }) => {
    if (!clearOnSubmit) {
      await onSubmit(name)
      return
    }
    // Empty the field before the request, not after it: emptying it when the request finishes
    // would erase whatever was typed in the meantime (a fast typer's next title).
    reset({ name: '' })
    setFocus('name')
    const saved = await onSubmit(name)
    // On failure, give the text back, unless the user has already started typing something new.
    if (!saved && getValues('name') === '') setValue('name', name)
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
        {/* Not disabled while adding: a disabled submit button also blocks submitting with
            Enter, and adding several items in a row is the point of clearOnSubmit. */}
        <Button type="submit" size="sm" loading={pending && !clearOnSubmit}>
          {submitLabel}
        </Button>
        <Button size="sm" variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
      </div>
    </form>
  )
}
