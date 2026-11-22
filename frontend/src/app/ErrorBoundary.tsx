import { Component, type ErrorInfo, type ReactNode } from 'react'

interface State {
  error: Error | null
}

/**
 * Catches rendering errors below it so one broken component shows a message instead of a blank
 * page. React only supports this with a class component (there is no hook equivalent).
 * Mount it with key={location.pathname} so that navigating elsewhere gives a fresh start.
 */
export class ErrorBoundary extends Component<{ children: ReactNode }, State> {
  state: State = { error: null }

  static getDerivedStateFromError(error: Error): State {
    return { error }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    // In production this is where an error-reporting service would be called.
    console.error('Unhandled rendering error', error, info.componentStack)
  }

  render() {
    if (!this.state.error) return this.props.children
    return (
      <div
        role="alert"
        className="mx-auto max-w-md rounded-xl border border-red-200 bg-white p-6 text-center"
      >
        <h1 className="text-lg font-semibold text-slate-900">Something went wrong</h1>
        <p className="mt-2 text-sm text-slate-600">
          This page hit an unexpected error. Reloading usually fixes it.
        </p>
        <div className="mt-4 flex justify-center gap-2">
          <button
            type="button"
            onClick={() => window.location.reload()}
            className="rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-500"
          >
            Reload page
          </button>
          <a
            href="/"
            className="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
          >
            Go to your workspaces
          </a>
        </div>
      </div>
    )
  }
}
