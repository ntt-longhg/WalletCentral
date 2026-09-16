import * as React from "react"
import { ChevronLeft, ChevronRight, Calendar } from "lucide-react"
import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover"

const MONTHS = [
  "Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4",
  "Tháng 5", "Tháng 6", "Tháng 7", "Tháng 8",
  "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12",
]

interface MonthPickerProps {
  value?: string
  onChange?: (value: string) => void
  placeholder?: string
  disabled?: boolean
}

function getCurrentYearMonth() {
  const now = new Date()
  return { year: now.getFullYear(), month: now.getMonth() + 1 }
}

function formatValue(year: number, month: number): string {
  return `${year}-${String(month).padStart(2, "0")}`
}

function isFuture(year: number, month: number): boolean {
  const { year: cy, month: cm } = getCurrentYearMonth()
  return year > cy || (year === cy && month > cm)
}

export function MonthPicker({ value, onChange, placeholder = "Chọn tháng", disabled }: MonthPickerProps) {
  const [open, setOpen] = React.useState(false)

  const initial = value ? (() => {
    const [y, m] = value.split("-").map(Number)
    return { year: y, month: m }
  })() : (() => {
    const now = getCurrentYearMonth()
    return { year: now.year, month: now.month }
  })()

  const [viewYear, setViewYear] = React.useState(initial.year)

  const handleSelect = (month: number) => {
    if (isFuture(viewYear, month)) return
    onChange?.(formatValue(viewYear, month))
    setOpen(false)
  }

  const displayText = value
    ? (() => {
        const [y, m] = value.split("-").map(Number)
        return `Tháng ${m}/${y}`
      })()
    : placeholder

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          variant="outline"
          disabled={disabled}
          className={cn(
            "w-full justify-start text-left font-normal",
            !value && "text-slate-400"
          )}
        >
          <Calendar className="mr-2 h-4 w-4 shrink-0" />
          {displayText}
        </Button>
      </PopoverTrigger>
      <PopoverContent className="w-auto p-0" align="start">
        <div className="p-3">
          <div className="flex items-center justify-between mb-3">
            <Button
              variant="ghost"
              size="icon"
              className="h-7 w-7"
              onClick={() => setViewYear((y) => y - 1)}
            >
              <ChevronLeft className="h-4 w-4" />
            </Button>
            <span className="text-sm font-semibold">{viewYear}</span>
            <Button
              variant="ghost"
              size="icon"
              className="h-7 w-7"
              onClick={() => setViewYear((y) => y + 1)}
            >
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
          <div className="grid grid-cols-3 gap-1.5">
            {MONTHS.map((label, i) => {
              const month = i + 1
              const disabled = isFuture(viewYear, month)
              const selected = value === formatValue(viewYear, month)
              return (
                <Button
                  key={month}
                  variant={selected ? "default" : "ghost"}
                  size="sm"
                  disabled={disabled}
                  className={cn(
                    "h-8 text-xs",
                    selected && "bg-blue-600 text-white hover:bg-blue-700",
                    disabled && "text-slate-300 cursor-not-allowed"
                  )}
                  onClick={() => handleSelect(month)}
                >
                  {label}
                </Button>
              )
            })}
          </div>
        </div>
      </PopoverContent>
    </Popover>
  )
}
