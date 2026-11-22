import {
  closestCorners,
  DndContext,
  DragOverlay,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
  type Announcements,
  type CollisionDetection,
  type DragEndEvent,
  type DragOverEvent,
  type DragStartEvent,
  type UniqueIdentifier,
} from '@dnd-kit/core'
import {
  horizontalListSortingStrategy,
  SortableContext,
  sortableKeyboardCoordinates,
} from '@dnd-kit/sortable'
import { useState } from 'react'
import { Alert } from '../../../components/ui/Alert'
import { errorMessage } from '../../../lib/errors'
import type { BoardDetail } from '../../../types/api'
import { CardContent } from '../../cards/components/CardItem'
import { CardDialog } from '../../cards/components/CardDialog'
import { useMoveCard } from '../../cards/hooks'
import { cardMoveFor, columnMoveFor, locateCard, moveCardTo, moveColumnTo } from '../dnd'
import { useCreateColumn, useMoveColumn } from '../hooks'
import { canManageBoards } from '../permissions'
import { columnNameSchema } from '../schemas'
import { ColumnView } from './ColumnView'
import { InlineNameForm } from './InlineNameForm'

type DragType = 'card' | 'column'

/**
 * Cards may be dropped on cards and on columns (an empty column has no card to hover); a column
 * may only be dropped on columns, never on the cards inside them.
 */
const collisionDetection: CollisionDetection = (args) => {
  const type = args.active.data.current?.type as DragType | undefined
  return closestCorners({
    ...args,
    droppableContainers: args.droppableContainers.filter(
      (container) => type === 'card' || container.data.current?.type === 'column',
    ),
  })
}

export function KanbanBoard({ board }: { board: BoardDetail }) {
  const canManage = canManageBoards(board.role)
  const moveCard = useMoveCard(board.id)
  const moveColumn = useMoveColumn(board.id)
  const [openCardId, setOpenCardId] = useState<string | null>(null)
  const [active, setActive] = useState<{ type: DragType; id: string } | null>(null)

  // The arrangement while dragging. It is tied to the board it was made from: as soon as the
  // cache holds a different board (the optimistic update after the drop, a rollback, a
  // refetch), the draft is ignored. That avoids a flash of the old order between the drop and
  // the optimistic update, without copying server data into state permanently.
  const [draft, setDraft] = useState<{ base: BoardDetail; board: BoardDetail } | null>(null)
  const shown = draft && draft.base === board ? draft.board : board

  const sensors = useSensors(
    // A few pixels of movement before a drag starts, so a plain click still opens the card.
    useSensor(PointerSensor, { activationConstraint: { distance: 5 } }),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
      // Enter is kept for opening a card, so only Space picks one up.
      keyboardCodes: { start: ['Space'], cancel: ['Escape'], end: ['Space', 'Enter'] },
    }),
  )

  const nameOf = (id: UniqueIdentifier) => {
    const column = shown.columns.find((c) => c.id === id)
    if (column) return `column ${column.name}`
    for (const c of shown.columns) {
      const card = c.cards.find((item) => item.id === id)
      if (card) return `card ${card.title}`
    }
    return 'item'
  }

  // Screen-reader messages in terms of titles instead of the default internal ids.
  const announcements: Announcements = {
    onDragStart: ({ active }) => `Picked up ${nameOf(active.id)}.`,
    onDragOver: ({ active, over }) =>
      over
        ? `${nameOf(active.id)} is over ${nameOf(over.id)}.`
        : `${nameOf(active.id)} is not over a drop area.`,
    onDragEnd: ({ active, over }) =>
      over
        ? `Dropped ${nameOf(active.id)} at ${nameOf(over.id)}.`
        : `Dropped ${nameOf(active.id)}.`,
    onDragCancel: ({ active }) => `Cancelled. ${nameOf(active.id)} was returned to its place.`,
  }

  function handleDragStart({ active }: DragStartEvent) {
    setActive({ type: active.data.current?.type as DragType, id: String(active.id) })
    setDraft({ base: board, board })
  }

  /** Cards cross into another column while hovering, so the target column makes room. */
  function handleDragOver({ active, over }: DragOverEvent) {
    if (!over || active.data.current?.type !== 'card') return
    const cardId = String(active.id)
    const from = locateCard(shown, cardId)
    if (!from) return

    let target: { columnId: string; index: number } | null
    if (over.data.current?.type === 'column') {
      const column = shown.columns.find((c) => c.id === over.id)
      target = column ? { columnId: column.id, index: column.cards.length } : null
    } else {
      target = locateCard(shown, String(over.id))
    }
    // Reordering inside the same column is animated by dnd-kit and resolved on drop.
    if (!target || target.columnId === from.columnId) return

    setDraft({ base: board, board: moveCardTo(shown, cardId, target.columnId, target.index) })
  }

  function handleDragEnd({ active, over }: DragEndEvent) {
    setActive(null)
    const id = String(active.id)
    if (!over) {
      setDraft(null)
      return
    }

    if (active.data.current?.type === 'column') {
      const toIndex = board.columns.findIndex((column) => column.id === over.id)
      if (over.id === active.id || toIndex === -1) {
        setDraft(null)
        return
      }
      const final = moveColumnTo(board, id, toIndex)
      setDraft({ base: board, board: final })
      moveColumn.mutate(columnMoveFor(final, id)!)
      return
    }

    let final = shown
    const current = locateCard(final, id)
    const overCard = over.data.current?.type === 'card' ? locateCard(final, String(over.id)) : null
    if (current && overCard && over.id !== active.id && overCard.columnId === current.columnId) {
      final = moveCardTo(final, id, overCard.columnId, overCard.index)
    }

    const before = locateCard(board, id)
    const after = locateCard(final, id)
    if (!before || !after || (before.columnId === after.columnId && before.index === after.index)) {
      setDraft(null)
      return
    }
    setDraft({ base: board, board: final })
    moveCard.mutate(cardMoveFor(final, id)!)
  }

  function handleDragCancel() {
    setActive(null)
    setDraft(null)
  }

  const moveError = moveCard.error ?? moveColumn.error
  const activeCard =
    active?.type === 'card'
      ? shown.columns.flatMap((column) => column.cards).find((card) => card.id === active.id)
      : undefined
  const activeColumn =
    active?.type === 'column' ? shown.columns.find((column) => column.id === active.id) : undefined

  return (
    <div className="flex flex-col gap-3">
      {moveError && <Alert>The move could not be saved: {errorMessage(moveError)}</Alert>}

      <DndContext
        sensors={sensors}
        collisionDetection={collisionDetection}
        accessibility={{ announcements }}
        onDragStart={handleDragStart}
        onDragOver={handleDragOver}
        onDragEnd={handleDragEnd}
        onDragCancel={handleDragCancel}
      >
        <div className="flex items-start gap-4 overflow-x-auto pb-4">
          <SortableContext
            items={shown.columns.map((column) => column.id)}
            strategy={horizontalListSortingStrategy}
          >
            {shown.columns.map((column) => (
              <ColumnView
                key={column.id}
                boardId={board.id}
                column={column}
                canManage={canManage}
                onOpenCard={setOpenCardId}
              />
            ))}
          </SortableContext>
          {canManage && <AddColumn boardId={board.id} />}
        </div>

        {/* The copy that follows the pointer; the original stays in place, faded. */}
        <DragOverlay>
          {activeCard && (
            <div className="rotate-2 rounded-lg border border-slate-200 bg-white p-3 shadow-lg">
              <CardContent card={activeCard} />
            </div>
          )}
          {activeColumn && (
            <div className="w-72 rounded-xl bg-slate-100 p-3 text-sm font-semibold text-slate-800 shadow-lg">
              {activeColumn.name}
            </div>
          )}
        </DragOverlay>
      </DndContext>

      {shown.columns.length === 0 && (
        <p className="text-sm text-slate-500">
          {canManage
            ? 'This board has no columns yet. Add one to get started.'
            : 'This board has no columns yet.'}
        </p>
      )}

      {openCardId && (
        <CardDialog
          cardId={openCardId}
          boardId={board.id}
          workspaceId={board.workspaceId}
          role={board.role}
          onClose={() => setOpenCardId(null)}
        />
      )}
    </div>
  )
}

function AddColumn({ boardId }: { boardId: string }) {
  const [adding, setAdding] = useState(false)
  const createColumn = useCreateColumn(boardId)

  return (
    <div className="w-72 shrink-0">
      {adding ? (
        <div className="flex flex-col gap-2 rounded-xl bg-slate-100 p-3">
          <InlineNameForm
            label="Name of the new column"
            schema={columnNameSchema}
            submitLabel="Add column"
            pending={createColumn.isPending}
            onCancel={() => setAdding(false)}
            onSubmit={async (name) => {
              try {
                await createColumn.mutateAsync(name)
                return true
              } catch {
                return false
              }
            }}
          />
          {createColumn.isError && <Alert>{errorMessage(createColumn.error)}</Alert>}
        </div>
      ) : (
        <button
          type="button"
          onClick={() => setAdding(true)}
          className="w-full rounded-xl border border-dashed border-slate-300 px-3 py-2.5 text-left text-sm font-medium text-slate-600 hover:bg-slate-100 focus-visible:outline-2 focus-visible:outline-indigo-600"
        >
          + Add column
        </button>
      )}
    </div>
  )
}
