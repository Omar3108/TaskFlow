import type { Label } from '../../../types/api'
import { LABEL_COLORS } from '../colors'

export function LabelChip({ label }: { label: Label }) {
  return (
    <span
      className={`inline-flex max-w-full items-center truncate rounded px-1.5 py-0.5 text-xs font-medium ${LABEL_COLORS[label.color].chip}`}
    >
      {label.name}
    </span>
  )
}
