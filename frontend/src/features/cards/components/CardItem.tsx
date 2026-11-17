import { useSortable } from '@dnd-kit/sortable'
import { CSS } from '@dnd-kit/utilities'
import type { CardSummary } from '../../../types/api'

interface Props {
  card: CardSummary
  onOpen: (cardId: string) => void
}

/**
 * A draggable card. Mouse: drag it, or click to open it. Keyboard: Space picks it up, the arrow
 * keys move it, Space drops it, Escape cancels; Enter opens it.
 */
export function CardItem({ card, onOpen }: Props) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: card.id,
    data: { type: 'card' },
  })

  return (
    <li
      ref={setNodeRef}
      style={{ transform: CSS.Translate.toString(transform), transition }}
      {...attributes}
      {...listeners}
      onClick={() => onOpen(card.id)}
      onKeyDown={(event) => {
        listeners?.onKeyDown?.(event)
        if (event.key === 'Enter' && !isDragging) onOpen(card.id)
      }}
      aria-label={card.title}
      className={`cursor-grab touch-none rounded-lg border border-slate-200 bg-white p-3 text-left shadow-xs outline-none hover:border-slate-300 focus-visible:ring-2 focus-visible:ring-indigo-500 ${
        isDragging ? 'opacity-40' : ''
      }`}
    >
      <CardContent card={card} />
    </li>
  )
}

/** What a card shows; also used for the copy that follows the pointer while dragging. */
export function CardContent({ card }: { card: CardSummary }) {
  return (
    <div className="flex flex-col gap-2">
      <span className="text-sm font-medium break-words text-slate-900">{card.title}</span>
      {(card.dueDate || card.assignee) && (
        <div className="flex items-center justify-between gap-2 text-xs text-slate-500">
          <span>
            {card.dueDate && <time dateTime={card.dueDate}>Due {formatDate(card.dueDate)}</time>}
          </span>
          {card.assignee && (
            <span
              title={card.assignee.displayName}
              className="flex size-6 shrink-0 items-center justify-center rounded-full bg-indigo-100 font-semibold text-indigo-700"
            >
              {initials(card.assignee.displayName)}
            </span>
          )}
        </div>
      )}
    </div>
  )
}

function initials(name: string) {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]!.toUpperCase())
    .join('')
}

/** "2027-01-31" -> "Jan 31". Parsed as a calendar date, so the time zone cannot shift the day. */
function formatDate(isoDate: string) {
  const [year, month, day] = isoDate.split('-').map(Number)
  return new Date(year!, month! - 1, day).toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
  })
}
