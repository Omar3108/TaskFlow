import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

// Vitest globals are disabled, so React Testing Library cannot register its own cleanup.
afterEach(() => {
  cleanup()
})

// jsdom may not implement the <dialog> methods; these minimal versions keep the `open`
// attribute and the `close` event behaving like a browser, which is all the Modal relies on.
if (typeof HTMLDialogElement !== 'undefined') {
  const prototype = HTMLDialogElement.prototype
  if (typeof prototype.showModal !== 'function') {
    prototype.showModal = function showModal(this: HTMLDialogElement) {
      this.setAttribute('open', '')
    }
  }
  if (typeof prototype.close !== 'function') {
    prototype.close = function close(this: HTMLDialogElement) {
      this.removeAttribute('open')
      this.dispatchEvent(new Event('close'))
    }
  }
}
