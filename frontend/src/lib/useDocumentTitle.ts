import { useEffect } from 'react'

/** Sets the browser tab title ("Roadmap · TaskFlow"); without a title, just "TaskFlow". */
export function useDocumentTitle(title?: string) {
  useEffect(() => {
    document.title = title ? `${title} · TaskFlow` : 'TaskFlow'
  }, [title])
}
