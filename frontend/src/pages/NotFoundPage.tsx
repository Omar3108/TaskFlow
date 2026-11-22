import { Link } from 'react-router'
import { EmptyState } from '../components/ui/EmptyState'
import { useDocumentTitle } from '../lib/useDocumentTitle'

export function NotFoundPage() {
  useDocumentTitle('Page not found')
  return (
    <div className="mx-auto max-w-md">
      <h1 className="sr-only">Page not found</h1>
      <EmptyState
        title="Page not found"
        description="The address may be mistyped, or the page may have been moved."
        action={
          <Link
            to="/"
            className="rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-500"
          >
            Go to your workspaces
          </Link>
        }
      />
    </div>
  )
}
