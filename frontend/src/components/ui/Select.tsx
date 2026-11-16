import { useId, type ComponentProps } from 'react'

interface SelectProps extends ComponentProps<'select'> {
  label: string
  /** Keep the label for screen readers but hide it visually (for example inside table rows). */
  hideLabel?: boolean
}

export function Select({
  label,
  hideLabel = false,
  id,
  className = '',
  children,
  ...props
}: SelectProps) {
  const generatedId = useId()
  const selectId = id ?? generatedId

  return (
    <div className="flex flex-col gap-1">
      <label
        htmlFor={selectId}
        className={hideLabel ? 'sr-only' : 'text-sm font-medium text-slate-700'}
      >
        {label}
      </label>
      <select
        id={selectId}
        {...props}
        className={`rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 shadow-xs outline-none focus:ring-2 focus:ring-indigo-500 disabled:bg-slate-100 ${className}`}
      >
        {children}
      </select>
    </div>
  )
}
