import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { expect, test, vi } from 'vitest'
import { Menu } from './Menu'

function renderMenu() {
  const rename = vi.fn()
  const remove = vi.fn()
  render(
    <>
      <Menu
        label="Column actions"
        trigger="⋯"
        items={[
          { label: 'Rename', onSelect: rename },
          { label: 'Delete', onSelect: remove, danger: true },
        ]}
      />
      <button type="button">Outside</button>
    </>,
  )
  return { rename, remove, trigger: screen.getByRole('button', { name: 'Column actions' }) }
}

test('opens on click, focuses the first item and runs the chosen action', async () => {
  const user = userEvent.setup()
  const { remove, trigger } = renderMenu()
  expect(trigger).toHaveAttribute('aria-expanded', 'false')

  await user.click(trigger)

  expect(trigger).toHaveAttribute('aria-expanded', 'true')
  expect(screen.getByRole('menuitem', { name: 'Rename' })).toHaveFocus()
  await user.click(screen.getByRole('menuitem', { name: 'Delete' }))
  expect(remove).toHaveBeenCalledTimes(1)
  expect(screen.queryByRole('menu')).not.toBeInTheDocument()
})

test('arrow keys move between items and wrap around', async () => {
  const user = userEvent.setup()
  const { rename, trigger } = renderMenu()

  await user.click(trigger)
  await user.keyboard('{ArrowDown}')
  expect(screen.getByRole('menuitem', { name: 'Delete' })).toHaveFocus()
  await user.keyboard('{ArrowDown}')
  expect(screen.getByRole('menuitem', { name: 'Rename' })).toHaveFocus()
  await user.keyboard('{ArrowUp}')
  expect(screen.getByRole('menuitem', { name: 'Delete' })).toHaveFocus()
  await user.keyboard('{Home}{Enter}')

  expect(rename).toHaveBeenCalledTimes(1)
})

test('Escape closes the menu and gives focus back to the trigger', async () => {
  const user = userEvent.setup()
  const { trigger } = renderMenu()

  await user.click(trigger)
  await user.keyboard('{Escape}')

  expect(screen.queryByRole('menu')).not.toBeInTheDocument()
  expect(trigger).toHaveFocus()
})

test('a click outside closes it without running anything', async () => {
  const user = userEvent.setup()
  const { rename, remove, trigger } = renderMenu()

  await user.click(trigger)
  await user.click(screen.getByRole('button', { name: 'Outside' }))

  expect(screen.queryByRole('menu')).not.toBeInTheDocument()
  expect(rename).not.toHaveBeenCalled()
  expect(remove).not.toHaveBeenCalled()
})
