export type DueStatus = 'overdue' | 'soon' | 'later'

/** Overdue before today; "soon" today and the next two days. Calendar dates, local time. */
export function dueStatus(isoDate: string, today: Date = new Date()): DueStatus {
  const todayIso = toIsoDate(today)
  if (isoDate < todayIso) return 'overdue'
  const soonLimit = new Date(today.getFullYear(), today.getMonth(), today.getDate() + 2)
  return isoDate <= toIsoDate(soonLimit) ? 'soon' : 'later'
}

function toIsoDate(date: Date) {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}
