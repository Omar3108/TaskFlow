import { Skeleton, SkeletonStatus } from '../../../components/ui/Skeleton'

/** Placeholder with the shape of a board: title, filter bar and three columns. */
export function BoardSkeleton() {
  return (
    <SkeletonStatus label="Loading board…">
      <Skeleton className="mb-2 h-4 w-32" />
      <Skeleton className="mb-6 h-7 w-64" />
      <Skeleton className="mb-4 h-16 w-full" />
      <div className="flex gap-4 overflow-hidden">
        {[3, 2, 1].map((cards, i) => (
          <div key={i} className="flex w-72 shrink-0 flex-col gap-2 rounded-xl bg-slate-100 p-3">
            <Skeleton className="mb-1 h-4 w-24" />
            {Array.from({ length: cards }, (_, j) => (
              <Skeleton key={j} className="h-16 w-full bg-white" />
            ))}
          </div>
        ))}
      </div>
    </SkeletonStatus>
  )
}
