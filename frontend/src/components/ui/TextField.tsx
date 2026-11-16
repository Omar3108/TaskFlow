import { useId, type ComponentProps } from 'react'

interface TextFieldProps extends ComponentProps<'input'> {
  label: string
  error?: string
}

/** Labelled input with an accessible error message. Works with React Hook Form's register(). */
export function TextField({ label, error, id, className = '', ...props }: TextFieldProps) {
  const generatedId = useId()
  const inputId = id ?? generatedId
  const errorId = `${inputId}-error`

  return (
    <div className="flex flex-col gap-1">
      <label htmlFor={inputId} className="text-sm font-medium text-slate-700">
        {label}
      </label>
      <input
        id={inputId}
        {...props}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? errorId : undefined}
        className={`rounded-md border px-3 py-2 text-sm text-slate-900 shadow-xs outline-none focus:ring-2 focus:ring-indigo-500 ${
          error ? 'border-red-500' : 'border-slate-300'
        } ${className}`}
      />
      {error && (
        <p id={errorId} className="text-sm text-red-600">
          {error}
        </p>
      )}
    </div>
  )
}
