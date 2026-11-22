import { useEffect, useId, useRef, type ReactNode } from 'react'

interface ModalProps {
  open: boolean
  onClose: () => void
  title: string
  children: ReactNode
  size?: 'md' | 'lg'
}

/**
 * Modal built on the native <dialog> element. showModal() gives, for free, what a hand-made
 * modal has to reimplement: focus trapped inside, Escape to close, the rest of the page inert
 * and a backdrop.
 */
export function Modal({ open, onClose, title, children, size = 'md' }: ModalProps) {
  const dialogRef = useRef<HTMLDialogElement>(null)
  const titleId = useId()

  useEffect(() => {
    const dialog = dialogRef.current
    if (!dialog) return
    if (open && !dialog.open) dialog.showModal()
    if (!open && dialog.open) dialog.close()
  }, [open])

  return (
    <dialog
      ref={dialogRef}
      // Fired when the dialog closes for any reason, including the Escape key.
      onClose={onClose}
      aria-labelledby={titleId}
      className={`m-auto w-[calc(100%-2rem)] ${size === 'lg' ? 'max-w-2xl' : 'max-w-md'} rounded-xl bg-white p-0 shadow-xl backdrop:bg-slate-900/40`}
    >
      {/* Mounted only while open, so forms inside start empty every time. */}
      {open && (
        <div className="p-6">
          <h2 id={titleId} className="mb-4 text-lg font-semibold text-slate-900">
            {title}
          </h2>
          {children}
        </div>
      )}
    </dialog>
  )
}
