import { useEffect, useId, useRef, useState, type ReactNode } from 'react'

export interface MenuItem {
  label: string
  onSelect: () => void
  danger?: boolean
}

interface Props {
  /** Accessible name of the trigger button, e.g. "Column actions for To do". */
  label: string
  /** Visible content of the trigger (an icon or text). */
  trigger: ReactNode
  items: MenuItem[]
  triggerClassName?: string
  align?: 'left' | 'right'
}

/**
 * A small dropdown menu following the WAI-ARIA menu button pattern: the trigger says it opens
 * a menu (aria-haspopup, aria-expanded), arrow keys move between items, Escape closes and
 * returns focus to the trigger, and a click outside closes it.
 */
export function Menu({ label, trigger, items, triggerClassName = '', align = 'right' }: Props) {
  const [open, setOpen] = useState(false)
  const containerRef = useRef<HTMLDivElement>(null)
  const triggerRef = useRef<HTMLButtonElement>(null)
  const itemRefs = useRef<(HTMLButtonElement | null)[]>([])
  const menuId = useId()

  useEffect(() => {
    if (!open) return
    itemRefs.current[0]?.focus()
    const onPointerDown = (event: PointerEvent) => {
      if (!containerRef.current?.contains(event.target as Node)) setOpen(false)
    }
    document.addEventListener('pointerdown', onPointerDown)
    return () => document.removeEventListener('pointerdown', onPointerDown)
  }, [open])

  const close = () => {
    setOpen(false)
    triggerRef.current?.focus()
  }

  const onMenuKeyDown = (event: React.KeyboardEvent) => {
    const index = itemRefs.current.findIndex((item) => item === document.activeElement)
    const last = items.length - 1
    const focus = (i: number) => itemRefs.current[i]?.focus()
    if (event.key === 'ArrowDown') focus(index === last ? 0 : index + 1)
    else if (event.key === 'ArrowUp') focus(index <= 0 ? last : index - 1)
    else if (event.key === 'Home') focus(0)
    else if (event.key === 'End') focus(last)
    else if (event.key === 'Escape') {
      // Keep the Escape inside the menu: an enclosing dialog must not close too.
      event.stopPropagation()
      close()
    } else if (event.key === 'Tab') setOpen(false)
    else return
    event.preventDefault()
  }

  return (
    <div ref={containerRef} className="relative">
      <button
        ref={triggerRef}
        type="button"
        aria-label={label}
        aria-haspopup="menu"
        aria-expanded={open}
        aria-controls={open ? menuId : undefined}
        onClick={() => setOpen((value) => !value)}
        className={`rounded-md focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-600 ${triggerClassName}`}
      >
        {trigger}
      </button>
      {open && (
        <div
          id={menuId}
          role="menu"
          aria-label={label}
          onKeyDown={onMenuKeyDown}
          className={`absolute z-20 mt-1 min-w-36 rounded-md border border-slate-200 bg-white py-1 shadow-lg ${
            align === 'right' ? 'right-0' : 'left-0'
          }`}
        >
          {items.map((item, i) => (
            <button
              key={item.label}
              ref={(node) => {
                itemRefs.current[i] = node
              }}
              type="button"
              role="menuitem"
              tabIndex={-1}
              onClick={() => {
                setOpen(false)
                item.onSelect()
              }}
              className={`block w-full px-3 py-1.5 text-left text-sm outline-none hover:bg-slate-100 focus:bg-slate-100 ${
                item.danger ? 'text-red-700' : 'text-slate-700'
              }`}
            >
              {item.label}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
