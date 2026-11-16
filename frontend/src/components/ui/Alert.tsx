import type { ReactNode } from 'react'

/** Error banner. role="alert" makes screen readers announce it as soon as it appears. */
export function Alert({ children }: { children: ReactNode }) {
  return (
    <div
      role="alert"
      className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700"
    >
      {children}
    </div>
  )
}
