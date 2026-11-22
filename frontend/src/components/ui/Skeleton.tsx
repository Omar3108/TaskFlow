import type { ReactNode } from 'react'

/** A grey placeholder block with the shape of content that is still loading. */
export function Skeleton({ className = '' }: { className?: string }) {
  return <div aria-hidden="true" className={`animate-pulse rounded-md bg-slate-200 ${className}`} />
}

/**
 * Wraps skeleton shapes in a status region: sighted users see the shapes, screen-reader users
 * hear the label ("Loading board…") instead of nothing.
 */
export function SkeletonStatus({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div role="status" aria-live="polite">
      <span className="sr-only">{label}</span>
      {children}
    </div>
  )
}

/** Placeholder for a grid of tiles (workspaces, boards). */
export function TileGridSkeleton({ label, count = 3 }: { label: string; count?: number }) {
  return (
    <SkeletonStatus label={label}>
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {Array.from({ length: count }, (_, i) => (
          <div
            key={i}
            className="flex flex-col gap-3 rounded-xl border border-slate-200 bg-white p-5"
          >
            <Skeleton className="h-4 w-2/3" />
            <Skeleton className="h-3 w-1/3" />
          </div>
        ))}
      </div>
    </SkeletonStatus>
  )
}
