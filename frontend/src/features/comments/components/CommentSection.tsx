import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Alert } from '../../../components/ui/Alert'
import { Button } from '../../../components/ui/Button'
import { TextArea } from '../../../components/ui/TextArea'
import { errorMessage } from '../../../lib/errors'
import type { Comment, WorkspaceRole } from '../../../types/api'
import { isAtLeast } from '../../workspaces/permissions'
import { useComments, useCreateComment, useDeleteComment, useUpdateComment } from '../hooks'
import { commentSchema, type CommentValues } from '../schemas'

interface Props {
  cardId: string
  boardId: string
  currentUserId: string | undefined
  role: WorkspaceRole
}

export function CommentSection({ cardId, boardId, currentUserId, role }: Props) {
  const comments = useComments(cardId)
  const createComment = useCreateComment(cardId, boardId)
  // Pages can overlap after concurrent changes; show each comment once.
  const items = [
    ...new Map(
      (comments.data?.pages ?? []).flatMap((page) => page.content).map((c) => [c.id, c]),
    ).values(),
  ]
  const total = comments.data?.pages[0]?.totalElements ?? 0

  return (
    <section
      aria-labelledby="comments-heading"
      className="flex flex-col gap-3 border-t border-slate-200 pt-4"
    >
      <h3 id="comments-heading" className="text-sm font-semibold text-slate-900">
        Comments{total > 0 ? ` (${total})` : ''}
      </h3>

      <CommentForm
        label="Add a comment"
        submitLabel="Comment"
        pending={createComment.isPending}
        error={createComment.isError ? errorMessage(createComment.error) : null}
        onSubmit={async (body) => {
          try {
            await createComment.mutateAsync(body)
            return true
          } catch {
            return false
          }
        }}
      />

      {comments.isPending && (
        <p role="status" className="text-sm text-slate-500">
          Loading comments…
        </p>
      )}
      {comments.isError && <Alert>{errorMessage(comments.error)}</Alert>}
      {comments.isSuccess && items.length === 0 && (
        <p className="text-sm text-slate-500">No comments yet.</p>
      )}

      <ul className="flex flex-col gap-3">
        {items.map((comment) => (
          <CommentItem
            key={comment.id}
            comment={comment}
            cardId={cardId}
            boardId={boardId}
            isAuthor={!!comment.author && comment.author.id === currentUserId}
            canModerate={isAtLeast(role, 'ADMIN')}
          />
        ))}
      </ul>

      {comments.hasNextPage && (
        <div>
          <Button
            size="sm"
            variant="secondary"
            loading={comments.isFetchingNextPage}
            onClick={() => void comments.fetchNextPage()}
          >
            Show older comments
          </Button>
        </div>
      )}
    </section>
  )
}

interface ItemProps {
  comment: Comment
  cardId: string
  boardId: string
  isAuthor: boolean
  canModerate: boolean
}

function CommentItem({ comment, cardId, boardId, isAuthor, canModerate }: ItemProps) {
  const updateComment = useUpdateComment(cardId)
  const deleteComment = useDeleteComment(cardId, boardId)
  const [mode, setMode] = useState<'view' | 'editing' | 'confirmingDelete'>('view')
  const authorName = comment.author?.displayName ?? 'Deleted user'

  return (
    <li className="flex flex-col gap-1 rounded-md bg-slate-50 px-3 py-2">
      <div className="flex flex-wrap items-baseline gap-x-2 text-xs text-slate-500">
        <span className="font-semibold text-slate-800">{authorName}</span>
        <time dateTime={comment.createdAt}>{formatDateTime(comment.createdAt)}</time>
        {comment.edited && <span>(edited)</span>}
      </div>

      {mode === 'editing' ? (
        <CommentForm
          label="Edit comment"
          submitLabel="Save"
          initialBody={comment.body}
          pending={updateComment.isPending}
          error={updateComment.isError ? errorMessage(updateComment.error) : null}
          onCancel={() => setMode('view')}
          onSubmit={async (body) => {
            try {
              await updateComment.mutateAsync({ commentId: comment.id, body })
              setMode('view')
              return false
            } catch {
              return false
            }
          }}
        />
      ) : (
        <p className="text-sm whitespace-pre-wrap break-words text-slate-800">{comment.body}</p>
      )}

      {mode === 'confirmingDelete' && (
        <div className="flex flex-col gap-2">
          <p className="text-sm text-slate-600">Delete this comment?</p>
          {deleteComment.isError && <Alert>{errorMessage(deleteComment.error)}</Alert>}
          <div className="flex gap-2">
            <Button
              size="sm"
              variant="danger"
              loading={deleteComment.isPending}
              onClick={() => deleteComment.mutate(comment.id)}
            >
              Delete comment
            </Button>
            <Button size="sm" variant="secondary" onClick={() => setMode('view')}>
              Cancel
            </Button>
          </div>
        </div>
      )}

      {mode === 'view' && (isAuthor || canModerate) && (
        <div className="flex gap-3 text-xs">
          {isAuthor && (
            <button
              type="button"
              onClick={() => setMode('editing')}
              aria-label={`Edit comment by ${authorName}`}
              className="font-medium text-slate-600 hover:text-slate-900 hover:underline"
            >
              Edit
            </button>
          )}
          <button
            type="button"
            onClick={() => setMode('confirmingDelete')}
            aria-label={`Delete comment by ${authorName}`}
            className="font-medium text-slate-600 hover:text-red-700 hover:underline"
          >
            Delete
          </button>
        </div>
      )}
    </li>
  )
}

interface FormProps {
  label: string
  submitLabel: string
  initialBody?: string
  pending: boolean
  error: string | null
  /** Return true to clear the form. */
  onSubmit: (body: string) => Promise<boolean>
  onCancel?: () => void
}

function CommentForm({
  label,
  submitLabel,
  initialBody = '',
  pending,
  error,
  onSubmit,
  onCancel,
}: FormProps) {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<CommentValues>({
    resolver: zodResolver(commentSchema),
    defaultValues: { body: initialBody },
  })

  const submit = handleSubmit(async ({ body }) => {
    if (await onSubmit(body)) reset({ body: '' })
  })

  return (
    <form onSubmit={submit} noValidate className="flex flex-col gap-2">
      {error && <Alert>{error}</Alert>}
      <TextArea label={label} rows={2} error={errors.body?.message} {...register('body')} />
      <div className="flex gap-2">
        <Button type="submit" size="sm" loading={pending}>
          {submitLabel}
        </Button>
        {onCancel && (
          <Button size="sm" variant="secondary" onClick={onCancel}>
            Cancel
          </Button>
        )}
      </div>
    </form>
  )
}

function formatDateTime(iso: string) {
  return new Date(iso).toLocaleString('en-US', {
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  })
}
