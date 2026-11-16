import type { ReactNode } from 'react'

interface AuthLayoutProps {
  title: string
  children: ReactNode
  footer: ReactNode
}

/** Centered card shared by the login and register pages. */
export function AuthLayout({ title, children, footer }: AuthLayoutProps) {
  return (
    <main className="flex min-h-screen items-center justify-center bg-slate-50 px-4 py-10">
      <div className="w-full max-w-sm">
        <p className="mb-6 text-center text-2xl font-bold tracking-tight text-indigo-600">
          TaskFlow
        </p>
        <div className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
          <h1 className="mb-4 text-lg font-semibold text-slate-900">{title}</h1>
          {children}
        </div>
        <p className="mt-4 text-center text-sm text-slate-600">{footer}</p>
      </div>
    </main>
  )
}
