import { SortableContext, useSortable, verticalListSortingStrategy } from '@dnd-kit/sortable'
import { CSS } from '@dnd-kit/utilities'
import { useState } from 'react'
import { toast } from 'sonner'
import { z } from 'zod'
import { Alert } from '../../../components/ui/Alert'
import { ConfirmDialog } from '../../../components/ui/ConfirmDialog'
import { Menu } from '../../../components/ui/Menu'
import { errorMessage } from '../../../lib/errors'
import type { BoardColumn } from '../../../types/api'
import { CardItem } from '../../cards/components/CardItem'
import { useCreateCard } from '../../cards/hooks'
import { newCardSchema } from '../../cards/schemas'
import { useDeleteColumn, useRenameColumn } from '../hooks'
import { columnNameSchema } from '../schemas'
import { InlineNameForm } from './InlineNameForm'

// The inline form has a single field called "name"; for a new card it holds the title.
const newCardTitleSchema = z.object({ name: newCardSchema.shape.title })

interface Props {
  boardId: string
  column: BoardColumn
  canManage: boolean
  onOpenCard: (cardId: string) => void
}

export function ColumnView({ boardId, column, canManage, onOpenCard }: Props) {
  const {
    setNodeRef,
    setActivatorNodeRef,
    attributes,
    listeners,
    transform,
    transition,
    isDragging,
  } = useSortable({
    id: column.id,
    data: { type: 'column' },
    // Members cannot reorder columns, but cards must still be droppable on any column.
    disabled: { draggable: !canManage, droppable: false },
  })
  const [mode, setMode] = useState<'idle' | 'renaming' | 'adding'>('idle')
  const [confirmingDelete, setConfirmingDelete] = useState(false)
  const renameColumn = useRenameColumn(boardId)
  const deleteColumn = useDeleteColumn(boardId)
  const createCard = useCreateCard(boardId)

  return (
    <section
      ref={setNodeRef}
      style={{ transform: CSS.Translate.toString(transform), transition }}
      aria-label={column.name}
      className={`flex max-h-full w-[85vw] max-w-72 shrink-0 snap-start flex-col sm:w-72 gap-3 rounded-xl bg-slate-100 p-3 ${
        isDragging ? 'opacity-40' : ''
      }`}
    >
      <header className="flex items-start justify-between gap-2">
        {mode === 'renaming' ? (
          <div className="flex-1">
            <InlineNameForm
              label={`New name for ${column.name}`}
              schema={columnNameSchema}
              initialValue={column.name}
              submitLabel="Save"
              pending={renameColumn.isPending}
              onCancel={() => setMode('idle')}
              onSubmit={(name) => {
                renameColumn.mutate(
                  { columnId: column.id, name },
                  { onSuccess: () => setMode('idle') },
                )
                return false
              }}
            />
          </div>
        ) : (
          <>
            <h2 className="flex items-center gap-2 px-1 text-sm font-semibold text-slate-800">
              {canManage && (
                <button
                  type="button"
                  ref={setActivatorNodeRef}
                  {...attributes}
                  {...listeners}
                  aria-label={`Move column ${column.name}`}
                  className="cursor-grab touch-none rounded px-0.5 text-slate-400 hover:text-slate-700 focus-visible:outline-2 focus-visible:outline-indigo-600"
                >
                  ⠿
                </button>
              )}
              <span className="break-words">{column.name}</span>
              <span className="font-normal text-slate-500">{column.cards.length}</span>
            </h2>
            {canManage && (
              // One "⋯" menu instead of two buttons per column keeps the headers quiet.
              <Menu
                label={`Column actions for ${column.name}`}
                trigger={
                  <span
                    aria-hidden="true"
                    className="block px-2 py-0.5 text-lg leading-none text-slate-500 hover:text-slate-900"
                  >
                    ⋯
                  </span>
                }
                triggerClassName="hover:bg-slate-200"
                items={[
                  { label: 'Rename', onSelect: () => setMode('renaming') },
                  { label: 'Delete', onSelect: () => setConfirmingDelete(true), danger: true },
                ]}
              />
            )}
          </>
        )}
      </header>
      {renameColumn.isError && <Alert>{errorMessage(renameColumn.error)}</Alert>}

      <SortableContext
        items={column.cards.map((card) => card.id)}
        strategy={verticalListSortingStrategy}
      >
        {/* min-h keeps an empty column tall enough to drop a card on it. */}
        <ul className="flex min-h-10 flex-col gap-2 overflow-y-auto">
          {column.cards.map((card) => (
            <CardItem key={card.id} card={card} onOpen={onOpenCard} />
          ))}
        </ul>
      </SortableContext>

      {mode === 'adding' ? (
        <InlineNameForm
          label={`Title of the new card in ${column.name}`}
          schema={newCardTitleSchema}
          submitLabel="Add card"
          clearOnSubmit
          pending={createCard.isPending}
          onCancel={() => setMode('idle')}
          onSubmit={async (title) => {
            try {
              await createCard.mutateAsync({ columnId: column.id, title })
              return true
            } catch {
              return false
            }
          }}
        />
      ) : (
        <button
          type="button"
          onClick={() => setMode('adding')}
          className="rounded-md px-2 py-1.5 text-left text-sm text-slate-600 hover:bg-slate-200 focus-visible:outline-2 focus-visible:outline-indigo-600"
        >
          + Add card
        </button>
      )}
      {createCard.isError && <Alert>{errorMessage(createCard.error)}</Alert>}

      <ConfirmDialog
        open={confirmingDelete}
        title="Delete column"
        confirmLabel="Delete column"
        loading={deleteColumn.isPending}
        error={deleteColumn.isError ? errorMessage(deleteColumn.error) : null}
        onCancel={() => {
          setConfirmingDelete(false)
          deleteColumn.reset()
        }}
        onConfirm={() =>
          deleteColumn.mutate(column.id, {
            onSuccess: () => {
              setConfirmingDelete(false)
              toast.success(`Column ${column.name} deleted`)
            },
          })
        }
      >
        Delete <strong>{column.name}</strong>
        {column.cards.length > 0
          ? ` and its ${column.cards.length} card${column.cards.length === 1 ? '' : 's'}?`
          : '?'}{' '}
        This cannot be undone.
      </ConfirmDialog>
    </section>
  )
}
