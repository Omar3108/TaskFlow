import { useId, type ComponentProps } from 'react'

interface TextAreaProps extends ComponentProps<'textarea'> {
  label: string
  error?: string
}

/** Labelled multi-line input with an accessible error message, like TextField. */
export function TextArea({ label, error, id, className = '', ...props }: TextAreaProps) {
  const generatedId = useId()
  const inputId = id ?? generatedId
  const errorId = `${inputId}-error`

  return (
    <div className="flex flex-col gap-1">
      <label htmlFor={inputId} className="text-sm font-medium text-slate-700">
        {label}
      </label>
      <textarea
        id={inputId}
        rows={4}
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
