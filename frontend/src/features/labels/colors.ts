import type { LabelColor } from '../../types/api'

/**
 * Every palette colour mapped to complete class names. Tailwind only generates classes it can
 * find written out in the source, so they cannot be built at runtime ("bg-" + color).
 */
export const LABEL_COLORS: Record<LabelColor, { name: string; chip: string; swatch: string }> = {
  GRAY: { name: 'Gray', chip: 'bg-slate-200 text-slate-800', swatch: 'bg-slate-400' },
  RED: { name: 'Red', chip: 'bg-red-100 text-red-800', swatch: 'bg-red-500' },
  ORANGE: { name: 'Orange', chip: 'bg-orange-100 text-orange-800', swatch: 'bg-orange-500' },
  YELLOW: { name: 'Yellow', chip: 'bg-yellow-100 text-yellow-900', swatch: 'bg-yellow-400' },
  GREEN: { name: 'Green', chip: 'bg-green-100 text-green-800', swatch: 'bg-green-500' },
  BLUE: { name: 'Blue', chip: 'bg-blue-100 text-blue-800', swatch: 'bg-blue-500' },
  PURPLE: { name: 'Purple', chip: 'bg-purple-100 text-purple-800', swatch: 'bg-purple-500' },
  PINK: { name: 'Pink', chip: 'bg-pink-100 text-pink-800', swatch: 'bg-pink-500' },
}

export const LABEL_COLOR_VALUES = Object.keys(LABEL_COLORS) as LabelColor[]
